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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

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

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 提交 AI 分析任务（异步，通过 RocketMQ 解耦）
     * action: analyze=完整分析(转录+总结), transcribe=仅提取文字
     */
    @PostMapping("/submit")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO submitAnalysis(HttpSession session,
                                     @VerifyParam(required = true) String fileId,
                                     @VerifyParam(required = true) String action) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

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

        // 发送 RocketMQ 消息
        if (rocketMQTemplate == null) {
            return getServerErrorResponseVO("RocketMQ 未启用，请使用 /ai/analyze 或 /ai/transcribe 接口");
        }
        AnalysisTaskMsg taskMsg = new AnalysisTaskMsg(fileId, action);
        rocketMQTemplate.convertAndSend("video-analysis-topic", JSON.toJSONString(taskMsg));

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("action", action);
        result.put("status", "processing");
        return getSuccessResponseVO(result);
    }

    /**
     * 直接提交 AI 分析（不走 RocketMQ，线程池异步执行）
     * 适用于未部署 RocketMQ 的场景
     */
    @PostMapping("/analyze")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO analyze(HttpSession session,
                              @VerifyParam(required = true) String fileId) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
        if (fileInfo == null) {
            return getServerErrorResponseVO("文件不存在");
        }

        // 线程池异步执行
        aiService.asyncAnalyze(fileId);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("status", "processing");
        return getSuccessResponseVO(result);
    }

    /**
     * 直接提交文字提取任务（不走 RocketMQ）
     */
    @PostMapping("/transcribe")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO transcribe(HttpSession session,
                                 @VerifyParam(required = true) String fileId) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
        if (fileInfo == null) {
            return getServerErrorResponseVO("文件不存在");
        }

        aiService.asyncTranscribe(fileId);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileId);
        result.put("status", "processing");
        return getSuccessResponseVO(result);
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
    @PostMapping("/list")
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
}
