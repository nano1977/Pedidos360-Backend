package cl.duoc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear un exchange. type solo acepta direct, topic o fanout. */
public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange no puede estar vacío")
        @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, números, punto, guion y guion bajo")
        String name,

        @NotBlank(message = "El tipo de exchange es obligatorio")
        @Pattern(regexp = "^(direct|topic|fanout)$", message = "El tipo debe ser direct, topic o fanout")
        String type
) {
}
