# Bookie Azure Functions

Este módulo contiene cuatro funciones Azure Serverless refactorizadas siguiendo el principio de Responsabilidad Única (SRP). Cada función es atómica y maneja una única responsabilidad.

## Arquitectura Refactorizada (SRP)

Las funciones están organizadas para máxima escalabilidad y mantenimiento:

- **CreateUserFunction**: Crea nuevos usuarios (POST /users)
- **GetUsersFunction**: Recupera lista de usuarios (GET /users)
- **CreateLoanFunction**: Crea nuevos préstamos (POST /loans)
- **GetLoansFunction**: Recupera lista de préstamos (GET /loans)

## Funciones Disponibles

### 1. GetUsersFunction (GET)
**Ruta:** `/api/users`  
**Método:** GET  
**Responsabilidad Única:** Listar todos los usuarios

```bash
curl -X GET http://localhost:7071/api/users
```

**Respuesta exitosa (200 OK):**
```json
{
  "status": "success",
  "data": [
    {
      "name": "Juan Pérez",
      "documentId": "12345678",
      "email": "juan@example.com"
    },
    {
      "name": "María García",
      "documentId": "87654321",
      "email": "maria@example.com"
    }
  ]
}
```

### 2. CreateUserFunction (POST)
**Ruta:** `/api/users`  
**Método:** POST  
**Responsabilidad Única:** Crear nuevo usuario

```bash
curl -X POST http://localhost:7071/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Pérez",
    "documentId": "12345678",
    "email": "juan@example.com"
  }'
```

**Respuesta exitosa (201 Created):**
```json
{
  "status": "success",
  "message": "User created successfully"
}
```

### 3. GetLoansFunction (GET)
**Ruta:** `/api/loans`  
**Método:** GET  
**Responsabilidad Única:** Listar todos los préstamos

```bash
curl -X GET http://localhost:7071/api/loans
```

**Respuesta exitosa (200 OK):**
```json
{
  "status": "success",
  "data": [
    {
      "userId": "12345678",
      "bookTitle": "Don Quijote"
    },
    {
      "userId": "87654321",
      "bookTitle": "Cien años de soledad"
    }
  ]
}
```

### 4. CreateLoanFunction (POST)
**Ruta:** `/api/loans`  
**Método:** POST  
**Responsabilidad Única:** Crear nuevo préstamo

```bash
curl -X POST http://localhost:7071/api/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "12345678",
    "bookTitle": "Don Quijote"
  }'
```

**Respuesta exitosa (201 Created):**
```json
{
  "status": "success",
  "message": "Loan created successfully"
}
```

## Configuración de Base de Datos

Las funciones requieren las siguientes variables de entorno (configuradas en `local.settings.json`):

- `DB_URL`: URL de conexión a Oracle (ej: `jdbc:oracle:thin:@localhost:1521/XEPDB1`)
- `DB_USER`: Usuario de base de datos
- `DB_PASSWORD`: Contraseña de base de datos

### Tablas de Base de Datos

**USERS:**
```sql
CREATE TABLE USERS (
  NAME VARCHAR2(100),
  DOCUMENT_ID VARCHAR2(20),
  EMAIL VARCHAR2(100)
);
```

**LOANS:**
```sql
CREATE TABLE LOANS (
  USER_ID VARCHAR2(20),
  BOOK_TITLE VARCHAR2(200)
);
```

## Estructura del Código

### DatabaseUtil (Clase Utilitaria)
Proporciona funcionalidad compartida para todas las funciones:

- `getConnection()`: Establece conexión JDBC usando variables de entorno
- `closeResources()`: Cierra ResultSet, Statement y Connection de forma segura
- `createErrorJson()`: Genera respuestas JSON de error
- `createSuccessJson()`: Genera respuestas JSON de éxito
- `createDataResponse()`: Genera respuestas JSON con datos

Esta clase centraliza la lógica de base de datos, evitando duplicación de código.

## Códigos de Estado HTTP

| Código | Significado | Escenario |
|--------|-------------|----------|
| 200 | OK | Operación GET exitosa |
| 201 | Created | Recurso POST creado exitosamente |
| 400 | Bad Request | JSON inválido, campos faltantes o solitud sin body |
| 405 | Method Not Allowed | Método HTTP no soportado en la ruta |
| 500 | Internal Server Error | Error de base de datos o error inesperado |

## Manejo de Errores

Todas las respuestas de error devuelven un JSON con el siguiente formato:

```json
{
  "status": "error",
  "message": "Descripción del error"
}
```

### Ejemplos de Errores

**Error: Campos faltantes en POST**
```json
{
  "status": "error",
  "message": "Missing required fields: name, documentId, email"
}
```

**Error: JSON inválido**
```json
{
  "status": "error",
  "message": "Invalid JSON format"
}
```

**Error: Conexión a base de datos**
```json
{
  "status": "error",
  "message": "Database error: ..."
}

## Compilación y Ejecución

### Compilar
```bash
mvn compile
```

### Empaquetar
```bash
mvn package
```

### Ejecutar localmente
```bash
func host start
```

## Ventajas de la Arquitectura SRP

✅ **Escalabilidad**: Cada función es independiente y puede escalarse por separado  
✅ **Mantenibilidad**: Cambios en una función no afectan a otras  
✅ **Testabilidad**: Cada función es fácil de probar de forma aislada  
✅ **Reutilización**: DatabaseUtil centraliza lógica común  
✅ **Despliegue**: Cada función puede desplegarse de forma independiente  
✅ **Monitoreo**: Métricas y logs específicos por función  

## Dependencias

- **Java 21**
- **Azure Functions Java Library 3.2.2**
- **Oracle JDBC 23.3.0.23.09**
- **Gson 2.10.1**

## Notas de Implementación

- Todas las respuestas incluyen el header `Content-Type: application/json`
- Las conexiones de base de datos se cierran correctamente tras cada operación
- Validación de campos requeridos en POST
- Los errores de base de datos son capturados y devueltos al cliente con detalles
- Logging detallado en cada función para debugging
