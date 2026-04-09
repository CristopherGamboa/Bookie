# GraphQL Loans API Documentation

## Overview

Se han añadido dos nuevas Azure Functions que utilizan GraphQL (versión 17.3 de graphql-java) para manejar operaciones sobre préstamos (loans):

1. **GetLoansGraphQLFunction** - Maneja queries de GraphQL
2. **CreateLoanGraphQLFunction** - Maneja mutations de GraphQL

## GetLoansGraphQLFunction

### Endpoint
```
POST /api/graphql/loans/query
```

### Descripción
Ejecuta consultas GraphQL para obtener la lista de préstamos de la base de datos Oracle.

### Esquema GraphQL

```graphql
type Query {
  loans: [Loan!]!
}

type Loan {
  userId: String!
  bookTitle: String!
}
```

### Ejemplo de Request

```json
{
  "query": "{ loans { userId bookTitle } }"
}
```

### Ejemplo de Response (Exitosa)

```json
{
  "data": {
    "loans": [
      {
        "userId": "user123",
        "bookTitle": "Clean Code"
      },
      {
        "userId": "user456",
        "bookTitle": "Design Patterns"
      }
    ]
  }
}
```

### Ejemplo de Response (Error)

```json
{
  "errors": [
    "Database error: Connection failed"
  ]
}
```

### Variables de Entorno Requeridas

- `DB_URL`: URL de conexión a la base de datos Oracle
- `DB_USER`: Usuario de la base de datos
- `DB_PASSWORD`: Contraseña de la base de datos

---

## CreateLoanGraphQLFunction

### Endpoint
```
POST /api/graphql/loans/mutation
```

### Descripción
Ejecuta mutations de GraphQL para crear nuevos préstamos en la base de datos Oracle.

### Esquema GraphQL

```graphql
type Mutation {
  createLoan(userId: String!, bookTitle: String!): CreateLoanPayload!
}

type CreateLoanPayload {
  loan: Loan
  success: String!
}

type Loan {
  userId: String!
  bookTitle: String!
}
```

### Ejemplo de Request

```json
{
  "query": "mutation { createLoan(userId: \"user123\", bookTitle: \"Clean Code\") { loan { userId bookTitle } success } }"
}
```

### Ejemplo de Response (Exitosa)

```json
{
  "data": {
    "createLoan": {
      "loan": {
        "userId": "user123",
        "bookTitle": "Clean Code"
      },
      "success": "Loan created successfully"
    }
  }
}
```

### Ejemplo de Response (Error)

```json
{
  "errors": [
    "Database error: Insert failed"
  ]
}
```

### Variables de Entorno Requeridas

- `DB_URL`: URL de conexión a la base de datos Oracle
- `DB_USER`: Usuario de la base de datos
- `DB_PASSWORD`: Contraseña de la base de datos

---

## Estructura de Base de Datos

Ambas funciones operan sobre la tabla `LOANS` con la siguiente estructura:

```sql
CREATE TABLE LOANS (
  USER_ID VARCHAR2(100) NOT NULL,
  BOOK_TITLE VARCHAR2(255) NOT NULL
);
```

---

## Detalles de Implementación

### GetLoansGraphQLFunction

- **Ubicación del archivo**: `faas/src/main/java/duoc/GetLoansGraphQLFunction.java`
- **Responsabilidad**: Definir un GraphQLSchema con un queryType que permite consultar la lista de préstamos
- **Método de acceso a BD**: JDBC con SELECT * FROM LOANS
- **Tipo de respuesta**: JSON estándar de GraphQL con propiedad `data` u `errors`

### CreateLoanGraphQLFunction

- **Ubicación del archivo**: `faas/src/main/java/duoc/CreateLoanGraphQLFunction.java`
- **Responsabilidad**: Definir un GraphQLSchema con un mutationType que recibe userId y bookTitle
- **Método de acceso a BD**: JDBC con INSERT INTO LOANS (USER_ID, BOOK_TITLE) VALUES (?, ?)
- **Tipo de respuesta**: JSON estándar de GraphQL con propiedad `data` u `errors`

---

## Características

✅ **Independencia**: Ambas funciones son completamente independientes  
✅ **HTTP POST**: Ambas responden únicamente a peticiones HTTP POST  
✅ **Respuestas estándar GraphQL**: Retornan JSON con estructura `{ data: {...} }` o `{ errors: [...] }`  
✅ **Manejo de errores**: Capturan y reportan errores de base de datos y validación  
✅ **Logging**: Registran operaciones en el contexto de ejecución de Azure Functions  
✅ **Seguridad**: Utilizan PreparedStatements para evitar SQL injection  

---

## Notas Técnicas

- **Librería GraphQL**: graphql-java versión 17.3
- **Controlador JDBC**: Oracle JDBC 11 (ojdbc11)
- **JSON**: Google Gson para serialización/deserialización
- **Ejecución**: Se ejecutan en Azure Functions con Java 21

