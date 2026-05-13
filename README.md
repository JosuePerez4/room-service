# room-service

Microservicio Spring Boot encargado de administrar salas asociadas a
conferencias. El servicio expone una API REST bajo `/rooms`, persiste las salas
en PostgreSQL y, antes de crear una sala, valida de forma sincronica que la
conferencia exista en `conference-service`.

## Arquitectura y flujo principal

- **API publica:** `RoomController` expone endpoints REST para crear, listar,
  consultar y eliminar salas.
- **Reglas de negocio:** `RoomService` normaliza espacios en blanco, evita
  nombres duplicados por conferencia y delega la validacion de conferencias.
- **Integracion HTTP:** `ConferenceClient` llama a
  `GET {CONFERENCE_SERVICE_URL}/conferences/get/{conferenceId}` antes de
  persistir una sala. Si llega un header `Authorization` al `POST`, se reenvia a
  `conference-service`.
- **Persistencia:** `RoomRepository` usa Spring Data JPA sobre la tabla
  `rooms`.

Flujo de creacion:

1. `POST /rooms/conference/{conferenceId}` recibe los datos de la sala.
2. El servicio consulta `conference-service` para validar la conferencia.
3. Rechaza nombres duplicados dentro de la misma conferencia sin distinguir
   mayusculas/minusculas.
4. Guarda la sala y devuelve el recurso creado.

### Dependencia con `conference-service`

La unica dependencia sincronica del flujo de escritura es
`ConferenceClient.ensureConferenceExists`:

```http
GET {CONFERENCE_SERVICE_URL}/conferences/get/{conferenceId}
Authorization: <mismo header recibido en POST /rooms/conference/{conferenceId}>
```

- Un `404` de `conference-service` se transforma en `404 Not Found` local con el
  mensaje `La conferencia no existe`.
- Otros errores HTTP de `conference-service` no se traducen en el
  `GlobalExceptionHandler`; se dejan al manejo por defecto de Spring.
- Fallas de red, URL mal configurada o errores construyendo la llamada se
  exponen como `400 Bad Request` con el prefijo
  `No se pudo validar la conferencia:`.
- Los endpoints de lectura (`GET /rooms/...`) y borrado no llaman a
  `conference-service`.

## Requisitos

- JDK 25, alineado con `pom.xml`.
- PostgreSQL accesible desde la aplicacion.
- Maven Wrapper incluido en el repo (`./mvnw`).

## Configuracion

`application.properties` importa opcionalmente un archivo `.env` en la raiz del
repo o en `paper-microservice-project-backend/.env`. Tambien se pueden exportar
las variables en el entorno.

| Variable | Obligatoria | Descripcion |
| --- | --- | --- |
| `ROOM_DB_URL` | Si | URL JDBC de PostgreSQL usada por `spring.datasource.url`. |
| `ROOM_SERVICE_PORT` | Si | Puerto HTTP donde se levanta el servicio. |
| `CONFERENCE_SERVICE_URL` | No | Base URL de `conference-service`; por defecto `http://localhost:8082`. |

Ejemplo local:

```properties
ROOM_DB_URL=jdbc:postgresql://localhost:5432/rooms
ROOM_SERVICE_PORT=8083
CONFERENCE_SERVICE_URL=http://localhost:8082
```

## Ejecucion y verificacion

Desde la raiz del repositorio:

```bash
./mvnw spring-boot:run
```

Comandos utiles:

```bash
./mvnw test
./mvnw package
```

La suite actual solo contiene una prueba de arranque de contexto
(`RoomApplicationTests.contextLoads`), por lo que no reemplaza pruebas manuales
del contrato REST ni de la integracion con PostgreSQL y `conference-service`.

### Verificacion manual rapida

Con PostgreSQL y `conference-service` levantados, crear una sala valida:

```bash
curl -i -X POST "http://localhost:${ROOM_SERVICE_PORT}/rooms/conference/<conference-uuid>" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token-opcional>" \
  -d '{
    "name": "Sala Principal",
    "capacity": 120,
    "type": "presencial",
    "locationOrLink": "Edificio A - Auditorio",
    "topicHints": "keynotes, apertura"
  }'
```

Comprobar las reglas principales:

```bash
# Lista las salas persistidas para la conferencia, ordenadas por name ascendente.
curl -i "http://localhost:${ROOM_SERVICE_PORT}/rooms/conference/<conference-uuid>"

# Debe responder 409 si se repite el name en la misma conferencia sin importar mayusculas.
curl -i -X POST "http://localhost:${ROOM_SERVICE_PORT}/rooms/conference/<conference-uuid>" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token-opcional>" \
  -d '{"name":"sala principal","capacity":50,"type":"presencial","locationOrLink":"Auditorio"}'
```

## API REST

### Crear sala

```http
POST /rooms/conference/{conferenceId}
Authorization: Bearer <token>   # opcional; se reenvia a conference-service
Content-Type: application/json
```

```json
{
  "name": "Sala Principal",
  "capacity": 120,
  "type": "presencial",
  "locationOrLink": "Edificio A - Auditorio",
  "topicHints": "keynotes, apertura"
}
```

Respuesta `201 Created`:

