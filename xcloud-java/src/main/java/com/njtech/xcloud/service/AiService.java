package com.njtech.xcloud.service;

import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.strategy.AiAnalysisStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Autowired
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Autowired
    @Qualifier("defaultAiStrategy")
    private AiAnalysisStrategy aiAnalysisStrategy;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${project.folder:d:/easypan/}")
    private String projectFolder;

    /**
     * 根据文件 MD5 查找已有分析结果（内容级去重）
     * @param fileMd5 文件 MD5
     * @param field 复用的字段："transcriptText" 或 "aiSummary"
     * @return 已有结果，未找到返回 null
     */
    private String findExistingResultByMd5(String fileMd5, String field) {
        if (!StringUtils.hasText(fileMd5)) {
            return null;
        }
        FileInfoQuery query = new FileInfoQuery();
        query.setFileMd5(fileMd5);
        query.setDelFlag(Constants.USING);
        query.setPageSize(10);
        List<FileInfo> list = fileInfoMapper.selectList(query);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        for (FileInfo item : list) {
            if ("transcriptText".equals(field) && StringUtils.hasText(item.getTranscriptText())) {
                return item.getTranscriptText();
            }
            if ("aiSummary".equals(field) && StringUtils.hasText(item.getAiSummary())) {
                return item.getAiSummary();
            }
        }
        return null;
    }

    /**
     * 异步执行完整 AI 分析（转录 + 总结）
     * 支持 MD5 内容级去重：相同内容的文件直接复用已有结果
     */
    @Async("aiTaskExecutor")
    public void asyncAnalyze(String fileId) {
        logger.info("[AI] 开始分析任务，fileId: {}", fileId);

        FileInfo fileInfo = fileInfoMapper.selectByFileId(fileId);
        if (fileInfo == null) return;

        String fileMd5 = fileInfo.getFileMd5();

        try {
            // ========== 1. 语音转文字（先去重）==========
            String text = findExistingResultByMd5(fileMd5, "transcriptText");
            if (StringUtils.hasText(text)) {
                logger.info("[AI] MD5 命中内容去重，直接复用转录文本，fileMd5: {}", fileMd5);
            } else {
                String localFilePath = projectFolder + fileInfo.getFilePath();
                text = aiAnalysisStrategy.transcribe(localFilePath);
            }
            FileInfo updateText = new FileInfo();
            updateText.setFileId(fileId);
            updateText.setTranscriptText(text);
            fileInfoMapper.updateAiFields(updateText);

            // ========== 2. 智能总结（先去重）==========
            String aiSummary = findExistingResultByMd5(fileMd5, "aiSummary");
            if (StringUtils.hasText(aiSummary)) {
                logger.info("[AI] MD5 命中内容去重，直接复用 AI 总结，fileMd5: {}", fileMd5);
            } else {
                String localFilePath = projectFolder + fileInfo.getFilePath();
                aiSummary = aiAnalysisStrategy.generateSummary(localFilePath);
            }
            FileInfo updateSummary = new FileInfo();
            updateSummary.setFileId(fileId);
            updateSummary.setAiSummary(aiSummary);
            fileInfoMapper.updateAiFields(updateSummary);

            logger.info("[AI] 任务完成，fileId: {}", fileId);

        } catch (Exception e) {
            logger.error("[AI] 任务失败，fileId: {}", fileId, e);
            // 记录失败状态
            FileInfo updateFail = new FileInfo();
            updateFail.setFileId(fileId);
            updateFail.setAiSummary("❌ 分析失败: " + e.getMessage());
            fileInfoMapper.updateAiFields(updateFail);
        }
    }

    /**
     * 异步提取全文文字
     * 支持 MD5 内容级去重
     */
    @Async("aiTaskExecutor")
    public void asyncTranscribe(String fileId) {
        logger.info("[AI] 开始全文提取任务，fileId: {}", fileId);

        FileInfo fileInfo = fileInfoMapper.selectByFileId(fileId);
        if (fileInfo == null) return;

        String fileMd5 = fileInfo.getFileMd5();

        try {
            // 先去重：查找相同 MD5 且已有转录结果的记录
            String text = findExistingResultByMd5(fileMd5, "transcriptText");
            if (StringUtils.hasText(text)) {
                logger.info("[AI] MD5 命中内容去重，直接复用转录文本，fileMd5: {}", fileMd5);
            } else {
                String localFilePath = projectFolder + fileInfo.getFilePath();
                text = aiAnalysisStrategy.transcribe(localFilePath);
            }

            FileInfo update = new FileInfo();
            update.setFileId(fileId);
            update.setTranscriptText(text);
            fileInfoMapper.updateAiFields(update);

            logger.info("[AI] 全文提取完成，fileId: {}", fileId);

        } catch (Exception e) {
            logger.error("[AI] 提取失败，fileId: {}", fileId, e);
        }
    }
}
