package cl.duoc.worker.notificacion;

import cl.duoc.worker.support.MensajeNoRecuperableException;
import cl.duoc.worker.support.ProcesadorMensajes;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/** Dominio NOTIFICACIONES: recibe los eventos pedido.* desde el exchange topic. */
@Component
public class NotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConsumer.class);

    private final ProcesadorMensajes procesador;

    public NotificacionConsumer(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.notificaciones}")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar("NOTIFICACION", message, channel, this::enviarNotificacion);
    }

    private void enviarNotificacion(Map<String, Object> evento) {
        Object total = evento.get("total");
        if (total == null || Double.parseDouble(total.toString()) <= 0) {
            throw new MensajeNoRecuperableException("total inválido en pedido " + evento.get("id"));
        }
        log.info("[NOTIFICACION] Correo de confirmación enviado a '{}' por el pedido {}",
                evento.get("cliente"), evento.get("id"));
    }
}
