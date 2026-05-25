package com.njtech.xcloud.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 手动配置
 * 仅在 rocketmq.enable=true 时才创建 RocketMQTemplate
 * 默认关闭，需要部署 RocketMQ 时手动开启
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.enable", havingValue = "true")
public class RocketMQConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
}
