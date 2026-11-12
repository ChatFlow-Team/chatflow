# ChaFlow — JavaFX + PostgreSQL + Flyway + Docker

ChaFlow is a Java 21 desktop client with a Java TCP authentication server and PostgreSQL database. Database schema and demo data are managed by **Flyway migrations**. There is no separate SQL seeder container and no local PostgreSQL socket connection.

## Requirements

- Docker Engine and Docker Compose plugin
- JDK 21 and Maven 3.9+ (only to run the JavaFX desktop client locally)

## 1. Start PostgreSQL and the server

From the project root (the directory containing `docker-compose.yml`):

```bash
docker compose up --build -d
```

The server waits until PostgreSQL is healthy, then runs all pending Flyway migrations before opening TCP port `5050`.

Check startup:

```bash
docker compose ps
docker compose logs -f chaflow-server
```

Expected log messages include:

```text
Flyway migrations completed. ChaFlow database is ready.
ChaFlow authentication server listening on port 5050
```

## 2. Verify Flyway and demo accounts

```bash
docker compose exec postgres psql -U postgres -d chatflow_db -c "SELECT installed_rank, version, description, success FROM chatflow.flyway_schema_history ORDER BY installed_rank;"
docker compose exec postgres psql -U postgres -d chatflow_db -c "SELECT id_utilisateur, numero_telephone, pseudo, nom_affiche FROM chatflow.utilisateur ORDER BY id_utilisateur;"
```

The migrations are in `server/src/main/resources/db/migration`:

- `V1__create_chatflow_schema.sql` creates the `chatflow` schema and tables used by the existing DAO classes.
- `V2__insert_demo_users.sql` inserts four sample accounts. Flyway records each successful migration in `chatflow.flyway_schema_history` and will not run it again. If a non-empty development schema already exists without Flyway metadata, Flyway baselines it at version 0, then applies V1 and V2; the V1 table creation statements are idempotent.

## 3. Run the JavaFX desktop client

Keep Docker running. In a second terminal, from the project root:

```bash
mvn clean javafx:run
```

The desktop app connects to `127.0.0.1:5050`. To use another server address:

```bash
mvn javafx:run -Djavafx.args="-Dchaflow.host=192.168.1.10"
```

## 4. Demo accounts

All four demo accounts use this password:

```text
ChatFlow123!
```

| Display name | Phone number | Username |
|---|---|---|
| Sara Demo | +212600000001 | sara_demo |
| Yassine Demo | +212600000002 | yassine_demo |
| Amina Demo | +212600000003 | amina_demo |
| Omar Demo | +212600000004 | omar_demo |

You can also create a new account from the **Create account** tab. Phone numbers and usernames must be unique. Passwords are stored as PBKDF2-HMAC-SHA256 hashes, never as plain text.

## 5. Database configuration

Docker Compose defaults are for local development:

- Database: `chatflow_db`
- User: `postgres`
- Password: `chatflow_dev_password`
- Host port on your machine: `5433`
- Hostname used by the server container: `postgres:5432`

Override with environment variables `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_HOST_PORT`. Keep real credentials out of source control. The PostgreSQL data is stored in the named Docker volume `chaflow-postgres-data`.

## 6. Stop services safely

```bash
docker compose down
```

This stops containers but preserves the database volume. **Do not use `docker compose down -v` unless you intentionally want to delete the database and all its data.**

## Notes

- Flyway migrations run inside the Java server container using the Docker service hostname `postgres`; they do not use `/var/run/postgresql`.
- Add future schema changes as new files such as `V3__add_profile_fields.sql`. Do not edit a migration that has already been applied to a shared database.
- This TCP authentication protocol is intended for local coursework/development. Do not expose port `5050` to the public internet without adding TLS and stronger session/security controls.
