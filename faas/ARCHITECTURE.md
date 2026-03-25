# Arquitectura Refactorizada - Responsabilidad Única (SRP)

## Diagrama de la Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Requests (Cliente)                  │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
    ┌─────▼─────┐     ┌─────▼─────┐     ┌─────▼─────┐
    │   GET      │     │   POST    │     │   GET     │
    │  /users    │     │  /users   │     │  /loans   │
    └─────┬─────┘     └─────┬─────┘     └─────┬─────┘
          │                   │                   │
    ┌─────▼──────────────┐   │            ┌─────▼──────────────┐
    │ GetUsersFunction   │   │            │ GetLoansFunction   │
    │ - SELECT * USERS   │   │            │ - SELECT * LOANS   │
    │ - JSON Response    │   │            │ - JSON Response    │
    └─────┬──────────────┘   │            └─────┬──────────────┘
          │                   │                   │
          │            ┌──────▼──────────────┐   │
          │            │CreateUserFunction   │   │
          │            │ - INSERT USERS      │   │
          │            │ - JSON Response     │   │
          │            └──────┬──────────────┘   │
          │                   │                   │
          │       ┌───────────┴──────────────────┴────┐
          │       │                                   │
          │  ┌────▼─────────────────────────────┐   │
          │  │    DatabaseUtil (Utilidad)       │   │
          │  ├──────────────────────────────────┤   │
          │  │ + getConnection()                │   │
          │  │ + closeResources()               │   │
          │  │ + createErrorJson()              │   │
          │  │ + createSuccessJson()            │   │
          │  │ + createDataResponse()           │   │
          │  └────┬──────────────────────────────┘   │
          │       │                                   │
          │       └──────────────┬────────────────────┘
          │                      │
          └──────────────────────┼────────────────────┐
                                 │                    │
                    ┌────────────▼─────────────┐     │
                    │   Oracle Database        │     │
                    ├───────────────────────────┤     │
                    │ USERS Table               │     │
                    │ - NAME                    │     │
                    │ - DOCUMENT_ID             │     │
                    │ - EMAIL                   │     │
                    │                           │     │
                    │ LOANS Table               │     │
                    │ - USER_ID                 │     │
                    │ - BOOK_TITLE              │     │
                    └─────────────────────────┘      │
                                                     │
                                    ┌────────────────▼─────────┐
                                    │  POST /loans              │
                                    │  CreateLoanFunction       │
                                    │  - INSERT LOANS           │
                                    │  - JSON Response          │
                                    └───────────────────────────┘
```

## Flujo de Ejecución

### GET /users
```
Cliente → GetUsersFunction → DatabaseUtil.getConnection()
         → SELECT USERS → DatabaseUtil.createDataResponse()
         → HTTP 200 + JSON
```

### POST /users
```
Cliente → CreateUserFunction → Validar JSON
         → DatabaseUtil.getConnection()
         → INSERT USERS → HTTP 201
```

### GET /loans
```
Cliente → GetLoansFunction → DatabaseUtil.getConnection()
         → SELECT LOANS → DatabaseUtil.createDataResponse()
         → HTTP 200 + JSON
```

### POST /loans
```
Cliente → CreateLoanFunction → Validar JSON
         → DatabaseUtil.getConnection()
         → INSERT LOANS → HTTP 201
```

## Clases y sus Responsabilidades

| Clase | Responsabilidad |
|-------|-----------------|
| **GetUsersFunction** | Listar usuarios (SELECT) |
| **CreateUserFunction** | Crear usuario (INSERT) |
| **GetLoansFunction** | Listar préstamos (SELECT) |
| **CreateLoanFunction** | Crear préstamo (INSERT) |
| **DatabaseUtil** | Gestionar conexiones y utilitarios |
| **Function** | (Legado - Ejemplo original) |
| **UserFunction** | (Legado - Ambas operaciones) |
| **LoanFunction** | (Legado - Ambas operaciones) |

## Ventajas de esta Arquitectura

### 1. Escalabilidad
- Cada función puede tener su propio plan de escalado
- Los recursos se distribuyen de forma más eficiente

### 2. Mantenibilidad
- Código más limpio y fácil de entender
- Una función = una responsabilidad
- Cambios aislados que no afectan otras funciones

### 3. Testabilidad
- Cada función es fácil de testear de forma aislada
- Mock de DatabaseUtil es simple

### 4. Rendimiento
- Funciones más pequeñas con cold start más rápido
- Menor consumo de memoria por función

### 5. Seguridad
- Control de acceso granular por función
- Auditoría específica de operaciones

## Ejemplo de Test (Pseudocódigo)

```java
@Test
public void testGetUsersFunction() {
    // Mock DatabaseUtil.getConnection()
    GetUsersFunction function = new GetUsersFunction();
    HttpResponseMessage response = function.run(mockRequest, mockContext);
    
    assertEquals(200, response.getStatus().value());
    assertTrue(response.getBody().toString().contains("success"));
}

@Test
public void testCreateUserFunction() {
    // Mock DatabaseUtil.getConnection()
    CreateUserFunction function = new CreateUserFunction();
    HttpResponseMessage response = function.run(mockRequest, mockContext);
    
    assertEquals(201, response.getStatus().value());
    assertTrue(response.getBody().toString().contains("success"));
}
```
