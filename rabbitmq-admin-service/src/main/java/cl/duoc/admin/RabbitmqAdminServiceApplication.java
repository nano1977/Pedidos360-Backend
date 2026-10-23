package cl.duoc.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** rabbitmq-admin-service: administra colas, exchanges y bindings mediante una API REST. */
@SpringBootApplication
public class RabbitmqAdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitmqAdminServiceApplication.class, args);
    }
}
