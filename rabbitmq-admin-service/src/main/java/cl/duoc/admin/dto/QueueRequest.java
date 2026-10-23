package cl.duoc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear una cola. durable es opcional (por defecto true). */
public record QueueRequest(
        @NotBlank(message = "El nombre de la cola no puede estar vacío")
        @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, números, punto, guion y guion bajo")
        String name,

        Boolean durable
) {
    public boolean esDurable() {
        return durable == null || durable;
    }
}
