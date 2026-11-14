FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
        org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies \
    && mkdir /javafx \
    && find target/dependency -name '*-linux*.jar' -exec cp {} /javafx/ \;

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgtk-3-0 libgl1-mesa-dri libgl1 libxtst6 fonts-dejavu-core \
        xvfb x11-utils x11vnc openbox novnc websockify curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /tmp/.X11-unix && chmod 1777 /tmp/.X11-unix \
    && useradd --create-home --uid 1000 chaflow
WORKDIR /app
COPY --from=build /build/target/chaflow-1.0.0.jar /app/chaflow.jar
COPY --from=build /javafx/ /app/lib/
COPY docker/frontend/start.sh /app/start.sh
USER chaflow
ENV DISPLAY=:99
EXPOSE 6080
ENTRYPOINT ["bash", "/app/start.sh"]
