FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/data
COPY --from=build /build/target/chaflow-server.jar /app/server.jar
EXPOSE 5050
ENV CHAFLOW_PORT=5050
ENV CHAFLOW_DATA_DIR=/app/data
VOLUME ["/app/data"]
CMD ["java", "-jar", "/app/server.jar"]
