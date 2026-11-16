# ChaFlow — JavaFX + Java Socket Server + PostgreSQL + Flyway + Docker

ChaFlow is a Java 21 desktop messaging application. The JavaFX client communicates with a Java TCP socket server. The server authenticates accounts, manages sessions, user discovery, friend requests, direct conversations and message history. PostgreSQL runs in Docker and Flyway owns every schema change.

## Implemented

- Phone-number registration and login; usernames are unique.
- PBKDF2 password hashing (no plaintext passwords stored).
- Server-issued in-memory session token required for every authenticated operation.
- Account discovery by username, display name or phone number.
- Optional friend system: send, accept, decline and remove friends; list incoming requests and accepted friends.
- Direct conversations can be opened with any registered user, whether or not the two users are friends.
- A unique canonical user pair prevents duplicate private conversations, including concurrent creation attempts.
- Conversation creation and participant insertion run in one database transaction.
- Send and retrieve text messages; server verifies conversation membership before reading or sending.
- Message history is presented oldest-to-newest. The TCP event stream refreshes the selected history and conversation previews as messages arrive.
- Docker Compose starts PostgreSQL and the socket server; Flyway runs automatically at server startup.

## Requirements

- Docker Engine and Docker Compose plugin
- JDK 21 and Maven 3.9+ (to run the JavaFX desktop client locally)

## 1. Start database and socket server

From the project root (where `docker-compose.yml` is located):

```bash
docker compose up --build -d
```

The server waits until PostgreSQL is healthy, then runs all pending Flyway migrations before opening TCP port `5050`.

Check startup:

```bash
docker compose ps
docker compose logs -f chaflow-server
```

The server waits for the PostgreSQL health check, runs pending Flyway migrations, then listens on TCP port `5050`.

## 2. Verify migrations and accounts

```bash
docker compose exec postgres psql -U postgres -d chatflow_db -c "SELECT installed_rank, version, description, success FROM chatflow.flyway_schema_history ORDER BY installed_rank;"
docker compose exec postgres psql -U postgres -d chatflow_db -c "SELECT id_utilisateur, numero_telephone, pseudo, nom_affiche FROM chatflow.utilisateur ORDER BY id_utilisateur;"
```

Migrations:

- `V1__create_chatflow_schema.sql`: users, conversations, participants, messages and recipient statuses.
- `V2__insert_demo_users.sql`: four demo accounts.
- `V3__friends_and_private_conversations.sql`: optional friend requests/friendships, canonical direct-conversation pair, activity timestamp and indexes.

**Do not edit an applied migration.** Add future changes as `V4__...sql`, `V5__...sql`, etc. V3 is additive and does not drop existing tables or messages.

## 3. Run the JavaFX client

Keep Docker running. In another terminal, from the project root:

```bash
mvn clean javafx:run
```

The client defaults to `127.0.0.1:5050`. To use another server host/port:

```bash
mvn javafx:run -Djavafx.args="-Dchaflow.host=192.168.1.10 -Dchaflow.port=5050"
```

### Regenerate the WebView stylesheet

After editing the HTML, JavaScript or Tailwind source, run these commands from the project root:

```bash
npm ci
npm run build:css
```

