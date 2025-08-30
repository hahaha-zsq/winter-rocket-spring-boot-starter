package com.zsq.winter.rocketmq.service;

import com.alibaba.fastjson.JSONObject;
import com.zsq.winter.rocketmq.config.WinterRocketMQProperties;
import com.zsq.winter.rocketmq.entity.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RocketMQ增强模板类
 * 对RocketMQTemplate进行增强，提供更便捷的消息发送方法
 * 支持多环境topic隔离
 */
@Slf4j
public class WinterRocketMQTemplate {

    /**
     * RocketMQ增强配置属性
     * 用于配置环境隔离等功能
     */
    private final WinterRocketMQProperties winterRocketMQProperties;

    /**
     * RocketMQ模板类
     * Spring Boot提供的RocketMQ操作模板
     */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构造函数
     * @param winterRocketMQProperties RocketMQ增强配置属性
     * @param rocketMQTemplate RocketMQ模板类
     */
    public WinterRocketMQTemplate(final WinterRocketMQProperties winterRocketMQProperties, final RocketMQTemplate rocketMQTemplate) {
        this.winterRocketMQProperties = winterRocketMQProperties;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 构建消息目的地
     * 根据系统上下文自动构建隔离后的topic
     * @param topic 主题
     * @param tag 标签
     * @return 完整的消息目的地(topic:tag)
     */
    public String buildDestination(String topic, String tag) {
        topic = reBuildTopic(topic);
        return topic + ":" + tag;
    }

    /**
     * 重建主题名称
     * 根据环境配置对topic进行隔离处理
     * 如果启用了环境隔离且环境名不为空，则在topic后添加环境后缀
     * @param topic 原始topic名称
     * @return 重建后的topic名称
     */
    private String reBuildTopic(String topic) {
        if (winterRocketMQProperties.isEnabledIsolation() && StringUtils.hasText(winterRocketMQProperties.getEnvironment())) {
            return topic + "_" + winterRocketMQProperties.getEnvironment();
        }
        return topic;
    }

    /**
     * 发送同步消息
     * @param topic 主题
     * @param tag 标签
     * @param message 消息体
     * @return 发送结果
     */
    public <T extends BaseMessage> SendResult send(String topic, String tag, T message) {
        return send(buildDestination(topic, tag), message);
    }

    /**
     * 发送延迟消息到指定目的地
     * @param destination 目的地
     * @param message 消息体
     * @param delayLevel 延迟等级
     * @return 发送结果
     */
    public <T extends BaseMessage> SendResult send(String destination, T message, int delayLevel) {
        Message<T> sendMessage = MessageBuilder.withPayload(message)
                .setHeader(RocketMQHeaders.KEYS, message.getKey())
                .build();
        // 设置3秒超时时间发送延迟消息
        SendResult sendResult = rocketMQTemplate.syncSend(destination, sendMessage, 3000, delayLevel);
        log.info("[{}]延迟等级[{}]消息[{}]发送结果[{}]", destination, delayLevel, JSONObject.toJSON(message), JSONObject.toJSON(sendResult));
        return sendResult;
    }

    /**
     * 发送同步消息到指定目的地
     * @param destination 目的地
     * @param message 消息体
     * @return 发送结果
     */
    public <T extends BaseMessage> SendResult send(String destination, T message) {
        // 构建消息对象，设置消息业务键
        Message<T> sendMessage = MessageBuilder.withPayload(message)
                .setHeader(RocketMQHeaders.KEYS, message.getKey())
                .build();
        // 同步发送消息并获取结果
        SendResult sendResult = rocketMQTemplate.syncSend(destination, sendMessage);
        log.info("[{}]同步消息[{}]发送结果[{}]", destination, JSONObject.toJSON(message), JSONObject.toJSON(sendResult));
        return sendResult;
    }

    /**
     * 发送延迟消息
     * RocketMQ的延迟等级说明：
     * 1级=1s, 2级=5s, 3级=10s, 4级=30s, 5级=1m, 6级=2m, 7级=3m, 8级=4m, 9级=5m
     * 10级=6m, 11级=7m, 12级=8m, 13级=9m, 14级=10m, 15级=20m, 16级=30m, 17级=1h, 18级=2h
     * @param topic 主题
     * @param tag 标签
     * @param message 消息体
     * @param delayLevel 延迟等级
     * @return 发送结果
     */
    public <T extends BaseMessage> SendResult send(String topic, String tag, T message, int delayLevel) {
        return send(buildDestination(topic, tag), message, delayLevel);
    }

    /**
     * 异步发送消息
     * @param topic 主题
     * @param tag 标签
     * @param message 消息体
     */
    public <T extends BaseMessage> void asyncSend(String topic, String tag, T message) {
        asyncSend(buildDestination(topic, tag), message);
    }

    /**
     * 异步发送消息到指定目的地
     * 通过回调接口处理发送结果
     * @param destination 目的地
     * @param message 消息体
     */
    public <T extends BaseMessage> void asyncSend(String destination, T message) {
        Message<T> sendMessage = MessageBuilder.withPayload(message)
                .setHeader(RocketMQHeaders.KEYS, message.getKey())
                .build();
        rocketMQTemplate.asyncSend(destination, sendMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("[{}]异步消息[{}]发送成功，结果[{}]", destination, JSONObject.toJSON(message), JSONObject.toJSON(sendResult));
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("[{}]异步消息[{}]发送异常", destination, JSONObject.toJSON(message), throwable);
            }
        });
    }

    /**
     * 发送单向消息
     * 不关心发送结果，性能最高
     * @param topic 主题
     * @param tag 标签
     * @param message 消息体
     */
    public <T extends BaseMessage> void sendOneWay(String topic, String tag, T message) {
        sendOneWay(buildDestination(topic, tag), message);
    }

    /**
     * 发送单向消息到指定目的地
     * @param destination 目的地
     * @param message 消息体
     */
    public <T extends BaseMessage> void sendOneWay(String destination, T message) {
        Message<T> sendMessage = MessageBuilder.withPayload(message)
                .setHeader(RocketMQHeaders.KEYS, message.getKey())
                .build();
        rocketMQTemplate.sendOneWay(destination, sendMessage);
        log.info("[{}]单向消息[{}]已发送", destination, JSONObject.toJSON(message));
    }

    /**
     * 批量发送消息
     * @param topic 主题
     * @param tag 标签
     * @param messages 消息列表
     * @return 发送结果
     */
    public <T extends BaseMessage> SendResult sendBatch(String topic, String tag, Collection<T> messages) {
        return sendBatch(buildDestination(topic, tag), messages);
    }

    /**
     * 批量发送消息到指定目的地
     * @param destination 目的地
     * @param messages 消息列表
     * @return 发送结果
     */
    public <T extends BaseMessage> SendResult sendBatch(String destination, Collection<T> messages) {
        // 将消息列表转换为RocketMQ消息对象列表
        List<Message<T>> messageList = messages.stream()
                .map(message -> MessageBuilder.withPayload(message)
                        .setHeader(RocketMQHeaders.KEYS, message.getKey())
                        .build())
                .collect(Collectors.toList());
        SendResult sendResult = rocketMQTemplate.syncSend(destination, messageList);
        log.info("[{}]批量消息[{}]发送结果[{}]", destination, JSONObject.toJSON(messages), JSONObject.toJSON(sendResult));
        return sendResult;
    }

    /**
     * 发送事务消息
     * @param topic 主题
     * @param tag 标签
     * @param message 消息体
     * @return 事务发送结果
     */
    public <T extends BaseMessage> TransactionSendResult sendMessageInTransaction(String topic, String tag, T message) {
        return sendMessageInTransaction(buildDestination(topic, tag), message, null);
    }

    /**
     * 发送事务消息到指定目的地
     * @param destination 目的地
     * @param message 消息体
     * @param arg 事务参数，传递给本地事务执行器
     * @return 事务发送结果
     */
    public <T extends BaseMessage> TransactionSendResult sendMessageInTransaction(String destination, T message, Object arg) {
        Message<T> sendMessage = MessageBuilder.withPayload(message)
                .setHeader(RocketMQHeaders.KEYS, message.getKey())
                .build();
        TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(destination, sendMessage, arg);
        log.info("[{}]事务消息[{}]发送结果[{}]", destination, JSONObject.toJSON(message), JSONObject.toJSON(sendResult));
        return sendResult;
    }


}