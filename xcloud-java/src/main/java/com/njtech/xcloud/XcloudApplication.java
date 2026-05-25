package com.njtech.xcloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(
        scanBasePackages = {"com.njtech.xcloud"},
        exclude = {
                org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class
        }
)
@EnableAsync
@MapperScan("com.njtech.xcloud.mappers")
public class XcloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(XcloudApplication.class, args);
    }

}
