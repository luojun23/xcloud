package com.njtech.xcloud.controller;

import com.alibaba.fastjson2.JSON;
import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.config.RedisUtils;
import com.njtech.xcloud.dto.AnalysisTaskMsg;
import com.njtech.xcloud.dto.DownloadFileDto;
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
import com.njtech.xcloud.strategy.impl.SiliconFlowStrategy;
import com.njtech.xcloud.utils.StringTools;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AI 视频分析 Controller
 */
@RestController("aiController")
@RequestMapping("/ai")
public class AiController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

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

    @Resource
    private SiliconFlowStrategy siliconFlowStrategy;

    @Resource
    private RedisUtils redisUtils;

    @Value("${project.folder:d:/easypan/}")
    private String projectFolder;

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

            // RabbitMQ 异步解耦
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

    /**
     * 提取音频并返回下载码
     * 使用 FFmpeg 从视频中提取 mp3，存入临时目录，生成下载码放入 Redis
     */
    @PostMapping("/extractAudio")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO extractAudio(HttpSession session,
                                   @VerifyParam(required = true) String fileId) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userInfo.getUserId());
        if (fileInfo == null) {
            return getServerErrorResponseVO("文件不存在");
        }
        String videoPath = projectFolder + fileInfo.getFilePath();
        String audioDir = projectFolder + Constants.FILE_FOLDE_TRANSLATE_AUDIO;
        new File(audioDir).mkdirs();
        String mp3Name = fileInfo.getFileName().replaceAll("\\.[^.]+$", "") + "_" + UUID.randomUUID().toString().substring(0, 8) + ".mp3";
        String mp3Path = audioDir + mp3Name;

        try {
            // 调用 FFmpeg 提取音频
            boolean ok = siliconFlowStrategy.extractAudioPublic(videoPath, mp3Path);
            if (!ok || !new File(mp3Path).exists()) {
                return getServerErrorResponseVO("音频提取失败，请检查视频文件是否完整");
            }
            // 生成下载码，存入 Redis（5分钟有效）
            String downloadCode = StringTools.getRandomString(30);
            DownloadFileDto dto = new DownloadFileDto();
            dto.setDownloadCode(downloadCode);
            dto.setFileName(mp3Name);
            dto.setFilePath(Constants.FILE_FOLDE_TRANSLATE_AUDIO + mp3Name);
            redisUtils.set(Constants.REDIS_KEY_DOWNLOAD + downloadCode, dto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
            return getSuccessResponseVO(downloadCode);
        } catch (Exception e) {
            logger.error("[音频提取] 失败，fileId={}", fileId, e);
            return getServerErrorResponseVO("音频提取异常: " + e.getMessage());
        }
    }

    /**
     * 链接下载视频（支持普通 http/https 直链）
     * 下载到本地 file 目录，注册为用户文件
     */
    @PostMapping("/urlDownload")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO urlDownload(HttpSession session,
                                  @VerifyParam(required = true) String url) {
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        try {
            // 解析文件名
            String rawName = url.substring(url.lastIndexOf('/') + 1);
            if (rawName.contains("?")) rawName = rawName.substring(0, rawName.indexOf('?'));
            if (rawName.isEmpty()) rawName = "video_" + System.currentTimeMillis() + ".mp4";
            if (!rawName.contains(".")) rawName = rawName + ".mp4";

            String fileDir = projectFolder + Constants.FILE_FOLDER_FILE;
            new File(fileDir).mkdirs();
            String localPath = fileDir + UUID.randomUUID().toString().replace("-", "") + "_" + rawName;

            logger.info("[链接下载] 开始下载，url={}", url);

            // 通过 HttpURLConnection 下载文件
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();
            if (conn.getResponseCode() != 200) {
                return getServerErrorResponseVO("链接无法访问，HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(localPath)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) fos.write(buf, 0, len);
            }
            conn.disconnect();

            File localFile = new File(localPath);
            if (!localFile.exists() || localFile.length() == 0) {
                return getServerErrorResponseVO("视频下载失败，文件为空");
            }
            logger.info("[链接下载] 下载完成，大小={}bytes，路径={}", localFile.length(), localPath);

            // 注册为 xcloud 文件记录
            String relPath = localPath.replace(projectFolder, "");
            FileInfo newFile = new FileInfo();
            newFile.setFileId(StringTools.getRandomString(Constants.TEN));
            newFile.setUserId(userInfo.getUserId());
            newFile.setFileName(rawName);
            newFile.setFilePath(relPath);
            newFile.setFileSize(localFile.length());
            newFile.setFileCategory(1); // 视频
            newFile.setFileType(3);     // 视频
            newFile.setFolderType(0);
            newFile.setFilePid("0");
            newFile.setDelFlag(0);
            newFile.setStatus(2);
            newFile.setCreateTime(new java.util.Date());
            newFile.setLastUpdateTime(new java.util.Date());
            fileInfoMapper.insert(newFile);

            return getSuccessResponseVO(null);
        } catch (Exception e) {
            logger.error("[链接下载] 失败，url={}", url, e);
            return getServerErrorResponseVO("链接下载失败: " + e.getMessage());
        }
    }
}
