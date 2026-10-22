package cl.duoc.pedidos360_backend.service;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public void crearCola(String nombre, boolean durable) {
        rabbitAdmin.declareQueue(new Queue(nombre, durable));
    }

    public boolean existeCola(String nombre) {
        return rabbitAdmin.getQueueProperties(nombre) != null;
    }

    public void eliminarCola(String nombre) {
        rabbitAdmin.deleteQueue(nombre);
    }
}