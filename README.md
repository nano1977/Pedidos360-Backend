# 📦 Pedidos360 - Backend

Backend del sistema de gestión de pedidos **Pedidos360**, construido con **Spring Boot 4**, integrado con **Azure AD** (autenticación), **Azure PostgreSQL** (persistencia) y **RabbitMQ** (mensajería asíncrona).

---

## 🏗️ Arquitectura

```
[ Angular + MSAL ]
        │  HTTPS
        ▼
[ AWS API Gateway ]
        │
        ▼
[ Spring Boot (AWS EC2) ] ──────────────► [ Azure PostgreSQL ]
        │  publica evento PedidoCreado
        ▼
[ RabbitMQ: pedidos.exchange ]
        │  routing key: pedidos.routingkey
        ▼
[ pedidos.queue ] ──► @RabbitListener (PedidoConsumer)
        │  si falla 3 veces
        ▼
[ pedidos.dlx ] ──► [ pedidos.dlq ] ──► @RabbitListener (registro de fallidos)
```

### Flujo de un pedido

1. El cliente envía `POST /api/pedidos`.
2. El pedido se guarda de forma **síncrona** en Azure PostgreSQL.
3. El `PedidoProducer` publica un evento **PedidoCreado** en RabbitMQ.
4. El `PedidoConsumer` procesa el evento de forma **asíncrona** (notificación / ticket).
5. Si el procesamiento termina bien, el mensaje se confirma con **ACK automático**.
6. Si falla, se reintenta **3 veces** y luego se envía a la **Dead Letter Queue** (`pedidos.dlq`).

> Si RabbitMQ no está disponible, el pedido igual se guarda: la mensajería no bloquea el flujo principal.

---

## 🐇 Topología RabbitMQ

Todos los nombres están centralizados en `application.yaml` (prefijo `app.rabbitmq`) y se declaran en `config/RabbitMQConfig.java`.

| Componente | Nombre | Descripción |
| :--- | :--- | :--- |
| Exchange principal | `pedidos.exchange` | Direct exchange |
| Cola principal | `pedidos.queue` | Durable, con DLX y DLK configurados |
| Routing key | `pedidos.routingkey` | Une exchange y cola principal |
| Dead Letter Exchange | `pedidos.dlx` | Recibe mensajes rechazados |
| Dead Letter Queue | `pedidos.dlq` | Almacena mensajes fallidos |
| Routing key DLQ | `pedidos.dlx.routingkey` | Une DLX y DLQ |

---

## 🌐 Endpoints

### Pedidos

| Método | Endpoint | Descripción | Seguridad |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/pedidos` | Lista los pedidos | Abierto |
| `POST` | `/api/pedidos` | Crea un pedido y publica el evento en RabbitMQ | Abierto |

### Administración de RabbitMQ

| Método | Endpoint | Descripción | Seguridad |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/queues` | Crea una cola (DTO validado con `@Valid`) | `ROLE_ADMIN` |
| `DELETE` | `/api/admin/queues/{name}` | Elimina una cola | `ROLE_ADMIN` |

Respuestas del endpoint de administración:

| Caso | Código |
| :--- | :--- |
| Sin token | `401 Unauthorized` |
| Token válido sin `ROLE_ADMIN` | `403 Forbidden` |
| Nombre de cola inválido | `400 Bad Request` |
| Cola ya existe | `409 Conflict` |
| Cola creada | `201 Created` |
| Cola no existe (al eliminar) | `404 Not Found` |
| Cola eliminada | `200 OK` |

---

## 🔐 Seguridad

- Validación de JWT emitidos por **Azure AD** (OAuth 2.0 Resource Server).
- Los roles se leen desde el claim `roles` del token (ej: `ROLE_ADMIN`).
- Los endpoints de administración de colas requieren `ROLE_ADMIN`.

---

## 📁 Estructura del proyecto

```
src/main/java/cl/duoc/pedidos360_backend/
├── config/
│   ├── RabbitMQConfig.java        # Colas, exchanges, bindings, DLQ, RabbitAdmin
│   └── SecurityConfig.java        # JWT Azure AD, roles, CORS
├── consumer/
│   └── PedidoConsumer.java        # @RabbitListener cola principal y DLQ
├── controller/
│   ├── PedidoController.java      # API de pedidos
│   └── RabbitAdminController.java # API de administración de colas
├── dto/
│   └── QueueRequestDTO.java       # DTO validado para crear colas
├── model/
│   └── Pedido.java                # Entidad JPA
├── producer/
│   └── PedidoProducer.java        # Publicación de eventos
├── repository/
│   └── PedidoRepository.java      # Spring Data JPA
└── service/
    └── RabbitAdminService.java    # Lógica de administración con RabbitAdmin
```

---

## 🛠️ Tecnologías

- Java 17
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, OAuth2 Resource Server, AMQP, Validation)
- PostgreSQL (Azure Database for PostgreSQL)
- RabbitMQ 4.x
- Docker
- Maven

---

## 🚀 Ejecución local

### 1. Levantar RabbitMQ con Docker

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
```

Panel de administración: http://localhost:15672 (usuario `guest`, contraseña `guest`).

### 2. Ejecutar el backend

```bash
./mvnw spring-boot:run
```

Al iniciar, la aplicación crea automáticamente `pedidos.queue` y `pedidos.dlq` en RabbitMQ.

---

## ✅ Pruebas

**Pedido válido (procesado y confirmado con ACK):**

```bash
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Prueba RabbitMQ","descripcion":"Pedido de prueba","total":25000}'
```

**Pedido inválido (reintentos y envío a la DLQ):**

```bash
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"cliente":"Prueba DLQ","descripcion":"Total invalido","total":0}'
```

**Administración sin token (debe responder 401):**

```bash
curl -i -X POST http://localhost:8080/api/admin/queues \
  -H "Content-Type: application/json" \
  -d '{"name":"cola.prueba"}'
```

---

## 👤 Autor

Manuel Álvarez Guevara ([@nano1977](https://github.com/nano1977))
