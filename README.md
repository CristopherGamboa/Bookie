# 📚 Bookie - Sistema de Biblioteca Cloud Native (EDA)

Este repositorio contiene el código fuente de la fase final del sistema de gestión de bibliotecas "Bookie", implementando una **Arquitectura Orientada a Eventos (EDA)** en un entorno híbrido y multinube (AWS + Azure).

## 🏗️ Arquitectura del Sistema

El sistema está diseñado bajo el principio de desacoplamiento y alta disponibilidad, separando las responsabilidades de orquestación, persistencia y lógica de negocio.

### 1. Capa de Orquestación y Datos (AWS)
* **BFF (Backend for Frontend):** Microservicio desarrollado en **Java Spring Boot**. Actúa como *API Gateway* y traductor de protocolos, recibiendo peticiones del cliente y derivándolas a la capa Serverless.
* **Base de Datos:** Motor **Oracle XE** dockerizado.
* *Infraestructura:* Ambos contenedores (BFF y BD) operan sobre una instancia **EC2 (Amazon Linux 2023)** en AWS mediante Docker Compose, exponiendo los puertos 8080 (HTTP) y 1521 (JDBC).

### 2. Capa de Lógica de Negocio (Azure Serverless)
Las operaciones CRUD atómicas están alojadas en **Azure Functions App** (Java 21, región `Brazil South`):
* **REST API:** Funciones encargadas de la gestión síncrona de Usuarios (`CreateUser`, `GetUsers`).
* **GraphQL API:** Funciones encargadas de la gestión de Préstamos (`GetLoans`, `CreateLoan`), centralizando las mutaciones y consultas en un único estándar.

### 3. Capa de Eventos Asíncronos (Azure Event Grid)
Para garantizar la escalabilidad y el desacoplamiento, implementamos el patrón Productor-Consumidor:
* **Productor:** Al realizar una mutación GraphQL exitosa, la función `CreateLoanGraphQLFunction` publica un evento `Biblioteca.Prestamo.Creado` bajo el patrón *Fire and Forget*.
* **Bus de Mensajería:** Un Tópico Personalizado (*Custom Topic*) en **Azure Event Grid** recibe y enruta la carga útil (payload).
* **Consumidor:** La función `LoanEventConsumerFunction` (activada por `@EventGridTrigger`) se despierta asíncronamente para capturar, procesar y registrar el evento, demostrando una trazabilidad impecable sin afectar la latencia del cliente final.

## 🚀 Despliegue y Ejecución

### Requisitos Previos
* Instancia EC2 con Docker y Docker Compose instalados.
* Cuenta de Microsoft Azure con suscripción activa para Function App y Event Grid.
* Maven y Java 21 instalados localmente.

### Paso 1: Levantar Infraestructura AWS
```bash
# Clonar el repositorio
git clone [https://github.com/CristopherGamboa/Bookie.git](https://github.com/CristopherGamboa/Bookie.git)
cd Bookie

# Construir y levantar contenedores en EC2
docker compose up -d --build

### Paso 2: Despliegue Serverless en Azure

Las funciones deben ser inyectadas en la nube mediante el plugin de Maven, asegurando que las variables de entorno (DB_URL, EVENT_GRID_ENDPOINT, EVENT_GRID_KEY) estén configuradas en el portal de Azure.

```bash
cd faas
mvn clean package azure-functions:deploy

👥 Equipo de Desarrollo
Cristopher Gamboa

Rodrigo Diaz

Sebastián Briceño