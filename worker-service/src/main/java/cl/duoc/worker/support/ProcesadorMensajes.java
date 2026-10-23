package cl.duoc.worker.support;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Política única de confirmación (ACK manual) para todos los consumidores:
 *
 *  - Procesado correctamente ............ basicAck  -> el mensaje sale de la cola
 *  - Error NO recuperable (datos) ........ basicNack(requeue=false) -> DLQ inmediata
 *  - Error transitorio, 1.er intento ..... basicNack(requeue=true)  -> se reintenta una vez
 *  - Error transitorio, ya reintentado ... basicNack(requeue=false) -> DLQ
 *
 * Así no hay reprocesos infinitos ni pérdida de mensajes.
 */
@Component
public class ProcesadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorMensajes.class);

    private final MessageConverter messageConverter;

    public ProcesadorMensajes(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @SuppressWarnings("unchecked")
    public void procesar(String dominio, Message message, Channel channel,
                         Consumer<Map<String, Object>> logicaDeNegocio) throws IOException {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean yaReintentado = Boolean.TRUE.equals(message.getMessageProperties().isRedelivered());

        try {
            Map<String, Object> evento = (Map<String, Object>) messageConverter.fromMessage(message);
            logicaDeNegocio.accept(evento);
            channel.basicAck(deliveryTag, false);
            log.info("[{}] ACK -> pedido {} procesado", dominio, evento.get("id"));

        } catch (MensajeNoRecuperableException | MessageConversionException e) {
            log.error("[{}] Error NO recuperable: {} -> NACK sin reencolar (va a la DLQ)", dominio, e.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            if (!yaReintentado) {
                log.warn("[{}] Error transitorio: {} -> NACK con reencolado (1 reintento)", dominio, e.getMessage());
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("[{}] Error transitorio persistente: {} -> NACK sin reencolar (va a la DLQ)", dominio, e.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }
}
