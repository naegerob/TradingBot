# Stage 1: Cache Gradle dependencies
FROM gradle:8.13.0-jdk17-corretto-al2023 as cache

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

COPY --from=cache $GRADLE_USER_HOME_CACHE $$GRADLE_USER_HOME
COPY --chown=gradle:gradle ../../.. /home/gradle/app
WORKDIR /home/gradle/app

# NOTE: assumes you have a `buildFatJar` task (custom or from shadow plugin)
RUN gradle clean buildFatJar --no-daemon


# Stage 3: Runtime image
FROM amazoncorretto:17 AS runtime

EXPOSE 8080
WORKDIR /app

# Use COPY with exact file name or wildcard if fatJar is consistent
COPY --from=build /home/gradle/app/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]