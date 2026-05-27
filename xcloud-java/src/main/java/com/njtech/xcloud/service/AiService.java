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

    // Redis 缓存 key 前缀
    private static final String REDIS_KEY_TRANSCRIPT = "ai:transcript:";
    private static final String REDIS_KEY_SUMMARY    = "ai:summary:";
    // 缓存有效期（7天）
    private static final long CACHE_TTL_DAYS = 7;

    /**
     * 判断文本是否为有效结果（非空且不含错误标记）
     */
    private boolean isValidResult(String text) {
        return StringUtils.hasText(text) && !text.contains("❌");
    }

    /**
     * 根据文件 MD5 查找已有分析结果（Redis 优先，降级查库）
     * 只复用有效结果，错误结果不复用
     *
     * @param fileMd5 文件 MD5
     * @param field   复用的字段："transcriptText" 或 "aiSummary"
     * @return 有效的已有结果，未找到返回 null
     */
    private String findExistingResultByMd5(String fileMd5, String field) {
        if (!StringUtils.hasText(fileMd5)) return null;

        // 1. 先查 Redis 缓存
        String redisKey = ("transcriptText".equals(field) ? REDIS_KEY_TRANSCRIPT : REDIS_KEY_SUMMARY) + fileMd5;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (isValidResult(cached)) {
            logger.info("[AI] Redis 缓存命中，field={}, fileMd5={}", field, fileMd5);
            return cached;
        }

        // 2. 缓存未命中，查数据库
        FileInfoQuery query = new FileInfoQuery();
        query.setFileMd5(fileMd5);
        query.setDelFlag(Constants.USING);
        query.setPageSize(10);
        List<FileInfo> list = fileInfoMapper.selectList(query);
        if (CollectionUtils.isEmpty(list)) return null;

        for (FileInfo item : list) {
            if ("transcriptText".equals(field) && isValidResult(item.getTranscriptText())) {
                // 回写 Redis
                stringRedisTemplate.opsForValue().set(redisKey, item.getTranscriptText(),
                        CACHE_TTL_DAYS, java.util.concurrent.TimeUnit.DAYS);
                logger.info("[AI] DB 命中去重结果，已回写 Redis，field={}, fileMd5={}", field, fileMd5);
                return item.getTranscriptText();
            }
            if ("aiSummary".equals(field) && isValidResult(item.getAiSummary())) {
                stringRedisTemplate.opsForValue().set(redisKey, item.getAiSummary(),
                        CACHE_TTL_DAYS, java.util.concurrent.TimeUnit.DAYS);
                logger.info("[AI] DB 命中去重结果，已回写 Redis，field={}, fileMd5={}", field, fileMd5);
                return item.getAiSummary();
            }
        }
        return null;
    }

    /**
     * 将成功结果写入 Redis 缓存
     */
    private void cacheResult(String fileMd5, String field, String result) {
        if (!StringUtils.hasText(fileMd5) || !isValidResult(result)) return;
        String redisKey = ("transcriptText".equals(field) ? REDIS_KEY_TRANSCRIPT : REDIS_KEY_SUMMARY) + fileMd5;
        stringRedisTemplate.opsForValue().set(redisKey, result, CACHE_TTL_DAYS, java.util.concurrent.TimeUnit.DAYS);
        logger.info("[AI] 结果已写入 Redis 缓存，field={}, fileMd5={}", field, fileMd5);
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

        // 清空旧错误结果，避免前端轮询时立刻展示旧错误
        clearErrorState(fileId);

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
            cacheResult(fileMd5, "transcriptText", text);

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
            cacheResult(fileMd5, "aiSummary", aiSummary);

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

        // 清空旧错误结果
        clearErrorState(fileId);

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
            cacheResult(fileMd5, "transcriptText", text);

            logger.info("[AI] 全文提取完成，fileId: {}", fileId);

        } catch (Exception e) {
            logger.error("[AI] 提取失败，fileId: {}", fileId, e);
            FileInfo updateFail = new FileInfo();
            updateFail.setFileId(fileId);
            updateFail.setTranscriptText("❌ 提取失败: " + e.getMessage());
            fileInfoMapper.updateAiFields(updateFail);
        }
    }

    /**
     * 判断文本是否为错误结果（含 ❌ 或已知错误关键字）
     * 兼容历史上未加 ❌ 前缀的旧错误记录
     */
    private boolean isErrorText(String text) {
        if (!StringUtils.hasText(text)) return false;
        return text.contains("❌")
                || text.startsWith("FFmpeg 转换失败")
                || text.startsWith("处理异常:")
                || text.startsWith("识别失败")
                || text.startsWith("最终失败");
    }

    /**
     * 清空指定 fileId 的旧错误结果（DB + Redis 同步清除）
     * 兼容历史上未加 ❌ 前缀的错误文本
     */
    private void clearErrorState(String fileId) {
        FileInfo current = fileInfoMapper.selectByFileId(fileId);
        if (current == null) return;
        boolean needClear = false;
        FileInfo clearInfo = new FileInfo();
        clearInfo.setFileId(fileId);

        if (isErrorText(current.getTranscriptText())) {
            clearInfo.setTranscriptText("");
            needClear = true;
            // 同步清除 Redis 中的脏缓存
            if (StringUtils.hasText(current.getFileMd5())) {
                stringRedisTemplate.delete(REDIS_KEY_TRANSCRIPT + current.getFileMd5());
                logger.info("[AI] 清除 Redis 脏缓存，key: {}", REDIS_KEY_TRANSCRIPT + current.getFileMd5());
            }
        }
        if (isErrorText(current.getAiSummary())) {
            clearInfo.setAiSummary("");
            needClear = true;
            if (StringUtils.hasText(current.getFileMd5())) {
                stringRedisTemplate.delete(REDIS_KEY_SUMMARY + current.getFileMd5());
                logger.info("[AI] 清除 Redis 脏缓存，key: {}", REDIS_KEY_SUMMARY + current.getFileMd5());
            }
        }
        if (needClear) {
            fileInfoMapper.updateAiFields(clearInfo);
            logger.info("[AI] 清空旧错误状态，fileId: {}", fileId);
        }
    }
}
