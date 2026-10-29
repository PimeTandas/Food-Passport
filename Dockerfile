# One image containing the whole app: the React website is built first, then baked into the Spring jar.
# Base images come from AWS's public mirror of the official Docker images (no Docker Hub rate limits).

# ---- 1. build the website ----
FROM public.ecr.aws/docker/library/node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. build the Spring app, with the website inside it ----
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-21-alpine AS api
WORKDIR /api
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
# Tests already ran in CI before this image is built.
RUN mvn -B -q package -DskipTests

# ---- 3. the small runtime image: just a JRE and the jar ----
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=api /api/target/food-passport-backend-*.jar app.jar
# Small server: cap the heap at a share of the container's memory limit, and restart cleanly if it runs out.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
