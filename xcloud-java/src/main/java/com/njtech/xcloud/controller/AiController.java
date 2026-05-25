package com.njtech.xcloud.controller;

import com.alibaba.fastjson2.JSON;
import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.dto.AnalysisTaskMsg;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.FileCategoryEnum;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.FileInfoVO;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.service.AiService;
import com.njtech.xcloud.service.FileInfoService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 视频分析 Controller
 */
@RestController("aiController")
@RequestMapping("/ai")
public class AiController extends ABaseController {

    @Resource
    private AiService aiService;

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 提交 AI 分析任务（异步，通过 RabbitMQ 解耦）
     * action: analyze=完整分析(转录+总结), transcribe=仅提取文字
     */
    @PostMapping("/submit")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO submitAnalysis(HttpSession session,
                                     @VerifyParam(required = true) String fileId,
                                     @VerifyParam(required = true) String action, Principal principal) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        // Redisson 分布式锁防重复提交（同一用户同一文件）
        String lockKey = "ai:lock:" + userInfo.getUserId() + ":" + fileId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return getServerErrorResponseVO("分析任务正在进行中，请勿重复提交");
            }
        } catch (InterruptedException e) {
            return getServerErrorResponseVO("系统繁忙，请稍后再试");
        }

        try {
            // 校验文件存在且属于当前用户
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
            if (fileInfo == null) {
                return getServerErrorResponseVO("文件不存在");
            }

            // 只支持视频和音频类型
            if (fileInfo.getFileCategory() != null
                    && fileInfo.getFileCategory() != FileCategoryEnum.VIDEO.getCategory()
                    && fileInfo.getFileCategory() != FileCategoryEnum.MUSIC.getCategory()) {
                return getServerErrorResponseVO("仅支持视频和音频文件的 AI 分析");
            }

            // 发送 RabbitMQ 消息
            AnalysisTaskMsg taskMsg = new AnalysisTaskMsg(fileId, action);
            rabbitTemplate.convertAndSend("video.analysis.exchange", "video.analysis.routing", JSON.toJSONString(taskMsg));

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("action", action);
            result.put("status", "processing");
            return getSuccessResponseVO(result);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 提交 AI 分析任务（走 RabbitMQ 异步解耦）
     */
    @PostMapping("/analyze")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO analyze(HttpSession session,
                              @VerifyParam(required = true) String fileId) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        // Redisson 分布式锁防重复提交
        String lockKey = "ai:lock:" + userInfo.getUserId() + ":" + fileId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return getServerErrorResponseVO("分析任务正在进行中，请勿重复提交");
            }
        } catch (InterruptedException e) {
            return getServerErrorResponseVO("系统繁忙，请稍后再试");
        }

        try {
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
            if (fileInfo == null) {
                return getServerErrorResponseVO("文件不存在");
            }

            // RabbitMQ 异步解耦，不再直接调用 aiService.asyncAnalyze
            AnalysisTaskMsg taskMsg = new AnalysisTaskMsg(fileId, "analyze");
            rabbitTemplate.convertAndSend("video.analysis.exchange", "video.analysis.routing", JSON.toJSONString(taskMsg));

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("status", "processing");
            return getSuccessResponseVO(result);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 提交文字提取任务（走 RabbitMQ 异步解耦）
     */
    @PostMapping("/transcribe")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO transcribe(HttpSession session,
                                 @VerifyParam(required = true) String fileId) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        // Redisson 分布式锁防重复提交
        String lockKey = "ai:lock:" + userInfo.getUserId() + ":" + fileId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return getServerErrorResponseVO("提取任务正在进行中，请勿重复提交");
            }
        } catch (InterruptedException e) {
            return getServerErrorResponseVO("系统繁忙，请稍后再试");
        }

        try {
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
            if (fileInfo == null) {
                return getServerErrorResponseVO("文件不存在");
            }

            // RabbitMQ 异步解耦，不再直接调用 aiService.asyncTranscribe
            AnalysisTaskMsg taskMsg = new AnalysisTaskMsg(fileId, "transcribe");
            rabbitTemplate.convertAndSend("video.analysis.exchange", "video.analysis.routing", JSON.toJSONString(taskMsg));

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("status", "processing");
            return getSuccessResponseVO(result);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取 AI 分析结果
     */
    @GetMapping("/result/{fileId}")
    @GlobalInterceptor
    public ResponseVO getAnalysisResult(HttpSession session,
                                        @PathVariable("fileId") String fileId) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
        if (fileInfo == null) {
            return getServerErrorResponseVO("文件不存在");
        }

        FileInfoVO vo = new FileInfoVO();
        BeanUtils.copyProperties(fileInfo, vo);
        return getSuccessResponseVO(vo);
    }

    /**
     * 获取已分析的文件列表（带 AI 总结的文件）
     */
    @RequestMapping("/list")
    @GlobalInterceptor
    public ResponseVO listAnalyzedFiles(HttpSession session, FileInfoQuery query) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        query.setUserId(userInfo.getUserId());
        query.setDelFlag(Constants.USING);

        // 仅查询视频和音频
        if (query.getFileCategory() == null) {
            // 默认查询视频分类
            query.setFileCategory(FileCategoryEnum.VIDEO.getCategory());
        }

        query.setOrderBy("last_update_time desc");

        PaginationResultVO<FileInfo> result = fileInfoMapper.selectList(query) != null
                ? new PaginationResultVO<>(
                fileInfoMapper.selectCount(query),
                query.getPageSize() != null ? query.getPageSize() : 15,
                query.getPageNo() != null ? query.getPageNo() : 1,
                0,
                fileInfoMapper.selectList(query))
                : new PaginationResultVO<>();

        PaginationResultVO<FileInfoVO> voResult = convertPaginationResult(result, FileInfoVO.class);
        return getSuccessResponseVO(voResult);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session,
                              @VerifyParam(required = true) String fileIds) {
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        fileInfoService.delFile(webUserVO.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }
}
