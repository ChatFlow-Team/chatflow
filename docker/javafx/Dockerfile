
FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

# Installer les bibliothèques graphiques et noVNC
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    x11vnc \
    novnc \
    websockify \
    libgtk-3-0 \
    libasound2t64 \
    libgl1 \
    libxi6 \
    libxtst6 \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Copier le projet JavaFX
COPY pom.xml .
COPY src ./src

# Compiler l'application
RUN mvn -B -DskipTests package

# Configurer l'écran virtuel et le serveur
ENV DISPLAY=:99
ENV JAVA_TOOL_OPTIONS="-Dchaflow.host=chaflow-server -Dchaflow.port=5050 -Dprism.order=sw"

EXPOSE 6080

# Démarrer l'écran virtuel, noVNC et JavaFX
CMD ["sh", "-c", "Xvfb :99 -screen 0 1280x720x24 & sleep 2; x11vnc -display :99 -forever -shared -localhost -nopw -rfbport 5900 -bg; websockify --web /usr/share/novnc 0.0.0.0:6080 127.0.0.1:5900 & exec mvn -q javafx:run"]
