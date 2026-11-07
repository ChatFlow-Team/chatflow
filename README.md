# ChaFlow — corrected starter project

Java 21 + JavaFX desktop client, Java TCP socket server, Docker and persistent named volume.

## Requirements
- JDK 21
- Maven 3.9+
- Docker Engine and Docker Compose plugin

## 1. Start the server
Run from the project root (the folder containing docker-compose.yml):

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f chaflow-server
```

The server listens on TCP port 5050. The named Docker volume `chaflow-data`
persists `/app/data/server.log`.

Stop the container but keep the volume:
```bash
docker compose down
```

Delete the volume and its data (destructive):
```bash
docker compose down -v
```

## 2. Run the JavaFX client
In another terminal, from the project root:

```bash
mvn clean javafx:run
```

The window displays “Welcome to ChaFlow!” and reports whether the socket server answered.

## Structure
```text
ChaFlow/
├── pom.xml
├── docker-compose.yml
├── src/main/java/
│   ├── module-info.java
│   └── com/chaflow/client/
│       ├── ChaFlowApp.java
│       └── SocketClient.java
└── server/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/chaflow/server/SocketServer.java
```

Important: Docker Compose builds with `server/` as its context. Therefore
`COPY src ./src` in `server/Dockerfile` correctly copies `server/src`.
