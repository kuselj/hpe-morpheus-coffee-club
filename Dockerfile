# =============================================================================
# HPE Morpheus Coffee Club - 'prod-cloud' container image (Render)
#
# Three stages:
#   1. node  - builds the React production bundle
#   2. jdk   - compiles Spring Boot, injecting stage 1's static output
#   3. jre   - minimal runtime that executes the standalone JAR
#
# The application reads the PORT environment variable Render injects and falls
# back to 8080 when it is not set (see application.yml).
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 - Build the React frontend
# -----------------------------------------------------------------------------
FROM node:22-alpine AS frontend-build

WORKDIR /build/frontend

# Dependencies first so the layer caches while only source files change.
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY frontend/ ./
RUN npm run build


# -----------------------------------------------------------------------------
# Stage 2 - Compile the Spring Boot backend and package the executable JAR
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS backend-build

WORKDIR /build

# Warm the Maven cache before the sources are copied in. Best-effort: anything it misses is
# fetched by the package step below, so a partial resolve must not fail the build.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline || true

COPY src ./src

# Inject the compiled frontend so it is packaged into the JAR's static resources.
# The 'prod' Maven profile is deliberately NOT used here: the frontend has
# already been built by stage 1, so there is no need to install Node again.
COPY --from=frontend-build /build/frontend/dist ./src/main/resources/static

RUN mvn -B clean package -DskipTests \
    && cp target/*.jar /build/app.jar


# -----------------------------------------------------------------------------
# Stage 3 - Minimal JRE runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Run as an unprivileged user, and give it ownership of the H2 data directory.
RUN addgroup -S coffee && adduser -S coffee -G coffee \
    && mkdir -p /app/data \
    && chown -R coffee:coffee /app

COPY --from=backend-build --chown=coffee:coffee /build/app.jar /app/app.jar

USER coffee

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Documentation only; Render routes to whatever PORT it injects.
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
