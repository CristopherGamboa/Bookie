# Bookie Azure Functions

Este módulo contiene dos funciones Azure Serverless para gestionar usuarios y préstamos de una biblioteca.

## Funciones Disponibles

### 1. UserFunction
**Ruta:** `/api/users`

#### GET - Listar Usuarios
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
    }
  ]
}
```

#### POST - Crear Usuario
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

### 2. LoanFunction
**Ruta:** `/api/loans`

#### GET - Listar Préstamos
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
    }
  ]
}
```

#### POST - Crear Préstamo
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

## Códigos de Estado HTTP

| Código | Significado |
|--------|-------------|
| 200 | OK - Operación exitosa (GET) |
| 201 | Created - Recurso creado exitosamente (POST) |
| 400 | Bad Request - Solicitud inválida |
| 405 | Method Not Allowed - Método HTTP no soportado |
| 500 | Internal Server Error - Error en el servidor |

## Manejo de Errores

Todas las respuestas de error devuelven un JSON con el siguiente formato:

```json
{
  "status": "error",
  "message": "Descripción del error"
}
```

## Compilación y Ejecución

### Compilar
```bash
mvn clean compile
```

### Empaquetar
```bash
mvn clean package
```

### Ejecutar localmente
```bash
func host start
```

## Dependencias

- **Java 21**
- **Azure Functions Java Library 3.2.2**
- **Oracle JDBC 23.3.0.23.09**
- **Gson 2.10.1**

## Notas de Implementación

- Todas las respuestas incluyen el header `Content-Type: application/json`
- Las conexiones de base de datos se cierran correctamente tras cada operación
- Se maneja validación de campos requeridos en POST
- Los errores de base de datos son capturados y devueltos al cliente con detalles
