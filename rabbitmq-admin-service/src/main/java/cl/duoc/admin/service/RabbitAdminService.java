package cl.duoc.admin.service;

import cl.duoc.admin.exception.OperacionInvalidaException;
import cl.duoc.admin.exception.RecursoDuplicadoException;
import cl.duoc.admin.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

/**
 * Toda la lógica de administración de RabbitMQ vive aquí.
 * El controlador solo llama a estos métodos y no conoce RabbitAdmin.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final String PREFIJO_RESERVADO = "amq.";

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    // ---------------------------- Colas ----------------------------

    public void crearCola(String nombre, boolean durable) {
        validarNoReservado(nombre);
        if (existeCola(nombre)) {
            throw new RecursoDuplicadoException("La cola '" + nombre + "' ya existe");
        }
        rabbitAdmin.declareQueue(new Queue(nombre, durable));
        log.info("[ADMIN] Cola creada: {} (durable={})", nombre, durable);
    }

    public void eliminarCola(String nombre) {
        validarNoReservado(nombre);
        if (!existeCola(nombre)) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        rabbitAdmin.deleteQueue(nombre);
        log.info("[ADMIN] Cola eliminada: {}", nombre);
    }

    // -------------------------- Exchanges --------------------------

    public void crearExchange(String nombre, String tipo) {
        validarNoReservado(nombre);
        if (existeExchange(nombre)) {
            throw new RecursoDuplicadoException("El exchange '" + nombre + "' ya existe");
        }
        Exchange exchange = switch (tipo) {
            case "direct" -> new DirectExchange(nombre, true, false);
            case "topic" -> new TopicExchange(nombre, true, false);
            case "fanout" -> new FanoutExchange(nombre, true, false);
            default -> throw new OperacionInvalidaException("Tipo de exchange no soportado: " + tipo);
        };
        rabbitAdmin.declareExchange(exchange);
        log.info("[ADMIN] Exchange creado: {} (tipo={})", nombre, tipo);
    }

    public void eliminarExchange(String nombre) {
        validarNoReservado(nombre);
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        rabbitAdmin.deleteExchange(nombre);
        log.info("[ADMIN] Exchange eliminado: {}", nombre);
    }

    // --------------------------- Bindings ---------------------------

    public void crearBinding(String cola, String exchange, String routingKey) {
        validarColaYExchange(cola, exchange);
        rabbitAdmin.declareBinding(binding(cola, exchange, routingKey));
        log.info("[ADMIN] Binding creado: {} -> {} con routing key '{}'", exchange, cola, routingKey);
    }

    public void eliminarBinding(String cola, String exchange, String routingKey) {
        validarColaYExchange(cola, exchange);
        rabbitAdmin.removeBinding(binding(cola, exchange, routingKey));
        log.info("[ADMIN] Binding eliminado: {} -> {} con routing key '{}'", exchange, cola, routingKey);
    }

    // --------------------------- Apoyo ---------------------------

    private Binding binding(String cola, String exchange, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private void validarColaYExchange(String cola, String exchange) {
        validarNoReservado(exchange);
        if (!existeCola(cola)) {
            throw new RecursoNoEncontradoException("La cola '" + cola + "' no existe");
        }
        if (!existeExchange(exchange)) {
            throw new RecursoNoEncontradoException("El exchange '" + exchange + "' no existe");
        }
    }

    private void validarNoReservado(String nombre) {
        if (nombre.startsWith(PREFIJO_RESERVADO)) {
            throw new OperacionInvalidaException("Los nombres que empiezan con 'amq.' están reservados por RabbitMQ");
        }
    }

    private boolean existeCola(String nombre) {
        return rabbitAdmin.getQueueProperties(nombre) != null;
    }

    /** Pregunta a RabbitMQ si el exchange existe (declaración pasiva). */
    private boolean existeExchange(String nombre) {
        try {
            return Boolean.TRUE.equals(rabbitAdmin.getRabbitTemplate().execute(channel -> {
                channel.exchangeDeclarePassive(nombre);
                return true;
            }));
        } catch (AmqpException e) {
            return false;
        }
    }
}
