package cl.duoc.admin.controller;

import cl.duoc.admin.dto.BindingRequest;
import cl.duoc.admin.dto.ExchangeRequest;
import cl.duoc.admin.dto.QueueRequest;
import cl.duoc.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API REST de administración de RabbitMQ. Requiere un JWT con ROLE_ADMIN.
 *
 *  POST   /api/admin/queues            crea una cola
 *  DELETE /api/admin/queues/{name}     elimina una cola
 *  POST   /api/admin/exchanges         crea un exchange (direct, topic o fanout)
 *  DELETE /api/admin/exchanges/{name}  elimina un exchange
 *  POST   /api/admin/bindings          une una cola a un exchange
 *  DELETE /api/admin/bindings          elimina esa unión
 */
@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> crearCola(@Valid @RequestBody QueueRequest req) {
        adminService.crearCola(req.name(), req.esDurable());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cola creada", "queue", req.name(), "durable", req.esDurable()));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> eliminarCola(@PathVariable String name) {
        adminService.eliminarCola(name);
        return ResponseEntity.ok(Map.of("mensaje", "Cola eliminada", "queue", name));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> crearExchange(@Valid @RequestBody ExchangeRequest req) {
        adminService.crearExchange(req.name(), req.type());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Exchange creado", "exchange", req.name(), "type", req.type()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Map<String, Object>> eliminarExchange(@PathVariable String name) {
        adminService.eliminarExchange(name);
        return ResponseEntity.ok(Map.of("mensaje", "Exchange eliminado", "exchange", name));
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> crearBinding(@Valid @RequestBody BindingRequest req) {
        adminService.crearBinding(req.queue(), req.exchange(), req.routingKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Binding creado", "queue", req.queue(),
                "exchange", req.exchange(), "routingKey", req.routingKey()));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, Object>> eliminarBinding(@Valid @RequestBody BindingRequest req) {
        adminService.eliminarBinding(req.queue(), req.exchange(), req.routingKey());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Binding eliminado", "queue", req.queue(),
                "exchange", req.exchange(), "routingKey", req.routingKey()));
    }
}
