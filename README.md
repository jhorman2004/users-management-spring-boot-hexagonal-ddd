# Users Management — microservicios con Spring Boot, Arquitectura Hexagonal y DDD

Gestión de usuarios con Java 17 y Spring Boot, dividida en tres microservicios que se comunican
por HTTP (síncrono) y por Kafka (asíncrono). Cada servicio sigue la arquitectura hexagonal
(`domain` / `application` / `infrastructure`).

| Módulo | Puerto local | Qué hace |
|---|---|---|
| `users-service` | 8080 | API pública: CRUD de usuarios, login JWT, Swagger. Orquesta el registro |
| `accounts-service` | 8081 | API interna: crea la cuenta de cada usuario (protegida con `X-Internal-Api-Key`) |
| `notifier-service` | 8082 | Lee el tópico de notificaciones y envía los correos por la API de Brevo |
| `notification-events` | — | Contrato del mensaje de Kafka (`EmailNotificationEvent`), compartido |

## Flujo de registro

1. **users** guarda el usuario en `crud_usuarios` (síncrono).
2. **users** pide a **accounts** crear la cuenta (`POST /api/accounts`, síncrono, con timeout,
   reintentos y circuit breaker). Si falla, **users** borra el usuario recién guardado
   (compensación) y responde `503`.
3. **accounts** guarda la cuenta en `crud_cuentas` y publica `ACCOUNT_CREATED` en Kafka.
4. **users** publica `USER_CREATED` en Kafka.
5. **notifier** consume ambos mensajes del tópico `notifications.email` y envía los correos.
   Ignora los mensajes repetidos (mismo `eventId`), reintenta los fallos y manda a
   `notifications.email.DLT` los que no puede procesar.

Las actualizaciones de usuario publican `USER_UPDATED`. Los mensajes nunca llevan contraseñas.

## Verificación

```bash
./mvnw clean verify
```

Compila y prueba los cuatro módulos. En Windows se puede utilizar `mvnw.cmd`.

## Desarrollo local desde IntelliJ

1. Levantar solo Kafka con Docker: `docker compose up -d kafka` (queda en `localhost:9092`).
2. Crear la base de cuentas en el MySQL local ejecutando
   `accounts-service/src/main/resources/schema.sql` (la de usuarios ya existe).
3. Copiar los ejemplos de secretos a la carpeta `.environments/` (excluida de Git y de Docker):

   | Ejemplo | Copiar como | Para |
   |---|---|---|
   | `application-secrets.properties.example` | `.environments/application-secrets.properties` | users-service |
   | `accounts-secrets.properties.example` | `.environments/accounts-secrets.properties` | accounts-service |
   | `notifier-secrets.properties.example` | `.environments/notifier-secrets.properties` | notifier-service (solo para correos reales) |

   `accounts.api-key` (users) e `internal.api-key` (accounts) deben tener el mismo valor.
4. Ejecutar la configuración **Microservicios (los 3)** (carpeta `.run/`). notifier-service
   arranca con el perfil `local`, que registra los correos en el log en vez de enviarlos.

## Docker Compose (todo en contenedores)

Copiar `compose.env.example` como `.env`, reemplazar los valores y ejecutar:

```bash
docker compose up -d --build
```

Levanta MySQL (en `127.0.0.1:3307`, con las bases `crud_usuarios` y `crud_cuentas`), Kafka y
los tres servicios. Con `NOTIFIER_PROFILE=local` los correos solo se registran en el log.

## Despliegue en Render (Aiven Kafka + TiDB)

`render.yaml` define los tres servicios web (plan gratuito, Docker, despliegue automático desde
`main`). Todos usan el perfil `cloud`, que conecta con Aiven Kafka por `SASL_SSL`.

1. **Aiven**: crear los tópicos `notifications.email` y `notifications.email.DLT`. Copiar de
   *Connection information* (método SASL) el host:puerto, usuario, contraseña y el CA certificate.
2. **TiDB**: ejecutar en el SQL Editor `users-service/src/main/resources/schema.sql` y
   `accounts-service/src/main/resources/schema.sql`. Copiar host (puerto 4000), usuario y contraseña.
3. **Render**: crear un Blueprint con este repositorio y completar las variables `sync: false`:
   `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_USERNAME`,
   `KAFKA_PASSWORD`, `KAFKA_CA_CERT` (contenido PEM completo), `BREVO_API_KEY` (clave API
   `xkeysib-…`), `BREVO_FROM_ADDRESS`, y en users `ACCOUNTS_BASE_URL` y `NOTIFIER_WAKEUP_URL`
   (las URL `.onrender.com` de accounts y del health check de notifier). `INTERNAL_API_KEY` y
   `JWT_SECRET` los genera Render.

**Plan gratuito:** Render duerme los servicios sin tráfico HTTP. Por eso users "despierta" a
notifier tras publicar (`NOTIFIER_WAKEUP_URL`) y espera hasta 70 s a accounts
(`ACCOUNTS_READ_TIMEOUT`). Los mensajes no se pierden mientras notifier duerme: esperan en Kafka.

Las credenciales nunca deben guardarse en `application.properties` ni en `render.yaml`.
