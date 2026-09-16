# 📦 Pedidos360 - Backend (Spring Boot)

API REST para el sistema de gestión "Pedidos360". Este backend está construido con Spring Boot 3, utiliza Azure PostgreSQL como base de datos y está protegido mediante Microsoft Entra ID (Azure AD) con el Tenant de Duoc UC.

## 🛠️ Tecnologías utilizadas
* **Framework:** Spring Boot 3 / Java
* **Base de Datos:** Azure Database for PostgreSQL
* **ORM:** Spring Data JPA / Hibernate
* **Seguridad:** Spring Security + OAuth2 Resource Server (JWT)
* **Gestor de dependencias:** Maven

## 🚀 Requisitos previos
* Java 17 o superior.
* Maven instalado (o usar el wrapper `mvnw` incluido).
* Acceso a la base de datos `pedidos360-db-nano` en Azure.

## ⚙️ Configuración del entorno (`application.yaml`)
Para ejecutar este proyecto, asegúrate de tener configurado tu archivo `src/main/resources/application.yaml` con las credenciales correctas de Azure PostgreSQL y el Tenant ID de Azure AD:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://[pedidos360-db-nano.postgres.database.azure.com:5432/postgres?sslmode=require](https://pedidos360-db-nano.postgres.database.azure.com:5432/postgres?sslmode=require)
    username: adminpedidos
    password: [TU_PASSWORD]
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: [https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250/v2.0](https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250/v2.0)