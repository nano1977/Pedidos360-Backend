package cl.duoc.worker.support;

/**
 * Error que no se soluciona reintentando (por ejemplo, datos inválidos).
 * El mensaje se rechaza sin reencolar y va directo a su DLQ.
 */
public class MensajeNoRecuperableException extends RuntimeException {

    public MensajeNoRecuperableException(String mensaje) {
        super(mensaje);
    }
}
