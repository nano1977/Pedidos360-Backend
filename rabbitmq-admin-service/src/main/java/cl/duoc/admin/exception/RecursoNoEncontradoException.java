package cl.duoc.admin.exception;

/** La cola o el exchange indicado no existe (404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
