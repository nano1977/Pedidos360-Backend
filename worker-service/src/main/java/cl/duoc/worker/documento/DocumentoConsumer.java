package cl.duoc.worker.documento;

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

/** Dominio DOCUMENTOS: recibe documento.generar desde el exchange direct. */
@Component
public class DocumentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentoConsumer.class);

    private final ProcesadorMensajes procesador;

    public DocumentoConsumer(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.documentos}")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar("DOCUMENTO", message, channel, this::generarDocumento);
    }

    private void generarDocumento(Map<String, Object> evento) {
        if (evento.get("id") == null) {
            throw new MensajeNoRecuperableException("evento sin id de pedido");
        }
        log.info("[DOCUMENTO] Comprobante pedido-{}.pdf generado", evento.get("id"));
    }
}
