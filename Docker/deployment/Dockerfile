# Stage 1: Cache Gradle dependencies
FROM gradle:8.13.0-jdk17-corretto-al2023 AS cache

ENV GRADLE_USER_HOME_CACHE=/home/gradle/cache_home
RUN mkdir -p $GRADLE_USER_HOME_CACHE
ENV GRADLE_USER_HOME=/home/gradle/.gradle
RUN mkdir -p $GRADLE_USER_HOME

COPY build.gradle.* gradle.properties settings.gradle.kts /home/gradle/app/
COPY gradle  /home/gradle/app/gradle
WORKDIR /home/gradle/app

# Only download dependencies here
RUN gradle dependencies --no-daemon || true

# Stage 2: Build Application (fatJar)
FROM gradle:8.13.0-jdk17-corretto-al2023 AS build

COPY --from=cache $GRADLE_USER_HOME_CACHE $GRADLE_USER_HOME
COPY --chown=gradle:gradle ../../.. /home/gradle/app
WORKDIR /home/gradle/app

# NOTE: assumes you have a `buildFatJar` task (custom or from shadow plugin)
RUN gradle clean buildFatJar --no-daemon


# Stage 3: Runtime image
FROM amazoncorretto:17-alpine AS runtime

EXPOSE 8081
ENV WORKDIR=/app

# Use COPY with exact file name or wildcard if fatJar is consistent
COPY --from=build /home/gradle/app/build/libs/*.jar $WORKDIR/app.jar

WORKDIR $WORKDIR
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8081/health || exit 1

# Limit permission at the end, so copy above still works
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup $WORKDIR/security
USER appuser

ENTRYPOINT ["/bin/sh","-c","set -eu; \
  echo 'Prüfe Keystore:' security/tls/keystore.p12; \
  if [ ! -f security/tls/keystore.p12 ]; then echo 'Keystore fehlt' >&2; ls -R security || true; exit 1; fi; \
  exec java -jar app.jar"]