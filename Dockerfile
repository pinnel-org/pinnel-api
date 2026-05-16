# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy the Maven wrapper and pom first so the dependency-download layer is
# cached and only invalidated when pom.xml changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 1001 --shell /sbin/nologin pinnel
COPY --from=build --chown=pinnel:pinnel /workspace/target/*.jar /app/app.jar

USER pinnel
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
