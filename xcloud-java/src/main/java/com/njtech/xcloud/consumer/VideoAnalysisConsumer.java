package com.njtech.xcloud.consumer;

import com.alibaba.fastjson2.JSON;
import com.njtech.xcloud.dto.AnalysisTaskMsg;
import com.njtech.xcloud.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 视频分析消费者
 * 监听 video.analysis.queue 队列，异步处理 AI 分析任务
 */
@Component
public class VideoAnalysisConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoAnalysisConsumer.class);

    @Autowired
    private AiService aiService;

    @RabbitListener(queues = "video.analysis.queue")
    public void onMessage(String message) {
        logger.info("[RabbitMQ] 收到视频分析消息: {}", message);
        try {
            AnalysisTaskMsg taskMsg = JSON.parseObject(message, AnalysisTaskMsg.class);
            if (taskMsg == null || taskMsg.getFileId() == null) {
                logger.warn("[RabbitMQ] 消息格式错误，忽略");
                return;
            }

            String action = taskMsg.getAction();
            String fileId = taskMsg.getFileId();

            if ("analyze".equals(action)) {
                // 完整分析：转录 + 总结
                aiService.asyncAnalyze(fileId);
            } else if ("transcribe".equals(action)) {
                // 仅提取文字
                aiService.asyncTranscribe(fileId);
            } else {
                logger.warn("[RabbitMQ] 未知操作类型: {}", action);
            }

        } catch (Exception e) {
            logger.error("[RabbitMQ] 消费失败", e);
        }
    }
}
