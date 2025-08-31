package com.zsq.winter.rocketmq.service;

import com.alibaba.fastjson.JSONObject;
import com.zsq.winter.rocketmq.entity.BaseMessage;
import com.zsq.winter.rocketmq.entity.DelayConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;

/**
 * RocketMQ消息处理增强模板类
 * 提供了消息处理的标准流程和常用功能：
 * 1. 消息过滤
 * 2. 重试机制
 * 3. 异常处理
 * 4. 消息追踪
 * 5. 延迟消息处理
 * 
 * @param <T> 消息类型，必须继承自BaseMessage
 */
@Slf4j
public abstract class EnhanceMessageHandler<T extends BaseMessage> {
    /**
     * 默认最大重试次数
     * 当消息消费失败时，最多重试3次
     */
    private static final int MAX_RETRY_TIMES = 3;

    /**
     * 消息重试的延时等级
     * 使用DelayConstant.FIVE_SECOND，表示重试时延迟5秒
     */
    private static final int DELAY_LEVEL = DelayConstant.FIVE_SECOND;

    /**
     * RocketMQ增强模板
     * 用于消息的发送操作
     */
    private final WinterRocketMQTemplate winterRocketMQTemplate;

    /**
     * 构造函数
     * @param winterRocketMQTemplate RocketMQ增强模板，用于消息发送
     */
    protected EnhanceMessageHandler(WinterRocketMQTemplate winterRocketMQTemplate) {
        this.winterRocketMQTemplate = winterRocketMQTemplate;
    }

    /**
     * 消息处理的具体业务逻辑
     * 由子类实现具体的消息处理逻辑
     *
     * @param message 待处理的消息对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    protected abstract void handleMessage(T message) throws Exception;

    /**
     * 处理超过最大重试次数的消息
     * 当消息重试次数超过限制时的处理逻辑
     * 子类必须实现此方法来处理这种情况
     *
     * @param message 超过重试次数的消息对象
     */
    protected abstract void handleMaxRetriesExceeded(T message);

    /**
     * 是否启用消息重试机制
     * 子类通过实现此方法来决定是否对失败的消息进行重试
     *
     * @return true表示启用重试，false表示不重试
     */
    protected abstract boolean isRetry();

    /**
     * 获取重试消息的前缀
     * 用于标识重试的消息，方便追踪和处理
     *
     * @return 重试消息的前缀字符串
     */
    protected abstract String retryPrefix();

    /**
     * 消费异常时的处理策略
     * 决定是否将异常抛给RocketMQ框架处理
     *
     * @return true表示抛出异常给RocketMQ框架，将使用RocketMQ的重试机制
     *         false表示内部处理异常，如果未开启重试，消息会被确认消费
     */
    protected abstract boolean throwException();

    /**
     * 消息过滤器
     * 可以通过重写此方法实现消息过滤逻辑
     * 例如：消息去重、消息有效性验证等
     *
     * @param message 待过滤的消息
     * @return true表示消息需要被过滤掉（不处理），false表示不需要处理该消息
     */
    protected boolean filter(T message) {
        return false;
    }

    /**
     * 获取最大重试次数
     * 可以被子类重写以自定义重试次数
     *
     * @return 最大重试次数，默认为3次
     */
    protected int getMaxRetryTimes() {
        return MAX_RETRY_TIMES;
    }

    /**
     * 获取重试消息的延迟等级
     * 可以被子类重写以自定义延迟时间
     *
     * @return 延迟等级，-1表示立即重试，其他值参考RocketMQ的延迟等级定义
     */
    protected int getDelayLevel() {
        return DELAY_LEVEL;
    }

    /**
     * 消息分发处理的主方法
     * 实现了模板方法模式，定义了消息处理的标准流程：
     * 1. 消息日志记录
     * 2. 消息过滤
     * 3. 重试次数检查
     * 4. 具体消息处理
     * 5. 异常处理和重试
     */
    public void dispatchMessage(T message) {
        // 记录收到的消息
        log.info("消费者收到消息[{}]", JSONObject.toJSON(message));

        // 执行消息过滤
        if (filter(message)) {
            log.info("消息业务键{}不满足消费条件，已过滤。", message.getKey());
            return;
        }

        // 检查重试次数是否超限
        if (message.getRetryTimes() > getMaxRetryTimes()) {
            log.warn("超过最大重试次数,尝试调用子类方法处理重试");
            handleMaxRetriesExceeded(message);
            return;
        }

        try {
            // 记录处理开始时间
            long now = System.currentTimeMillis();
            // 调用具体的消息处理逻辑
            handleMessage(message);
            // 计算处理耗时
            long costTime = System.currentTimeMillis() - now;
            log.info("消息{}消费成功，耗时[{}ms]", message.getKey(), costTime);
        } catch (Exception e) {
            // 记录异常信息
            log.error("消息{}消费异常", message.getKey(), e);
            
            // 根据配置决定异常处理方式
            if (throwException()) {
                // 抛出异常，交由RocketMQ的DefaultMessageListenerConcurrently处理
                throw new RuntimeException(e);
            }
            
            // 判断是否需要重试
            if (isRetry()) {
                handleRetry(message);
            }
        }
    }

    /**
     * 处理消息重试
     * 实现了消息重试的具体逻辑：
     * 1. 获取消息的目标主题和标签
     * 2. 更新消息的重试信息
     * 3. 重新发送消息
     * 4. 处理发送结果
     */
    protected void handleRetry(T message) {
        // 获取消息监听器的配置信息
        RocketMQMessageListener annotation = this.getClass().getAnnotation(RocketMQMessageListener.class);
        if (annotation == null) {
            return;
        }

        // 处理消息源信息，添加重试标识
        String messageSource = message.getSource();
        if (!messageSource.startsWith(retryPrefix())) {
            message.setSource(retryPrefix() + messageSource);
        }
        // 增加重试次数
        message.setRetryTimes(message.getRetryTimes() + 1);

        SendResult sendResult;

        try {
            // 重新发送消息，使用配置的延迟级别
            sendResult = winterRocketMQTemplate.send(annotation.topic(), annotation.selectorExpression(), message, getDelayLevel());
        } catch (Exception ex) {
            // 发送异常，抛出运行时异常，由RocketMQ重试
            throw new RuntimeException(ex);
        }

        // 检查发送结果，如果发送失败，抛出异常触发重试
        if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
            throw new RuntimeException("重试消息发送失败");
        }
    }
}