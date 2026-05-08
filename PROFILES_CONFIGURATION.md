# Configuración de Perfiles de Spring Boot - Bookie BFF

El BFF utiliza **Spring Boot Profiles** para manejar diferentes ambientes de ejecución. Esto permite que la misma aplicación funcione en desarrollo local, Docker y producción sin cambiar código.

## 📋 Perfiles Disponibles

### 1. **Perfil por Defecto (Default - Producción)**
**Archivo**: `application.properties`

- **Azure Functions**: URLs de producción en Azure (`https://bookie-functions.azurewebsites.net/api/...`)
- **Base de Datos**: Conexión a Oracle local o en la nube
- **Caso de Uso**: Producción en EC2

```bash
java -jar app.jar
```

### 2. **Perfil LOCAL (application-local.properties)**
- **Azure Functions**: Emulator en `http://localhost:7071/api/...`
- **Base de Datos**: Oracle en `localhost:1521`
- **Caso de Uso**: Desarrollo local con emulator de Azure Functions

```bash
java -jar app.jar --spring.profiles.active=local
```

**Requisitos**:
- Azure Functions Core Tools instalado
- Azure Functions emulator corriendo en puerto 7071
- Oracle XE ejecutándose en localhost:1521

### 3. **Perfil DOCKER (application-docker.properties)**
- **Azure Functions**: URLs de producción en Azure (`https://bookie-functions.azurewebsites.net/api/...`)
- **Base de Datos**: Oracle en contenedor Docker (`oracle-db:1521`)
- **Caso de Uso**: Docker con docker-compose (EC2)

```bash
docker-compose up
```

**Automático**: El `docker-compose.yaml` ya activa este perfil con:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

## 🔧 Cómo Testear Localmente

Si estás viendo errores de `Connection refused: http://localhost:7071/...`, aquí hay varias opciones:

### Opción A: Usar Azure Functions Emulator (Recomendado para desarrollo local)
```bash
# 1. Asegúrate de tener Azure Functions Core Tools instalado
# 2. En la carpeta faas/, ejecuta:
cd faas
func start --port 7071

# 3. En otra terminal, ejecuta el BFF con perfil local:
cd bff
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### Opción B: Usar Azure Functions en Azure (Sin emulator)
```bash
# 1. Asegúrate de que las Azure Functions están deployadas en Azure
# 2. El BFF usará por defecto las URLs de Azure
# 3. Ejecuta el BFF normalmente:
cd bff
mvn spring-boot:run
```

Este es tu caso actual - Las Azure Functions ya están en Azure, así que el BFF debería conectar directamente.

### Opción C: Usar Docker Compose (Para simular producción)
```bash
# 1. Desde la raíz del proyecto:
docker-compose up

# 2. El BFF estará disponible en http://localhost:8080
# 3. La BD Oracle estará disponible en localhost:1521
```

## 📝 Configuración por Variable de Entorno

Todas las URLs pueden ser override usando variables de entorno:

```bash
# Ejemplo para usar URLs locales
java -jar app.jar \
  --spring.profiles.active=docker \
  --FAAS_USER_URL=http://localhost:7071/api/users \
  --FAAS_GRAPHQL_QUERY_URL=http://localhost:7071/api/graphql/query \
  --FAAS_GRAPHQL_MUTATION_URL=http://localhost:7071/api/graphql/mutation
```

O en Docker:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - FAAS_USER_URL=http://localhost:7071/api/users
  - FAAS_GRAPHQL_QUERY_URL=http://localhost:7071/api/graphql/query
  - FAAS_GRAPHQL_MUTATION_URL=http://localhost:7071/api/graphql/mutation
```

## 🔍 Verificar Qué Perfil Se Está Usando

Mira los logs de arranque del BFF:

```
The following profiles are active: docker
```

O si no aparece ningún perfil, está usando la configuración por defecto (producción).

## 📚 Resumen de Archivos

| Archivo | Propósito | Cuándo usar |
|---------|----------|------------|
| `application.properties` | Default (Producción) | Deployado en EC2 |
| `application-local.properties` | Desarrollo con emulator | Desarrollo local |
| `application-docker.properties` | Docker/Docker Compose | docker-compose up |

## 🚀 Para tu caso actual

Visto que las Azure Functions ya están desplegadas en Azure, deberías:

1. **Testear localmente SIN Docker** (tu caso actual):
   ```bash
   cd bff
   mvn spring-boot:run
   ```
   El BFF usará las URLs por defecto de Azure (azurewebsites.net)

2. **Testear en Docker** (simulando EC2):
   ```bash
   docker-compose up
   ```
   Mismo resultado: usa URLs de Azure

Si necesitas testear con emulator local, usa el perfil `local` como se explicó arriba.
