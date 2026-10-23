package cl.duoc.admin.exception;

/** Operación no permitida, por ejemplo sobre recursos reservados de RabbitMQ (400). */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
