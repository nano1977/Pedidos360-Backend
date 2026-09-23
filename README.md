Markdown
# 📦 Pedidos360 - Backend API & Arquitectura Multi-Cloud

Sistema de gestión de pedidos integrado con arquitectura cloud distribuida en **AWS** y **Azure**, con frontend en **Angular** y backend en **Spring Boot**.

---

## 🏗️ Arquitectura del Sistema

La solución implementa un flujo *End-to-End* completamente funcional e integrado:

[ Angular Frontend ]
│
▼
[ AWS API Gateway ] (Endpoint HTTPS público)
│
▼
[ AWS EC2 Instance ] (Spring Boot - Puerto 8080)
│
▼
[ Azure PostgreSQL ] (Base de datos relacional)


### Componentes de Infraestructura:
* **Frontend:** Angular con integración `HttpClient` apuntando a API Gateway.
* **API Gateway (AWS):** Proxy de entrada (`esqclflrwc`) en `us-east-1` encargado de enrutar peticiones hacia la instancia EC2.
* **Servidor de Aplicación (AWS EC2):** Instancia EC2 Linux ejecutando la aplicación Spring Boot.
* **Persistencia (Azure Database for PostgreSQL):** Instancia gestionada en Azure para la persistencia de datos.

---

## 🌐 Endpoints de la API

* **URL Base de API Gateway:** `https://esqclflrwc.execute-api.us-east-1.amazonaws.com`

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/pedidos` | Obtener el listado de pedidos almacenados |
| `POST` | `/api/pedidos` | Registrar un nuevo pedido en el sistema |

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java 17
* **Framework:** Spring Boot 3.x
* **Persistencia:** Spring Data JPA / Hibernate
* **Base de Datos:** Azure PostgreSQL Database
* **Infraestructura Cloud:** AWS EC2, AWS API Gateway, Azure Database
* **Construcción y Dependencias:** Apache Maven

---

## ⚙️ Configuración del Entorno (`application.yaml`)

Para ejecutar la aplicación localmente, asegúrate de configurar las credenciales en `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<TU_AZURE_POSTGRES_HOST>:5432/<NOMBRE_BD>?sslmode=require
    username: <TU_USUARIO>
    password: <TU_CONTRASEÑA>
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8080
🚀 Ejecución en Entorno Local
Clonar el repositorio:

Bash
git clone [https://github.com/nano1977/Pedidos360-Backend.git](https://github.com/nano1977/Pedidos360-Backend.git)
cd Pedidos360-Backend/pedidos360-backend
Compilar el proyecto con Maven:

Bash
./mvnw clean install
Ejecutar la aplicación:

Bash
./mvnw spring-boot:run
✅ Verificación y Pruebas de Conectividad
Estado de Integración: Verificado con respuesta STATUS: 200 OK a través de pruebas de conectividad de la API.

JavaScript
// Prueba de conectividad vía Fetch API desde cliente
fetch('[https://esqclflrwc.execute-api.us-east-1.amazonaws.com/api/pedidos](https://esqclflrwc.execute-api.us-east-1.amazonaws.com/api/pedidos)')
  .then(res => console.log('STATUS:', res.status)) // STATUS: 200
  .then(data => console.log(data));
👤 Autor
Manuel Álvarez Guevara (@nano1977)


### Instrucciones rápidas:
1. En GitHub, presiona `Ctrl + A` dentro del cuadro de texto y luego la tecla `Borrar` para dejar el editor en blanco.
2. Pega este texto copiado con `Ctrl + V`.
3. Haz clic en la pestaña **Preview** arriba para revisar que se vea ordenado.
4. Presiona **Commit changes...** para guardar.
