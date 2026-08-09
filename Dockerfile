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

WORKDIR /build

# The frontend is an npm workspace, so both manifests are needed before dependencies can be
# installed. Copying them on their own keeps this layer cached while only source files change.
#
# package-lock.json is deliberately NOT copied. npm only records the native binaries matching the
# platform the lockfile was generated on, so a lockfile produced on Windows pins the win32 builds
# of rolldown (Vite's bundler), @tailwindcss/oxide and lightningcss. Under `npm ci` this image
# would then install no usable binding at all and the build would die on an ESM import. Resolving
# here instead picks the linux-musl builds this base image needs; versions still come from the
# semver ranges in the manifests.
COPY package.json ./
COPY frontend/package.json ./frontend/
RUN npm install --no-audit --no-fund

COPY frontend/ ./frontend/
RUN npm run build --workspace=frontend


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
