# Winter RocketMQ Spring Boot Starter 🚀
## 简介 ✨
winter-rocket-spring-boot-starter 是对原生 RocketMQ Spring Boot Starter 的增强实现，提供了以下核心特性：

1. 🕒 完美支持 Java 8 中的 LocalDate 和 LocalDateTime 类型
2. 🌍 环境隔离：自动为不同环境（如dev、test、prod）的消息添加环境后缀
3. 🔄 强大的重试机制和异常处理
4. 📝 详细的日志追踪
5. ⏰ 灵活的延迟消息支持
## 快速开始 🚀
### 1. 项目引入 📌
在你的项目 pom.xml 中添加依赖：
```xml
 <dependency>
    <groupId>io.github.hahaha-zsq</groupId>
    <artifactId>winter-rocket-spring-boot-starter</artifactId>
    <version>xxx</version>
</dependency>
```
### 2. 配置文件 📝
在你的 `application.properties` 或 `application.yml` 中添加 RocketMQ 配置：
```yaml
winter:
  rocketmq:
    enabled-isolation: true  # 启用环境隔离
    environment: dev  # 环境标识
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
### 1. 消息发送 
📨 1.1 定义消息实体

创建消息类需继承 BaseMessage ：
```java
@Data
public class MemberMessage extends BaseMessage {
    private String userName;
    private Integer age;
    private BigDecimal money;
    private LocalDate birthday;
}
```
1.2 发送消息

使用 WinterRocketMQTemplate 发送消息：
```java
@RestController
@RequestMapping("/enhance")
@Slf4j
public class EnhanceProduceController {
    @Resource
    private WinterRocketMQTemplate winterRocketMQTemplate;
    
    private static final String topic = "rocket_enhance";
    private static final String tag = "member";
    
    @PostMapping("/member")
    public Result<?> member(@RequestBody MemberMessage message) {
        // 设置消息属性
        message.setKey(IdUtil.randomUUID().toString());
        message.setSource("MEMBER");
        message.setRetryTimes(1);
        message.setSendTime(LocalDateTime.now());
        
        // 发送消息
        SendResult send = winterRocketMQTemplate.send(topic, tag, message);
        return Result.ok(send);
    }
}
```
### 2. 消息消费 📥
创建消息监听器需继承 EnhanceMessageHandler ：
```java
@Slf4j
@Component
@RocketMQMessageListener(
    consumerGroup = "enhance_consumer_group",
    topic = "rocket_enhance",
    selectorExpression = "*",
    consumeThreadMax = 5
)
public class EnhanceMemberMessageListener extends EnhanceMessageHandler<MemberMessage> 
    implements RocketMQListener<MemberMessage> {

    @Override
    protected void handleMessage(MemberMessage message) throws Exception {
        // 实现具体的业务逻辑
        System.out.println("处理消息: " + message.getUserName());
    }

    @Override
    protected void handleMaxRetriesExceeded(MemberMessage message) {
        // 处理超过重试次数的消息
        log.error("消息处理失败，请执行补偿逻辑");
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
        return false;  // 异常处理方式
    }

    @Override
    public void onMessage(MemberMessage message) {
        super.dispatchMessage(message);
    }
}
```
## 高级特性 🌟
### 1. 环境隔离 🌍
通过配置 rocketmq.enhance.environment ，框架会自动为消息Topic添加环境后缀，实现消息的环境隔离。

例如：

- 原始Topic: order_topic
- 环境配置: dev
- 最终Topic: order_topic_dev
### 2. 消息重试机制 🔄
框架提供了灵活的重试机制：

- 可配置最大重试次数
- 支持延迟重试
- 自定义重试策略
- 失败消息处理
### 3. 延迟消息等级 ⏰
支持多个延迟等级：

- 1级: 1s
- 2级: 5s
- 3级: 10s
- ...
- 18级: 2h
## 注意事项 ⚠️
1. 所有消息实体类必须继承 BaseMessage
2. 消息监听器必须继承 EnhanceMessageHandler
3. 环境隔离功能默认开启
4. 重试次数默认最大为3次
## 最佳实践 💡
1. 合理配置消费线程数
2. 使用适当的重试策略
3. 正确处理消息幂等性
4. 做好监控和日志记录
## 贡献指南 🤝
欢迎提交 Issue 和 Pull Request！

## 许可证 📄
本项目采用 MIT 许可证

🎉 感谢使用 Winter RocketMQ Spring Boot Starter！如有问题，欢迎反馈！
