FROM maven:3.9.14-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=10000

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=build /app/target/ganaderia4backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown app:app app.jar

USER app

EXPOSE 10000

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent "http://localhost:${SERVER_PORT:-${PORT:-8080}}/actuator/health" > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${SERVER_PORT:-${PORT:-8080}} -jar app.jar"]
