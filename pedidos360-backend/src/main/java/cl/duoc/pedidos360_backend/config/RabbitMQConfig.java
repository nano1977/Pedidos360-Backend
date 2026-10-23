package cl.duoc.pedidos360_backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de mensajería de Pedidos360.
 * Todos los nombres se leen desde application.yaml (prefijo app.rabbitmq).
 *
 *  pedidos.topic  (topic)  --"pedido.*"----------> notificaciones.queue --(falla)--> notificaciones.dlq
 *  pedidos.direct (direct) --"ticket.generar"-----> tickets.queue        --(falla)--> tickets.dlq
 *  pedidos.direct (direct) --"documento.generar"--> documentos.queue     --(falla)--> documentos.dlq
 *
 * Cada cola principal declara x-dead-letter-exchange = pedidos.dlx, que enruta
 * el mensaje rechazado a su DLQ usando el nombre de la DLQ como routing key.
 */
@Configuration
public class RabbitMQConfig {

    // ===== Exchanges =====
    @Value("${app.rabbitmq.exchanges.topic}")
    private String topicExchangeName;

    @Value("${app.rabbitmq.exchanges.direct}")
    private String directExchangeName;

    @Value("${app.rabbitmq.exchanges.dlx}")
    private String dlxExchangeName;

    // ===== Colas principales =====
    @Value("${app.rabbitmq.queues.notificaciones}")
    private String notificacionesQueueName;

    @Value("${app.rabbitmq.queues.tickets}")
    private String ticketsQueueName;

    @Value("${app.rabbitmq.queues.documentos}")
    private String documentosQueueName;

    // ===== Dead Letter Queues =====
    @Value("${app.rabbitmq.dlqs.notificaciones}")
    private String notificacionesDlqName;

    @Value("${app.rabbitmq.dlqs.tickets}")
    private String ticketsDlqName;

    @Value("${app.rabbitmq.dlqs.documentos}")
    private String documentosDlqName;

    // ===== Routing keys de los bindings =====
    @Value("${app.rabbitmq.routing-keys.notificaciones-pattern}")
    private String notificacionesPattern;

    @Value("${app.rabbitmq.routing-keys.tickets}")
    private String ticketsRoutingKey;

    @Value("${app.rabbitmq.routing-keys.documentos}")
    private String documentosRoutingKey;

    // ------------------------------------------------------------------
    // Exchanges
    // ------------------------------------------------------------------

    /** Exchange topic: enruta por patrón (pedido.*). */
    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(topicExchangeName, true, false);
    }

    /** Exchange direct: enruta por coincidencia exacta de routing key. */
    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(directExchangeName, true, false);
    }

    /** Dead Letter Exchange: recibe los mensajes rechazados de las tres colas. */
    @Bean
    public DirectExchange pedidosDlxExchange() {
        return new DirectExchange(dlxExchangeName, true, false);
    }

    // ------------------------------------------------------------------
    // Caso de uso 1: NOTIFICACIONES (exchange topic)
    // ------------------------------------------------------------------

    @Bean
    public Queue notificacionesQueue() {
        return colaConDlq(notificacionesQueueName, notificacionesDlqName);
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(notificacionesDlqName).build();
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosTopicExchange()).with(notificacionesPattern);
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(pedidosDlxExchange()).with(notificacionesDlqName);
    }

    // ------------------------------------------------------------------
    // Caso de uso 2: TICKETS (exchange direct)
    // ------------------------------------------------------------------

    @Bean
    public Queue ticketsQueue() {
        return colaConDlq(ticketsQueueName, ticketsDlqName);
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(ticketsDlqName).build();
    }

    @Bean
    public Binding ticketsBinding() {
        return BindingBuilder.bind(ticketsQueue()).to(pedidosDirectExchange()).with(ticketsRoutingKey);
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return BindingBuilder.bind(ticketsDlq()).to(pedidosDlxExchange()).with(ticketsDlqName);
    }

    // ------------------------------------------------------------------
    // Caso de uso 3: DOCUMENTOS (exchange direct)
    // ------------------------------------------------------------------

    @Bean
    public Queue documentosQueue() {
        return colaConDlq(documentosQueueName, documentosDlqName);
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(documentosDlqName).build();
    }

    @Bean
    public Binding documentosBinding() {
        return BindingBuilder.bind(documentosQueue()).to(pedidosDirectExchange()).with(documentosRoutingKey);
    }

    @Bean
    public Binding documentosDlqBinding() {
        return BindingBuilder.bind(documentosDlq()).to(pedidosDlxExchange()).with(documentosDlqName);
    }

    // ------------------------------------------------------------------
    // Infraestructura común
    // ------------------------------------------------------------------

    /** Cola durable cuyos mensajes rechazados viajan por el DLX hacia su DLQ. */
    private Queue colaConDlq(String nombreCola, String nombreDlq) {
        return QueueBuilder.durable(nombreCola)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", nombreDlq)
                .build();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /** Los mensajes viajan como JSON. Spring Boot lo aplica al RabbitTemplate. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
