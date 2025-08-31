# Winter Rocket Spring Boot Starter 🚀

[![GitHub stars](https://img.shields.io/github/stars/hahaha-zsq/winter-rocketmq-spring-boot-starter.svg?style=social&label=Stars)](https://github.com/hahaha-zsq/winter-rocketmq-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-8%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.11-brightgreen)
![RocketMQ](https://img.shields.io/badge/RocketMQ-5.3.0-005571)
![Lombok](https://img.shields.io/badge/Lombok-1.18.22-orange)

## 简介 ✨

Winter RocketMQ Spring Boot Starter 是对原生 RocketMQ Spring Boot Starter 的增强实现，提供了以下核心特性：

1. 🕒 完美支持 Java 8 中的 LocalDate 和 LocalDateTime 类型
2. 🌍 环境隔离：自动为不同环境（如dev、test、prod）的消息添加环境后缀
3. 🔄 强大的重试机制和异常处理
4. 📝 详细的日志追踪
5. ⏰ 灵活的延迟消息支持
6. 🔢 顺序消息发送支持
7. 💼 事务消息支持
8. 📦 批量消息发送支持

## 快速开始 🚀

### 1. 项目引入 📌

在你的项目 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>io.github.hahaha-zsq</groupId>
    <artifactId>winter-rocket-springboot-starter</artifactId>
    <version>xxx</version>
</dependency>
```

### 2. 配置文件 📝

在你的 `application.properties` 或 `application.yml` 中添加 RocketMQ 配置：

```yaml
# Winter RocketMQ 增强配置
winter-rocketmq:
  enabled-isolation: true  # 启用环境隔离
  environment: dev         # 环境标识

# RocketMQ 原生配置
rocketmq:
  # 消费者配置 👥
  consumer:
    group: springboot_consumer_group
    pull-batch-size: 10  # 单次拉取消息最大数
  
  # 生产者配置 📤
  producer:
    group: springboot_producer_group
    sendMessageTimeout: 10000  # 消息发送超时时间(ms)
    retryTimesWhenSendFailed: 2  # 同步发送失败重试次数
    retryTimesWhenSendAsyncFailed: 2  # 异步发送失败重试次数
    maxMessageSize: 4096  # 消息最大长度(默认4M)
    compressMessageBodyThreshold: 4096  # 消息压缩阈值(默认4K)
    retryNextServer: false  # 是否在内部发送失败时重试其他broker
  
  # 服务器配置 🖥️
  name-server: 127.0.0.1:9876
```

## 核心功能 💡

### 1. 消息实体定义 📨

所有消息实体类必须继承 `BaseMessage`：

```java
@Data
public class OrderMessage extends BaseMessage {
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime createTime;
    private LocalDate orderDate;
}
```

### 2. 消息发送功能 📤

使用 `WinterRocketMQTemplate` 发送消息，支持以下发送模式：

#### 2.1 同步消息发送

```java
@RestController
@RequestMapping("/message")
@Slf4j
public class MessageController {
    @Resource
    private WinterRocketMQTemplate winterRocketMQTemplate;
    
    private static final String TOPIC = "order_topic";
    private static final String TAG = "create";
    
    @PostMapping("/send")
    public Result<?> sendMessage(@RequestBody OrderMessage message) {
        // 设置消息属性
        message.setKey(IdUtil.randomUUID().toString());
        message.setSource("ORDER_SERVICE");
        message.setRetryTimes(0);
        message.setSendTime(LocalDateTime.now());
        
        // 发送同步消息
        SendResult result = winterRocketMQTemplate.send(TOPIC, TAG, message);
        return Result.ok(result);
    }
}
```

#### 2.2 异步消息发送

```java
@PostMapping("/async-send")
public Result<?> asyncSendMessage(@RequestBody OrderMessage message) {
    message.setKey(IdUtil.randomUUID().toString());
    message.setSource("ORDER_SERVICE");
    
    // 异步发送消息
    winterRocketMQTemplate.asyncSend(TOPIC, TAG, message);
    return Result.ok("消息已异步发送");
}
```

#### 2.3 单向消息发送

```java
@PostMapping("/oneway-send")
public Result<?> onewaySendMessage(@RequestBody OrderMessage message) {
    message.setKey(IdUtil.randomUUID().toString());
    message.setSource("ORDER_SERVICE");
    
    // 单向发送消息（不关心发送结果）
    winterRocketMQTemplate.sendOneWay(TOPIC, TAG, message);
    return Result.ok("消息已单向发送");
}
```

#### 2.4 延迟消息发送

```java
@PostMapping("/delay-send")
public Result<?> delaySendMessage(@RequestBody OrderMessage message) {
    message.setKey(IdUtil.randomUUID().toString());
    message.setSource("ORDER_SERVICE");
    
    // 发送延迟消息（延迟5分钟）
    SendResult result = winterRocketMQTemplate.send(TOPIC, TAG, message, DelayConstant.FIVE_MINUTES);
    return Result.ok(result);
}
```

#### 2.5 顺序消息发送

```java
@PostMapping("/orderly-send")
public Result<?> orderlySendMessage(@RequestBody OrderMessage message) {
    message.setKey(message.getOrderId()); // 使用订单ID作为消息键，确保同一订单的消息顺序
    message.setSource("ORDER_SERVICE");
    
    // 顺序发送消息
    SendResult result = winterRocketMQTemplate.sendOrderly(TOPIC, TAG, message);
    return Result.ok(result);
}
```

#### 2.6 事务消息发送

```java
@PostMapping("/transaction-send")
public Result<?> transactionSendMessage(@RequestBody OrderMessage message) {
    message.setKey(IdUtil.randomUUID().toString());
    message.setSource("ORDER_SERVICE");
    
    // 发送事务消息
    TransactionSendResult result = winterRocketMQTemplate.sendMessageInTransaction(TOPIC, TAG, message);
    return Result.ok(result);
}
```

#### 2.7 批量消息发送

```java
@PostMapping("/batch-send")
public Result<?> batchSendMessage(@RequestBody List<OrderMessage> messages) {
    // 设置消息属性
    messages.forEach(message -> {
        message.setKey(IdUtil.randomUUID().toString());
        message.setSource("ORDER_SERVICE");
        message.setSendTime(LocalDateTime.now());
    });
    
    // 批量发送消息
    SendResult result = winterRocketMQTemplate.sendBatch(TOPIC, TAG, messages);
    return Result.ok(result);
}
```

### 3. 消息消费功能 📥

创建消息监听器需继承 `EnhanceMessageHandler`：

```java
@Slf4j
@Component
@RocketMQMessageListener(
    consumerGroup = "order_consumer_group",
    topic = "order_topic",
    selectorExpression = "*",
    consumeThreadMax = 5
)
public class OrderMessageListener extends EnhanceMessageHandler<OrderMessage> 
    implements RocketMQListener<OrderMessage> {

    public OrderMessageListener(WinterRocketMQTemplate winterRocketMQTemplate) {
        super(winterRocketMQTemplate);
    }

    @Override
    protected void handleMessage(OrderMessage message) throws Exception {
        // 实现具体的业务逻辑
        log.info("处理订单消息: {}", message.getOrderId());
        // 处理订单业务逻辑...
    }

    @Override
    protected void handleMaxRetriesExceeded(OrderMessage message) {
        // 处理超过重试次数的消息
        log.error("订单消息处理失败，超过最大重试次数: {}", message.getOrderId());
        // 执行补偿逻辑，如发送告警、记录失败日志等
    }

    @Override
    protected boolean isRetry() {
        return true;  // 启用重试机制
    }

    @Override
    protected String retryPrefix() {
        return "retry:";  // 重试消息前缀
    }

    @Override
    protected boolean throwException() {
        return false;  // 异常处理方式：内部处理，不抛给RocketMQ
    }

    @Override
    public void onMessage(OrderMessage message) {
        super.dispatchMessage(message);
    }
}
```

## 消息发送功能完整列表 📋

### 基础消息发送
| 方法 | 说明 | 适用场景 |
|------|------|----------|
| `send(topic, tag, message)` | 同步发送消息 | 需要确认发送结果的场景 |
| `send(destination, message)` | 同步发送消息到指定目的地 | 直接指定完整目的地 |
| `asyncSend(topic, tag, message)` | 异步发送消息 | 高性能场景，不关心发送结果 |
| `asyncSend(destination, message)` | 异步发送消息到指定目的地 | 异步发送到指定目的地 |
| `sendOneWay(topic, tag, message)` | 单向发送消息 | 最高性能场景，完全不关心结果 |
| `sendOneWay(destination, message)` | 单向发送消息到指定目的地 | 单向发送到指定目的地 |

### 延迟消息发送
| 方法 | 说明 | 适用场景 |
|------|------|----------|
| `send(topic, tag, message, delayLevel)` | 发送延迟消息 | 定时任务、延迟处理 |
| `send(destination, message, delayLevel)` | 发送延迟消息到指定目的地 | 延迟发送到指定目的地 |

### 顺序消息发送
| 方法 | 说明 | 适用场景 |
|------|------|----------|
| `sendOrderly(topic, tag, message)` | 顺序发送同步消息 | 需要保证消息顺序的场景 |
| `sendOrderly(destination, message)` | 顺序发送同步消息到指定目的地 | 顺序发送到指定目的地 |
| `sendOrderly(topic, tag, message, delayLevel)` | 顺序发送延迟消息 | 需要顺序且延迟的场景 |
| `sendOrderly(destination, message, delayLevel)` | 顺序发送延迟消息到指定目的地 | 顺序延迟发送到指定目的地 |
| `asyncSendOrderly(topic, tag, message)` | 顺序异步发送消息 | 顺序发送但不关心结果 |
| `asyncSendOrderly(destination, message)` | 顺序异步发送消息到指定目的地 | 顺序异步发送到指定目的地 |
| `sendOneWayOrderly(topic, tag, message)` | 顺序单向发送消息 | 最高性能的顺序发送 |
| `sendOneWayOrderly(destination, message)` | 顺序单向发送消息到指定目的地 | 顺序单向发送到指定目的地 |

### 批量消息发送
| 方法 | 说明 | 适用场景 |
|------|------|----------|
| `sendBatch(topic, tag, messages)` | 批量发送消息 | 大量消息一次性发送 |
| `sendBatch(destination, messages)` | 批量发送消息到指定目的地 | 批量发送到指定目的地 |
| `sendBatchOrderly(topic, tag, messages)` | 批量顺序发送消息 | 批量且需要顺序的场景 |
| `sendBatchOrderly(destination, messages)` | 批量顺序发送消息到指定目的地 | 批量顺序发送到指定目的地 |

### 事务消息发送
| 方法 | 说明 | 适用场景 |
|------|------|----------|
| `sendMessageInTransaction(topic, tag, message)` | 发送事务消息 | 分布式事务场景 |
| `sendMessageInTransaction(topic, tag, message, arg)` | 发送事务消息（带参数） | 需要传递参数的事务场景 |
| `sendMessageInTransaction(destination, message, arg)` | 发送事务消息到指定目的地 | 事务消息发送到指定目的地 |

## 延迟等级常量 ⏰

使用 `DelayConstant` 接口中定义的常量：

```java
// 常用延迟等级
DelayConstant.ONE_SECOND      // 1秒
DelayConstant.FIVE_SECOND     // 5秒
DelayConstant.TEN_SECOND      // 10秒
DelayConstant.THIRTY_SECOND   // 30秒
DelayConstant.ONE_MINUTES     // 1分钟
DelayConstant.FIVE_MINUTES    // 5分钟
DelayConstant.TEN_MINUTES     // 10分钟
DelayConstant.TWENTY_MINUTES  // 20分钟
DelayConstant.THIRTY_MINUTES  // 30分钟
DelayConstant.ONE_HOURS       // 1小时
DelayConstant.TWO_HOURS       // 2小时
```

## 高级特性 🌟

### 1. 环境隔离 🌍

通过配置 `winter-rocketmq.environment`，框架会自动为消息Topic添加环境后缀：

- 原始Topic: `order_topic`
- 环境配置: `dev`
- 最终Topic: `order_topic_dev`

### 2. 消息重试机制 🔄

框架提供了灵活的重试机制：

- 可配置最大重试次数（默认3次）
- 支持延迟重试（默认5秒）
- 自定义重试策略
- 失败消息处理

### 3. 消息过滤 🚫

可以通过重写 `filter()` 方法实现消息过滤：

```java
@Override
protected boolean filter(OrderMessage message) {
    // 过滤无效订单
    return message.getAmount() == null || message.getAmount().compareTo(BigDecimal.ZERO) <= 0;
}
```

## 注意事项 ⚠️

1. **消息实体类**：所有消息实体类必须继承 `BaseMessage`
2. **消息监听器**：消息监听器必须继承 `EnhanceMessageHandler`
3. **环境隔离**：环境隔离功能默认开启
4. **重试机制**：重试次数默认最大为3次
5. **顺序消息**：顺序消息使用消息的 `key` 作为hash key，确保相同key的消息发送到同一队列
6. **事务消息**：需要实现 `TransactionListener` 接口处理本地事务

## 最佳实践 💡

1. **合理配置消费线程数**：根据业务复杂度和机器性能调整
2. **使用适当的重试策略**：根据业务特点选择合适的重试次数和延迟时间
3. **正确处理消息幂等性**：确保消息重复消费不会产生副作用
4. **做好监控和日志记录**：便于问题排查和性能优化
5. **合理使用顺序消息**：只在真正需要顺序的场景使用，避免影响性能
6. **批量发送优化**：大量消息优先使用批量发送提高性能

## 贡献指南 🤝

欢迎提交 Issue 和 Pull Request！

## 许可证 📄

本项目采用 MIT 许可证

---

🎉 感谢使用 Winter RocketMQ Spring Boot Starter！如有问题，欢迎反馈！
