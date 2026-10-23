package cl.duoc.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * worker-service: procesa en segundo plano las tareas asíncronas de Pedidos360.
 * No expone endpoints HTTP; solo consume mensajes de RabbitMQ.
 */
@SpringBootApplication
public class WorkerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerServiceApplication.class, args);
    }
}
