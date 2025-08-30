package com.zsq.winter.rocketmq.config;

import com.zsq.winter.rocketmq.config.WinterRocketMQProperties;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringUtils;
/*
现状：
在使用RocketMQ时，通常会在代码中直接指定消息主题(topic)，而且开发环境和测试环境可能共用一个RocketMQ环境。
如果没有进行处理，在开发环境发送的消息就可能被测试环境的消费者消费，测试环境发送的消息也可能被开发环境的消费者消费，从而导致数据混乱的问题。
解决方案：
我们可以根据不同的环境实现自动隔离。通过简单配置一个选项，如dev、test、prod等不同环境，所有的消息都会被自动隔离。
例如，当发送的消息主题为consumer_topic时，可以自动在topic后面加上环境后缀，如consumer_topic_dev。
具体实现步骤：
可以编写一个配置类实现BeanPostProcessor，并重写postProcessBeforeInitialization方法，在监听器实例初始化前修改对应的topic。
BeanPostProcessor是Spring框架中的一个接口，它的作用是在Spring容器实例化、配置完bean之后，在bean初始化前后进行一些额外的处理工作
它可以对所有的Bean类实例进行增强处理，使得开发人员可以在Bean初始化前后自定义一些操作，从而实现自己的业务需求。比如，可以通过BeanPostProcessor来实现注入某些必要的属性值、加入某一个对象等等。
*/
public class EnvironmentIsolationConfig implements BeanPostProcessor {
    private final WinterRocketMQProperties winterRocketMQTemplate;

    public EnvironmentIsolationConfig(WinterRocketMQProperties winterRocketMQTemplate) {
        this.winterRocketMQTemplate = winterRocketMQTemplate;
    }


    /**
     * postProcessBeforeInitialization在bean初始化之前进行处理，可以对bean做一些修改等操作。
     * postProcessAfterInitialization在bean初始化之后进行处理，可以进行一些清理或者其他操作。BeanPostProcessor可以在应用程序中对Bean的创建和初始化过程进行拦截和修改，对Bean的生命周期进行干预和操作。
     * 在装载Bean之前实现参数修改
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof DefaultRocketMQListenerContainer){
           /*
            DefaultRocketMQListenerContainer 是 Apache RocketMQ 与 Spring 框架集成时使用的一个关键组件，它主要用于简化 RocketMQ 消息的接收处理过程，实现了消息监听的容器化管理。具体来说，这个类的作用主要包括：

            自动消费配置：它允许开发者以声明式的方式配置RocketMQ消费者，通过Spring的配置文件或者注解来设置消费者的相关属性，如消费组ID、服务地址、消费模式（集群消费或广播消费）等，而无需手动创建线程来拉取消息。

            消息监听与处理：DefaultRocketMQListenerContainer 负责监听指定主题的RocketMQ消息队列，并根据配置调用用户自定义的监听器（实现了 MessageListener 接口的类）来处理消息。当消息到达时，它会自动调用监听器的回调方法，如 onMessage() 方法来处理消息。

            生命周期管理：作为Spring容器管理的一个Bean，DefaultRocketMQListenerContainer 支持自动启动与停止，其生命周期可以被Spring容器完全管理。这意味着在应用启动时自动开始监听消息，而在应用关闭时能够优雅地停止消息消费，确保消息处理的完整性。

            异常处理与重试机制：当消息处理过程中出现异常时，它可以配置错误处理策略，比如自动重试、死信队列处理等，增强了消息消费的健壮性。

            并发消费控制：它支持配置并发消费的线程数，允许用户根据实际场景调整消息处理的并发能力，从而优化消费性能。

            健康检查与监控：提供了一些监控指标和健康检查功能，便于集成到微服务监控体系中，确保消息系统的稳定性和可观察性。
            */

            DefaultRocketMQListenerContainer container = (DefaultRocketMQListenerContainer) bean;

            if(winterRocketMQTemplate.isEnabledIsolation() && StringUtils.hasText(winterRocketMQTemplate.getEnvironment())){
                container.setTopic(String.join("_", container.getTopic(),winterRocketMQTemplate.getEnvironment()));
            }
            return container;
        }
        return bean;
    }
}