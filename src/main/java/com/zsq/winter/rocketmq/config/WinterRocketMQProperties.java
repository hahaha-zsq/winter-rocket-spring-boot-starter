package com.zsq.winter.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "winter.rocketmq")
@Data
public class WinterRocketMQProperties {

    /**
     * 启动后会自动在topic上拼接激活的配置文件，达到自动隔离的效果(默认为true)
     */
    private boolean enabledIsolation=true;

    /**
     * 隔离环境名称，拼接到topic后，如：topic_dev，默认空字符串
     */
    private String environment;
}