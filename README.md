# 📦 Pedidos360 – Backend (microservicios + RabbitMQ)

Backend del sistema de gestión de pedidos **Pedidos360**, desarrollado para la asignatura **Desarrollo Cloud Native I (DSY1107) – Duoc UC**.

Está compuesto por **tres microservicios Spring Boot** que se comunican de forma asíncrona mediante un **clúster RabbitMQ de dos nodos**, con autenticación en **Azure AD (Microsoft Entra ID)** y persistencia en **Azure Database for PostgreSQL**.

**Integrantes:** Manuel Álvarez Guevara · Diego Sepúlveda · Martín Escobedo

**Frontend:** [Pedidos360-Frontend](https://github.com/nano1977/Pedidos360-Frontend) (Angular + MSAL)

---

## 🏗️ Arquitectura

```
[ Angular + MSAL ] ──login OAuth 2.0 / PKCE──> [ Azure AD ] ──> JWT
        │  Authorization: Bearer <JWT>
        ▼
[ AWS API Gateway ]
        │
        ▼
[ pedidos-service :8080 ] ─────────────────────────> [ Azure PostgreSQL ]
        │  publica 3 eventos por pedido
        ▼
[ Clúster RabbitMQ (rabbitmq1 + rabbitmq2) ] <──── [ rabbitmq-admin-service :8082 ]
        │                                            (crea/elimina colas, exchanges y bindings)
        ▼
[ worker-service ]  (consumidores por dominio con ACK manual)
```

| Microservicio | Carpeta | Puerto | Responsabilidad |
| :--- | :--- | :--- | :--- |
| **pedidos-service** | `pedidos360-backend/` | 8080 | API REST de pedidos, persistencia en Azure PostgreSQL, **productor** de eventos y declaración de la topología RabbitMQ |
| **worker-service** | `worker-service/` | – | **Consumidores** por dominio (notificaciones, tickets, documentos) con ACK manual y DLQ |
| **rabbitmq-admin-service** | `rabbitmq-admin-service/` | 8082 | API REST para **crear y eliminar colas, exchanges y bindings**, protegida con `ROLE_ADMIN` |

Si RabbitMQ no está disponible, el pedido **se guarda igual**: la mensajería nunca bloquea el flujo principal. Si el worker está apagado, los mensajes **esperan en las colas** y se procesan cuando vuelve.

---

## 🐇 Topología RabbitMQ

```
pedidos.topic  (topic)  ── "pedido.*" ──────────> notificaciones.queue ──(falla)──> notificaciones.dlq
pedidos.direct (direct) ── "ticket.generar" ─────> tickets.queue        ──(falla)──> tickets.dlq
pedidos.direct (direct) ── "documento.generar" ──> documentos.queue     ──(falla)──> documentos.dlq
```

| Caso de uso | Exchange (tipo) | Routing key | Cola | DLQ |
| :--- | :--- | :--- | :--- | :--- |
| Notificaciones | `pedidos.topic` (**topic**) | `pedido.*` | `notificaciones.queue` | `notificaciones.dlq` |
| Tickets | `pedidos.direct` (**direct**) | `ticket.generar` | `tickets.queue` | `tickets.dlq` |
| Documentos | `pedidos.direct` (**direct**) | `documento.generar` | `documentos.queue` | `documentos.dlq` |

Cada cola principal declara `x-dead-letter-exchange = pedidos.dlx`, que enruta los mensajes rechazados a su propia DLQ.

- **Nombres centralizados:** todos los nombres de colas, exchanges y routing keys están en `application.yaml` (prefijo `app.rabbitmq`) y se inyectan con `@Value`.
- **Beans por caso de uso:** `pedidos360-backend/.../config/RabbitMQConfig.java` declara `Queue`, `Exchange` y `Binding` de cada flujo y de su DLQ.

---

## ✅ Confirmación de mensajes (ACK manual)

Los consumidores usan `acknowledge-mode: manual`. La política está en `worker-service/.../support/ProcesadorMensajes.java` y la usan todos los dominios:

| Situación | Acción | Resultado |
| :--- | :--- | :--- |
| Procesado correctamente | `basicAck` | El mensaje sale de la cola |
| Error **no recuperable** (datos inválidos) | `basicNack(requeue=false)` | **DLQ inmediata** |
| Error **transitorio**, primer intento | `basicNack(requeue=true)` | Se reintenta una vez |
| Error transitorio, ya reintentado | `basicNack(requeue=false)` | **DLQ** |

`DlqConsumer` registra en el log todo mensaje que llega a cualquiera de las tres DLQ.

---

## 🌐 Endpoints

### pedidos-service (`:8080`) — requiere JWT válido de Azure AD

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/pedidos` | Lista los pedidos |
| `POST` | `/api/pedidos` | Crea un pedido y publica 3 eventos (notificación, ticket y documento) |

### rabbitmq-admin-service (`:8082`) — requiere JWT con `ROLE_ADMIN`

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/admin/queues` | Crea una cola |
| `DELETE` | `/api/admin/queues/{name}` | Elimina una cola |
| `POST` | `/api/admin/exchanges` | Crea un exchange (`direct`, `topic` o `fanout`) |
| `DELETE` | `/api/admin/exchanges/{name}` | Elimina un exchange |
| `POST` | `/api/admin/bindings` | Une una cola a un exchange con una routing key |
| `DELETE` | `/api/admin/bindings` | Elimina ese binding |

Respuestas: `201` creado · `200` eliminado · `400` datos inválidos (con mensaje por campo) · `401` sin token · `403` sin `ROLE_ADMIN` · `404` no existe · `409` ya existe · `503` RabbitMQ no disponible.

Ejemplo de error de validación:

```json
{
  "error": "Datos inválidos",
  "campos": { "name": "Solo se permiten letras, números, punto, guion y guion bajo" }
}
```

---

## 🔐 Seguridad

- Los servicios validan la **firma del JWT** con las llaves públicas (JWKS) del tenant de Azure AD y su **expiración**.
- `ROLE_ADMIN` se obtiene del claim `roles` del token (App Role de Azure AD). Como respaldo, `app.security.admin-users` permite indicar cuentas administradoras cuando el tenant no permite asignar App Roles (por ejemplo, a usuarios invitados).
- Sin token → **401**. Token válido sin el rol → **403**.

---

## 📁 Estructura del repositorio

```
Pedidos360-Backend/
├── docker-compose.yml              # Clúster RabbitMQ de 2 nodos
├── rabbitmq/rabbitmq.conf          # Usuario admin y formación del clúster
├── pedidos360-backend/             # pedidos-service
│   └── src/main/java/cl/duoc/pedidos360_backend/
│       ├── config/                 # RabbitMQConfig (topología), SecurityConfig (JWT)
│       ├── controller/             # PedidoController
│       ├── model/                  # Pedido (entidad JPA)
│       ├── producer/               # PedidoProducer
│       └── repository/             # PedidoRepository
├── worker-service/
│   └── src/main/java/cl/duoc/worker/
│       ├── notificacion/           # NotificacionConsumer
│       ├── ticket/                 # TicketConsumer
│       ├── documento/              # DocumentoConsumer
│       ├── dlq/                    # DlqConsumer
│       ├── support/                # ProcesadorMensajes (ACK/NACK), MensajeNoRecuperableException
│       └── config/
└── rabbitmq-admin-service/
    └── src/main/java/cl/duoc/admin/
        ├── controller/             # RabbitAdminController
        ├── service/                # RabbitAdminService (usa RabbitAdmin)
        ├── dto/                    # QueueRequest, ExchangeRequest, BindingRequest (@Valid)
        ├── exception/              # GlobalExceptionHandler y excepciones
        └── config/                 # SecurityConfig, RabbitConfig
```

---

## 🚀 Ejecución local

**Requisitos:** Java 17+, Docker Desktop.

### 1. Levantar el clúster RabbitMQ

```bash
docker compose up -d
docker exec -u rabbitmq rabbitmq1 rabbitmqctl cluster_status   # Running Nodes: rabbitmq1 y rabbitmq2
```

- Nodo 1: AMQP `localhost:5672`, panel **http://localhost:15672** (usuario `admin`, contraseña `admin123`; muestra ambos nodos).
- Nodo 2: AMQP `localhost:5682`.
- Los microservicios se conectan a **ambos nodos** (`spring.rabbitmq.addresses`): si uno cae, usan el otro.

### 2. Arrancar los microservicios (una terminal por servicio)

```bash
cd pedidos360-backend      && ./mvnw spring-boot:run
cd worker-service          && ./mvnw spring-boot:run
cd rabbitmq-admin-service  && ./mvnw spring-boot:run
```

Al arrancar, pedidos-service crea automáticamente los exchanges, las 6 colas y los bindings.

---

## 🧪 Pruebas

Obtener un token iniciando sesión en el frontend y guardarlo en una variable (`TOKEN`).

**Pedido válido** → los tres consumidores procesan y confirman con ACK:

```bash
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cliente":"Cliente demo","descripcion":"Pedido de prueba","total":25000}'
```

**Error no recuperable** (`total` = 0) → `notificaciones.dlq` de inmediato:

```bash
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cliente":"Prueba DLQ","descripcion":"Total invalido","total":0}'
```

**Error transitorio** (cliente `FALLA-TICKET`) → 1 reintento y luego `tickets.dlq`:

```bash
curl -i -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"cliente":"FALLA-TICKET","descripcion":"Reintento","total":5000}'
```

**Administración:**

```bash
# Sin token -> 401
curl -i -X POST http://localhost:8082/api/admin/exchanges -H "Content-Type: application/json" \
  -d '{"name":"demo.exchange","type":"topic"}'

# Con token de administrador -> 201
curl -i -X POST http://localhost:8082/api/admin/exchanges -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"demo.exchange","type":"topic"}'
curl -i -X POST http://localhost:8082/api/admin/queues -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"demo.queue"}'
curl -i -X POST http://localhost:8082/api/admin/bindings -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"queue":"demo.queue","exchange":"demo.exchange","routingKey":"demo.#"}'

# Nombre inválido -> 400 con mensaje claro
curl -i -X POST http://localhost:8082/api/admin/queues -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"a b"}'
```

> En Windows (CMD) se usa `%TOKEN%` en lugar de `$TOKEN` y comillas dobles escapadas en el JSON.

---

## 📋 Relación con la pauta de evaluación

| Indicador | Dónde se cumple |
| :--- | :--- |
| Nombres centralizados en `application.yml` / `@Configuration` | `application.yaml` (`app.rabbitmq`) de cada servicio |
| Beans de Queue, Exchange y Binding por caso de uso | `pedidos360-backend/.../config/RabbitMQConfig.java` |
| Negocio separado de la configuración de mensajería | El controlador solo usa `PedidoProducer`; la topología vive en `RabbitMQConfig` |
| Consumidores con `@RabbitListener` agrupados por dominio | `worker-service`: paquetes `notificacion`, `ticket`, `documento`, `dlq` |
| ACK y manejo explícito de errores | `ProcesadorMensajes` (basicAck / basicNack / reintento / DLQ) |
| Microservicio administrador con endpoints REST | `rabbitmq-admin-service/.../RabbitAdminController.java` |
| Lógica encapsulada en un servicio | `RabbitAdminService` (usa `RabbitAdmin`) |
| Validación de parámetros de entrada | DTOs con `@NotBlank`, `@Size`, `@Pattern` + `GlobalExceptionHandler` |

---

## 🛠️ Tecnologías

Java 17 · Spring Boot 4.1 (Web MVC, Data JPA, Security, OAuth2 Resource Server, AMQP, Validation) · RabbitMQ 4 · Docker Compose · Azure Database for PostgreSQL · Microsoft Entra ID (Azure AD) · AWS API Gateway · AWS EC2 · Maven
