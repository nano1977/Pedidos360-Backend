
package cl.duoc.pedidos360_backend.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PedidoProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public PedidoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoCreado(Map<String, Object> evento) {
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        log.info("[PRODUCER] Evento PedidoCreado publicado: {}", evento);
    }
}