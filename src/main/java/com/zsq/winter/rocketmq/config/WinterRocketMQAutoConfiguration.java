package com.zsq.winter.rocketmq.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zsq.winter.rocketmq.service.WinterRocketMQTemplate;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
@EnableConfigurationProperties(WinterRocketMQProperties.class)
public class WinterRocketMQAutoConfiguration {

    /**
     * 注入增强的RocketMQEnhanceTemplate
     */
    @Bean
    @ConditionalOnMissingBean({WinterRocketMQTemplate.class})
    public WinterRocketMQTemplate rocketMQEnhanceTemplate(WinterRocketMQProperties winterRocketMQProperties, RocketMQTemplate rocketMQTemplate){
        return new WinterRocketMQTemplate(winterRocketMQProperties,rocketMQTemplate);
    }
    /**
     * 现状：
     * Cannot construct instance of java.time.LocalDate
     * 原因：
     * RocketMQMessageConverter 默认情况下不直接支持 Java 8 中的 LocalDate 和 LocalDateTime 类型。根据提供的信息，
     * RocketMQMessageConverter 使用 MappingJackson2MessageConverter 进行 JSON 转换，而这个转换器本身不直接支持 Java 8 的新的时间类型。
     * 解决方案：
     * 自定义消息转换器，将MappingJackson2MessageConverter进行替换，并添加支持时间模块
     * 源码参考：{@link 'org.apache.rocketmq.spring.autoconfigure.MessageConverterConfiguration'}
     * 创建增强的RocketMQ消息转换器，用于支持Java 8时间类型的序列化
     * 
     * @Bean 注解：将此方法返回的对象注册为Spring容器中的Bean
     * @Primary 注解：当存在多个相同类型的Bean时，优先使用此Bean进行注入
     * 
     * @return 返回配置好的RocketMQMessageConverter实例
     */
    @Bean
    @Primary
    public RocketMQMessageConverter enhanceRocketMQMessageConverter() {
        return createEnhancedConverter(new JavaTimeModule());
    }
    
    /**
     * 创建并配置增强的RocketMQ消息转换器
     * 
     * @param timeModule JavaTimeModule实例，用于支持Java 8时间类型的序列化
     * @return 返回配置好的RocketMQMessageConverter实例
     * @throws IllegalStateException 当未找到MappingJackson2MessageConverter时抛出此异常
     */
    private RocketMQMessageConverter createEnhancedConverter(JavaTimeModule timeModule) {
        // 创建RocketMQ默认的消息转换器
        RocketMQMessageConverter converter = new RocketMQMessageConverter();
        // 获取内部的组合消息转换器，它包含了多个具体的消息转换器实现
        CompositeMessageConverter compositeConverter = (CompositeMessageConverter) converter.getMessageConverter();
        
        // 用于标记是否找到Jackson转换器
        boolean found = false;
        // 遍历所有消息转换器
        for (MessageConverter messageConverter : compositeConverter.getConverters()) {
            // 检查是否为Jackson消息转换器
            if (messageConverter instanceof MappingJackson2MessageConverter) {
                found = true;
                // 将消息转换器转换为Jackson类型
                MappingJackson2MessageConverter jacksonConverter = (MappingJackson2MessageConverter) messageConverter;
                // 获取ObjectMapper并注册JavaTimeModule，使其支持Java 8时间类型
                jacksonConverter.getObjectMapper().registerModules(timeModule);
                // 找到并配置后即可退出循环
                break;
            }
        }
        
        // 如果未找到Jackson转换器，抛出异常
        if (!found) {
            throw new IllegalStateException("未找到 MappingJackson2MessageConverter");
        }
        
        // 返回配置好的转换器
        return converter;
    }


    /**
     * 环境隔离配置
     */
    @Bean
    @ConditionalOnProperty(name="rocketmq.enhance.enabledIsolation", havingValue="true",matchIfMissing = true)
    //只有当 rocketmq.enhance.enabledIsolation=true 设置在配置文件中时或者不写该属性，EnvironmentIsolationConfig 才会被 Spring 加载和处理
    public EnvironmentIsolationConfig environmentSetup(WinterRocketMQProperties rocketEnhanceProperties){
        return new EnvironmentIsolationConfig(rocketEnhanceProperties);
    }

}