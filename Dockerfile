# syntax=docker/dockerfile:1
#
# Imagem do BACKEND Multicore (com.phcpro.MulticoreApplication, headless).
# Multi-stage: compila com Maven, corre num JRE 21 magro.
#
# O runtime inclui o postgresql-client porque o backup físico (ScheduledBackupService)
# corre pg_dump/pg_restore DENTRO do processo. A versão do cliente tem de ser >= à do
# servidor — ajustar PG_MAJOR para coincidir com o teu PostgreSQL (ex.: 18).

# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B -e dependency:go-offline
COPY src ./src
RUN mvn -q -B -e -DskipTests clean package

# ---- Runtime ----
FROM eclipse-temurin:21-jre-jammy AS runtime
ARG PG_MAJOR=16

# postgresql-client-${PG_MAJOR} a partir do repositório oficial PGDG (para pg_dump/pg_restore).
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates gnupg \
 && install -d /usr/share/postgresql-common/pgdg \
 && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
      -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
 && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] http://apt.postgresql.org/pub/repos/apt jammy-pgdg main" \
      > /etc/apt/sources.list.d/pgdg.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends "postgresql-client-${PG_MAJOR}" \
 && apt-get purge -y gnupg && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/*

# Utilizador não-root.
RUN useradd -r -u 1001 -m -d /app appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/backups && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
# Limita o heap à RAM do container (evita OOM-kill em VPS pequenos).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

# Verifica só que a porta 8080 está a aceitar ligações (não há endpoint /health sem actuator).
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
