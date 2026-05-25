package com.njtech.xcloud.service;

import com.njtech.xcloud.entity.po.FileInfo;
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

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Autowired
    private FileInfoMapper<FileInfo, ?> fileInfoMapper;

    @Autowired
    @Qualifier("defaultAiStrategy")
    private AiAnalysisStrategy aiAnalysisStrategy;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${project.folder:d:/easypan/}")
    private String projectFolder;

    /**
     * 异步执行完整 AI 分析（转录 + 总结）
     */
    @Async("aiTaskExecutor")
    public void asyncAnalyze(String fileId) {
        logger.info("[AI] 开始分析任务，fileId: {}", fileId);

        FileInfo fileInfo = fileInfoMapper.selectByFileId(fileId);
        if (fileInfo == null) return;

        try {
            // 获取本地文件完整路径
            String localFilePath = projectFolder + fileInfo.getFilePath();

            // 1. 语音转文字
            String text = aiAnalysisStrategy.transcribe(localFilePath);
            FileInfo updateText = new FileInfo();
            updateText.setFileId(fileId);
            updateText.setTranscriptText(text);
            fileInfoMapper.updateAiFields(updateText);

            // 2. 智能总结（基于已提取的文字）
            String aiSummary = aiAnalysisStrategy.generateSummary(localFilePath);
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
     */
    @Async("aiTaskExecutor")
    public void asyncTranscribe(String fileId) {
        logger.info("[AI] 开始全文提取任务，fileId: {}", fileId);

        FileInfo fileInfo = fileInfoMapper.selectByFileId(fileId);
        if (fileInfo == null) return;

        try {
            String localFilePath = projectFolder + fileInfo.getFilePath();
            String text = aiAnalysisStrategy.transcribe(localFilePath);

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
