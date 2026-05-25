package com.njtech.xcloud.consumer;

import com.alibaba.fastjson2.JSON;
import com.njtech.xcloud.dto.AnalysisTaskMsg;
import com.njtech.xcloud.service.AiService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 视频分析消费者
 * 监听 video-analysis-topic 主题，异步处理 AI 分析任务
 * 仅在 rocketmq.enable=true 时启用
 */
@Component
@ConditionalOnProperty(name = "rocketmq.enable", havingValue = "true")
@RocketMQMessageListener(
        topic = "video-analysis-topic",
        consumerGroup = "video-analysis-consumer-group"
)
public class VideoAnalysisConsumer implements RocketMQListener<String> {

    private static final Logger logger = LoggerFactory.getLogger(VideoAnalysisConsumer.class);

    @Autowired
    private AiService aiService;

    @Override
    public void onMessage(String message) {
        logger.info("[RocketMQ] 收到视频分析消息: {}", message);
        try {
            AnalysisTaskMsg taskMsg = JSON.parseObject(message, AnalysisTaskMsg.class);
            if (taskMsg == null || taskMsg.getFileId() == null) {
                logger.warn("[RocketMQ] 消息格式错误，忽略");
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
                logger.warn("[RocketMQ] 未知操作类型: {}", action);
            }

        } catch (Exception e) {
            logger.error("[RocketMQ] 消费失败", e);
        }
    }
}
