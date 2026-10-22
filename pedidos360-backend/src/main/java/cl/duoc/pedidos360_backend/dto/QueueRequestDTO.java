package cl.duoc.pedidos360_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QueueRequestDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
             message = "Solo se permiten letras, números, punto, guion y guion bajo")
    private String name;

    private boolean durable = true;

    public QueueRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
}