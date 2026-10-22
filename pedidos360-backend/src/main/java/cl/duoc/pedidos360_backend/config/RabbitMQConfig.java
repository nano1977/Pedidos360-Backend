package cl.duoc.pedidos360_backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.dlq}")
    private String dlqName;

    @Value("${app.rabbitmq.dlx}")
    private String dlxName;

    @Value("${app.rabbitmq.dlx-routing-key}")
    private String dlxRoutingKey;

    // ===== Dead Letter (mensajes fallidos) =====
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxName);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(dlxRoutingKey);
    }

    // ===== Cola principal (redirige a la DLQ si falla) =====
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlxRoutingKey)
                .build();
    }

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue())
                .to(mainExchange())
                .with(routingKey);
    }

    // ===== Administración de colas vía API REST =====
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // ===== Mensajes en formato JSON =====
    // Spring Boot lo aplica automáticamente al RabbitTemplate y a los @RabbitListener
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}