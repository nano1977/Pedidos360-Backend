package cl.duoc.pedidos360_backend.controller;

import cl.duoc.pedidos360_backend.dto.QueueRequestDTO;
import cl.duoc.pedidos360_backend.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/queues")
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    // POST /api/admin/queues  → crea una cola
    @PostMapping
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody QueueRequestDTO dto) {
        if (adminService.existeCola(dto.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "La cola ya existe", "queue", dto.getName()));
        }
        adminService.crearCola(dto.getName(), dto.isDurable());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Cola creada exitosamente", "queue", dto.getName()));
    }

    // DELETE /api/admin/queues/{name}  → elimina una cola
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> eliminarCola(@PathVariable String name) {
        if (!adminService.existeCola(name)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La cola no existe", "queue", name));
        }
        adminService.eliminarCola(name);
        return ResponseEntity.ok(Map.of("message", "Cola eliminada exitosamente", "queue", name));
    }
}