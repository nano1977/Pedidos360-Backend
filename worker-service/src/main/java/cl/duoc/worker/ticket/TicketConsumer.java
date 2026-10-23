package cl.duoc.worker.ticket;

import cl.duoc.worker.support.ProcesadorMensajes;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/** Dominio TICKETS: recibe ticket.generar desde el exchange direct. */
@Component
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);

    private final ProcesadorMensajes procesador;

    public TicketConsumer(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.tickets}")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar("TICKET", message, channel, this::generarTicket);
    }

    private void generarTicket(Map<String, Object> evento) {
        // Simulación de una caída temporal del sistema de tickets (para demostrar el reintento)
        if (String.valueOf(evento.get("cliente")).contains("FALLA-TICKET")) {
            throw new IllegalStateException("sistema de tickets no disponible");
        }
        log.info("[TICKET] Ticket TCK-{} generado para '{}'", evento.get("id"), evento.get("cliente"));
    }
}
