package cl.duoc.worker.dlq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Registra en el log todo mensaje que llega a cualquiera de las tres DLQ. */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    @RabbitListener(queues = {
            "${app.rabbitmq.dlqs.notificaciones}",
            "${app.rabbitmq.dlqs.tickets}",
            "${app.rabbitmq.dlqs.documentos}"
    })
    public void registrar(Message message, Channel channel) throws IOException {
        String cola = message.getMessageProperties().getConsumerQueue();
        String cuerpo = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("[DLQ] Mensaje fallido recibido en {}: {}", cola, cuerpo);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
