# ---------- Stage 1: build ----------
# Maven + JDK 17 compiling for Java 8 (the pom sets release 8, so only Java 8 APIs are accepted)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
# Retry up to 3 times: a truncated download on a slow network won't fail the build
RUN mvn -B -q dependency:go-offline || mvn -B -q dependency:go-offline || mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: run ----------
# Real Java 8 runtime
FROM eclipse-temurin:8-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
