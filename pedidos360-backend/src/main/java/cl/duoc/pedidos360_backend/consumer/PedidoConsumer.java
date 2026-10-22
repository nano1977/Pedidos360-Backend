package cl.duoc.pedidos360_backend.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoConsumer.class);

    // Cola principal: procesa los pedidos en segundo plano
    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void procesarPedido(Map<String, Object> evento) {
        log.info("[CONSUMER] Pedido recibido desde RabbitMQ: {}", evento);

        // Regla de negocio: un pedido con total <= 0 es inválido.
        // Al lanzar la excepción, Spring reintenta 3 veces y luego lo envía a la DLQ.
        Object total = evento.get("total");
        if (total == null || Double.parseDouble(total.toString()) <= 0) {
            throw new IllegalArgumentException("Pedido inválido: total debe ser mayor a 0");
        }

        log.info("[CONSUMER] Pedido procesado OK (notificación/ticket generado) para cliente: {}",
                evento.get("cliente"));
        // Si termina sin excepción → ACK automático: RabbitMQ elimina el mensaje de la cola
    }

    // Dead Letter Queue: recibe los mensajes que fallaron
    @RabbitListener(queues = "${app.rabbitmq.dlq}")
    public void procesarFallido(Map<String, Object> evento) {
        log.error("[DLQ] Mensaje fallido recibido en pedidos.dlq: {}", evento);
    }
}