Tailwind CSS 3.4.19 scans the HTML and JavaScript under `src/main/resources/web`.
Commit the generated `chatflow.css` along with its source changes. Maven copies the
HTML, JavaScript, CSS, font and logo from this directory into the client resources.
JavaFX 21.0.6 reads these local resources at launch. Node.js is only used to regenerate
the stylesheet. [Inter 4.1](https://github.com/rsms/inter/releases/tag/v4.1) is bundled as static TrueType fonts with the OFL license.

On an ARM64 Docker host, build and run the TCP server in AMD64 mode because JavaFX
21.0.6's Linux ARM64 artifacts are unavailable in Maven Central:

```bash
DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose up --build -d
```

Successful registration signs the new account in and opens the messenger directly.
Signing in afterwards opens the welcome screen, where the messenger can be opened.

The messenger opens with a 1216 × 680 content area. Resize it to 880 × 600
to review the compact layout. Docker Compose runs PostgreSQL and the TCP server.
Start the JavaFX client on the host with the Maven JavaFX goal above. The
shaded `target/chaflow-server.jar` is the TCP server entry point.

## 4. Demo accounts

All demo accounts use the password `ChatFlow123!`.

| Display name | Phone number | Username |
|---|---|---|
| Sara Demo | +212600000001 | sara_demo |
| Yassine Demo | +212600000002 | yassine_demo |
| Amina Demo | +212600000003 | amina_demo |
| Omar Demo | +212600000004 | omar_demo |

## 5. How to use the new features

1. Sign in with two different demo accounts (run two client instances to test both sides).
2. In **Find by username or phone number**, search for a registered user.
3. Select **Message** to open or create a direct conversation. No friend request is required.
4. Select **+ Friend** to send a friend request. The recipient sees it under **Friend requests** and can accept or decline.
5. Accepted users appear under **Friends**. Removing a friend does not delete the direct conversation or its message history.
6. Send messages. The other client refreshes messages and conversation previews approximately every three seconds.

## 6. Database and environment configuration

Docker Compose development defaults:

- Database: `chatflow_db`
- User: `postgres`
- Password: `chatflow_dev_password`
- Host PostgreSQL port: `5433` (container port `5432`)
- Socket server port: `5050`

Override `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST_PORT` in the shell or a local `.env` file. `DB_URL`, `DB_USER`, `DB_PASSWORD` configure the server container. Do not commit real credentials.

The PostgreSQL data is persisted in the named Docker volume `chaflow-postgres-data`.

## 7. Stop services safely

```bash
docker compose down
```

This stops containers but preserves the database volume. **Do not run `docker compose down -v` unless you intentionally want to delete the database and all its data.**

## Security and current scope

This is a coursework/development implementation. The session token is held in server memory and the current TCP connection is not encrypted. Do not expose port `5050` to the public internet. Production deployment needs TLS, persistent/revocable sessions, rate limiting, robust phone-number verification, and a persistent push/event channel. The current client polls for new messages every three seconds; it is not yet a server-push WebSocket-style implementation. Group chat, attachments, read receipts and message editing/deletion are not included in this iteration.


## Application architecture

The code is organized into explicit layers. The existing socket wire format and
database schema are retained.

### JavaFX client (`src/main/java/com/chaflow/client`)

- `view`: the local WebView host and messenger actions. The connection, registration and messenger pages live in `src/main/resources/web`.
- `controller`: UI action coordinators (`AuthController`, `MessengerController`,
  `ChatController`).
- `service`: client use cases and the socket transport adapter (`AuthService`,
  `ChatService`, `SocketService`).
- `model`: client domain records (`User`, `Message`, `Conversation`,
  `FriendRequest`).
- `SocketClient`: low-level implementation of the existing TCP protocol.
- `ChaFlowApp`: JavaFX application entry point and screen lifecycle.

### Socket server (`src/main/java/com/chaflow/server`)

- `SocketServer`: TCP lifecycle, request framing, session lookup and response
  serialization only.
- `controller`: socket-facing request controllers.
- `service`: application use cases and validation (`AuthService`, `ChatService`).
- `auth`: password hashing utilities.
- `repository`: persistence contracts and JDBC repository adapters.
- `dao`: SQL and JDBC mapping, including `ChatDAO` and the existing entity DAOs.
- `model`: persistence/domain records and enums.
- `db` and `config`: shared PostgreSQL DataSource, Flyway and runtime settings.

The server dependency direction is:
`SocketServer -> Controller -> Service -> Repository -> DAO -> PostgreSQL` for both chat and authentication.
The JavaFX client never receives database credentials and only communicates
with the server over the existing socket protocol.

### Verification checklist

After extracting the project, use Java 21 and run:

```bash
cd chatflow/server
mvn clean package
cd ..
mvn clean javafx:run
```

For Docker-backed server startup, from `chatflow/` run:

```bash
docker compose up --build -d
docker compose logs -f chaflow-server
```

Run the manual acceptance checks in this order: create an account, sign out,
sign in, search another account by username and phone, send/accept a friend
request, start a direct conversation, send a message, restart the client, and
confirm message history is still present. The ZIP integrity check is not a
substitute for these runtime integration tests.
