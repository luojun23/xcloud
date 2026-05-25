package com.njtech.xcloud.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 声明视频分析队列、交换机和绑定关系
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 视频分析队列
     */
    @Bean
    public Queue videoAnalysisQueue() {
        return new Queue("video.analysis.queue", true);
    }

    /**
     * 视频分析交换机（Direct 类型）
     */
    @Bean
    public DirectExchange videoAnalysisExchange() {
        return new DirectExchange("video.analysis.exchange");
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding videoAnalysisBinding() {
        return BindingBuilder
                .bind(videoAnalysisQueue())
                .to(videoAnalysisExchange())
                .with("video.analysis.routing");
    }
}
