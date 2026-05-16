# CollabBoard

Desktop collaborative whiteboard built with **JavaFX** and **Spring Boot**. Users sign up, join sessions over **LAN** or **cloud** (WebSockets / STOMP), draw together in real time, and use optional screen-sharing controls.

## Features

- **Accounts**: Sign up, login, JWT-based sessions (`/api/auth/signup`, `/api/auth/login`, `/api/auth/me`)
- **LAN collaboration**: Host or join by IP on port **12345** (peer-style socket workflow)
- **Cloud collaboration**: Room codes with STOMP over **`/ws`**; messages under `/app/board/{roomCode}`, broadcasts on `/topic/board/{roomCode}`
- **Whiteboard**: Shared drawing synced per room
- **Screen sharing**: Capture quality / FPS options in the desktop UI (JavaCV / webcam-related stack where enabled)
- **Password reset**: OTP flow via email when SMTP is configured

## Tech stack

| Layer | Technologies |
|--------|----------------|
| UI | JavaFX 22, FXML |
| Backend | Spring Boot 3.2, Spring Security, Spring WebSocket (STOMP), JPA |
| Database | PostgreSQL |
| Auth | JWT (JJWT), BCrypt passwords |
| Mail | Spring Mail (e.g. Gmail SMTP + app password) |
| Build | Maven, Java 17 |

## Repository layout

Maven project:

```text
collabboard/
├── pom.xml
├── Dockerfile
└── src/main/java/com/example/collabboard/
```

## Prerequisites

- **JDK 17**
- **Maven** (or use the included `./mvnw`)
- **PostgreSQL** and an empty database (default URL uses `collabboard_db` on `localhost:5432`)

## Configuration

1. Copy `collabboard/src/main/resources/application.properties.example` to `application.properties`.
2. Set database credentials and (optional) mail settings.

Supported environment variables (see example file):

| Variable | Purpose |
|----------|---------|
| `DB_USERNAME` | PostgreSQL user |
| `DB_PASSWORD` | PostgreSQL password |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password (use an app password for Gmail) |

**JWT secret (production):** set `COLLABBOARD_JWT_SECRET` to a long random string. If unset, the app uses a development default — **do not use that in production**.

Do **not** commit real passwords or `application.properties` if it contains secrets.

## How to run

From `collabboard/`:

### Desktop app (JavaFX + embedded Spring Boot)

Recommended:

```bash
./mvnw javafx:run
```

Main class used by the JavaFX Maven plugin: `com.example.collabboard.JavaFxApplication`.

Entry point that launches the desktop client: `com.example.collabboard.CollabboardApplication`.

### Backend only (no JavaFX UI)

For a server deployment or API/WebSocket-only process:

```bash
./mvnw spring-boot:run
```

Packaged JAR uses **`ServerApplication`** as the Spring Boot entry point (`start-class` in `pom.xml`).

### Docker

Build context is `collabboard/`:

```bash
cd collabboard
docker build -t collabboard .
docker run -p 8080:8080 \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your_password \
  -e COLLABBOARD_JWT_SECRET=your_long_secret \
  collabboard
```

The image exposes **8080** and runs `java -jar app.jar`.

## Cloud WebSocket URL (developers)

The desktop client’s cloud mode uses a **hardcoded** WebSocket URL in `CollaborationService` (currently pointed at a Render deployment; local `ws://localhost:8080/ws` is commented in source). For your own backend, change that URL to match where **`ServerApplication`** is reachable (`wss://` when TLS-terminated).

## API snapshot

| Method | Path | Notes |
|--------|------|--------|
| POST | `/api/auth/signup` | Create account |
| POST | `/api/auth/login` | Returns JWT |
| GET | `/api/auth/me` | Authenticated username |

WebSocket/STOMP endpoint: **`/ws`** (clients subscribe/publish per room as implemented in `WhiteboardSocketController`).

## License

This project is licensed under the [MIT License](LICENSE).

## Contributing

Pull requests welcome — describe LAN vs cloud testing steps for whiteboard changes.
