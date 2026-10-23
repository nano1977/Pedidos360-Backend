package cl.duoc.admin.exception;

/** La cola o el exchange ya existe (409). */
public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