```json
{
  "id": "4d2f8e4f-9a57-4f0f-a5a6-93f742a9982d",
  "conferenceId": "1c74f64e-c2fd-4f9c-b54b-03af353f62d4",
  "name": "Sala Principal",
  "capacity": 120,
  "type": "presencial",
  "locationOrLink": "Edificio A - Auditorio",
  "topicHints": "keynotes, apertura",
  "createdAt": "2026-05-10T00:00:00Z"
}
```

### Listar salas de una conferencia

```http
GET /rooms/conference/{conferenceId}
```

Devuelve `200 OK` con una lista ordenada por `name` ascendente.

### Consultar sala por id

```http
GET /rooms/{roomId}
```

Devuelve `200 OK` si existe o `404 Not Found` si no se encuentra.

### Eliminar sala

```http
DELETE /rooms/{roomId}
```

Devuelve `204 No Content` si elimina la sala o `404 Not Found` si no existe.

## Campos y restricciones

| Campo | Restricciones |
| --- | --- |
| `name` | Obligatorio, maximo 120 caracteres, se guarda sin espacios externos. Debe ser unico por conferencia sin distinguir mayusculas/minusculas. |
| `capacity` | Minimo `1`. En la entidad y la tabla es no nulo; enviarlo siempre aunque el DTO solo declare `@Min`. |
| `type` | Obligatorio, maximo 80 caracteres, se guarda sin espacios externos. |
| `locationOrLink` | Obligatorio, maximo 255 caracteres, se guarda sin espacios externos. |
| `topicHints` | Opcional, maximo 255 caracteres; valores en blanco se guardan como `null`. |

## Errores

Errores de dominio:

```json
{
  "timestamp": "2026-05-10T00:00:00Z",
  "status": 404,
  "error": "Sala no encontrada"
}
```

Errores de validacion:

```json
{
  "timestamp": "2026-05-10T00:00:00Z",
  "status": 400,
  "error": "Validation failed",
  "details": {
    "capacity": "La capacidad debe ser mayor a 0"
  }
}
```

Codigos relevantes:

- `400 Bad Request`: validacion Bean Validation o datos obligatorios en blanco.
- `404 Not Found`: sala inexistente o conferencia inexistente al crear.
- `409 Conflict`: ya existe una sala con el mismo nombre en la conferencia.

Otros errores HTTP devueltos por `conference-service` durante la validacion se
propagan como errores del cliente HTTP de Spring.

## OpenAPI

El proyecto incluye `springdoc-openapi-starter-webmvc-ui` y no define rutas
custom de Springdoc. Con la aplicacion levantada, usa los endpoints por defecto
de Springdoc para inspeccionar el contrato generado, por ejemplo:

- `/v3/api-docs`
- `/swagger-ui/index.html`

## Notas operativas y pitfalls

- `spring.jpa.hibernate.ddl-auto=update` permite que Hibernate actualice el
  esquema automaticamente. Revisar esta configuracion antes de usar el servicio
  en ambientes donde las migraciones deban ser controladas.
- `ROOM_DB_URL` y `ROOM_SERVICE_PORT` no tienen valores por defecto; si faltan,
  la aplicacion no puede arrancar correctamente.
- La creacion de salas depende de que `conference-service` responda a
  `/conferences/get/{id}`. Si ese servicio requiere autenticacion, enviar el
  header `Authorization` al crear la sala.
- El servicio no valida la existencia de la conferencia al listar salas por
  `conferenceId`; solo filtra las salas existentes en su base de datos.
- Los valores `name`, `type`, `locationOrLink` y `topicHints` se recortan con
  `String.trim()` antes de persistir. Los controles `@Size` se aplican al JSON
  recibido antes de ese recorte.
- No existe una restriccion unica de base de datos para
  `(conference_id, lower(name))`; la deteccion de duplicados vive en
  `RoomService`. Si hay escrituras concurrentes para el mismo nombre, considerar
  una migracion con indice unico funcional antes de usar el servicio con alta
  concurrencia.

## Troubleshooting

| Sintoma | Causa probable | Verificacion |
| --- | --- | --- |
| La aplicacion no arranca y falla resolviendo placeholders | Falta `ROOM_DB_URL` o `ROOM_SERVICE_PORT` | Exportar variables o crear `.env` en la raiz del repo. |
| `POST /rooms/conference/{id}` responde `400` con `No se pudo validar la conferencia` | `CONFERENCE_SERVICE_URL` apunta a una URL incorrecta, el servicio no esta disponible o la llamada no pudo construirse | Probar `GET ${CONFERENCE_SERVICE_URL}/conferences/get/{id}` desde el mismo entorno. |
| `POST /rooms/conference/{id}` responde `404` con `La conferencia no existe` | `conference-service` devolvio `404` para el id enviado | Confirmar que el UUID existe en `conference-service` y que el token usado permite consultarlo. |
| `POST` responde `409` aun cambiando mayusculas/minusculas | Ya existe una sala con el mismo `name` normalizado para la conferencia | Listar `GET /rooms/conference/{conferenceId}` y comparar nombres ignorando mayusculas. |

## Alcance de eventos

Este repositorio no contiene consumers RabbitMQ ni handlers para
`paper.evaluated`. Aunque el `pom.xml` incluye dependencias AMQP, el contrato
RabbitMQ de `paper.evaluated` se centraliza fuera de este servicio, en
`scheduler-service/contracts/`.
