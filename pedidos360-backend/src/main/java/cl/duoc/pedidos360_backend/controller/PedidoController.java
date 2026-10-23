package cl.duoc.pedidos360_backend.controller;

import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.producer.PedidoProducer;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "http://localhost:4200")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoRepository pedidoRepository;
    private final PedidoProducer pedidoProducer;

    public PedidoController(PedidoRepository pedidoRepository, PedidoProducer pedidoProducer) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoProducer = pedidoProducer;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> obtenerPedidos() {
        return ResponseEntity.ok(pedidoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Pedido> crearPedido(@RequestBody Pedido pedido) {
        // 1. Guardado síncrono en Azure PostgreSQL
        Pedido nuevoPedido = pedidoRepository.save(pedido);

        // 2. Evento asíncrono a RabbitMQ
        Map<String, Object> evento = new HashMap<>();
        evento.put("tipo", "PedidoCreado");
        evento.put("id", nuevoPedido.getId());
        evento.put("cliente", nuevoPedido.getCliente());
        evento.put("total", nuevoPedido.getTotal());

        try {
            pedidoProducer.publicarPedidoCreado(evento);
        } catch (Exception e) {
            // Si RabbitMQ está caído, el pedido igual queda guardado
            log.warn("[CONTROLLER] No se pudo publicar el evento en RabbitMQ: {}", e.getMessage());
        }

        return ResponseEntity.ok(nuevoPedido);
    }
}
