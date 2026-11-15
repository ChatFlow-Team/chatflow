FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/chaflow-server.jar /app/server.jar
EXPOSE 5050
ENV CHAFLOW_PORT=5050
ENV DB_URL=jdbc:postgresql://postgres:5432/chatflow_db
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
