package cl.duoc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Une una cola a un exchange con una routing key (en topic admite * y #). */
public record BindingRequest(
        @NotBlank(message = "La cola es obligatoria")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de cola inválido")
        String queue,

        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de exchange inválido")
        String exchange,

        @NotNull(message = "La routing key es obligatoria (puede ser vacía para fanout)")
        @Size(max = 100, message = "La routing key no puede superar 100 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._*#-]*$", message = "La routing key solo admite letras, números, punto, guion, * y #")
        String routingKey
) {
}
