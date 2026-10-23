package cl.duoc.pedidos360_backend.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publica los eventos de un pedido. Los servicios de negocio solo llaman a este
 * componente; no conocen exchanges ni routing keys.
 */
@Component
public class PedidoProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchanges.topic}")
    private String topicExchange;

    @Value("${app.rabbitmq.exchanges.direct}")
    private String directExchange;

    @Value("${app.rabbitmq.routing-keys.pedido-creado}")
    private String pedidoCreadoRoutingKey;

    @Value("${app.rabbitmq.routing-keys.tickets}")
    private String ticketsRoutingKey;

    @Value("${app.rabbitmq.routing-keys.documentos}")
    private String documentosRoutingKey;

    public PedidoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Un pedido creado genera tres tareas asíncronas: notificación, ticket y documento. */
    public void publicarPedidoCreado(Map<String, Object> evento) {
        enviar("NOTIFICACION", topicExchange, pedidoCreadoRoutingKey, evento);
        enviar("TICKET", directExchange, ticketsRoutingKey, evento);
        enviar("DOCUMENTO", directExchange, documentosRoutingKey, evento);
    }

    private void enviar(String destino, String exchange, String routingKey, Map<String, Object> evento) {
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        log.info("[PRODUCER] {} -> exchange={} routingKey={} evento={}", destino, exchange, routingKey, evento);
    }
}
