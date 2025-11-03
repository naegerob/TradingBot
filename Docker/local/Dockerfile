# Stage 1: Cache Gradle dependencies
FROM gradle:8.13.0-jdk17-corretto-al2023 AS cache

ENV GRADLE_USER_HOME_CACHE=/home/gradle/cache_home
RUN mkdir -p $GRADLE_USER_HOME_CACHE
ENV GRADLE_USER_HOME=/home/gradle/.gradle
RUN mkdir -p $GRADLE_USER_HOME

COPY build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app

# Only download dependencies here
RUN gradle dependencies --no-daemon || true

# Stage 2:
FROM gradle:8.13.0-jdk17-corretto-al2023  AS runner

ENV REPORT_DIR=/app/reports
COPY --chown=gradle:gradle . /home/gradle/app
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
WORKDIR /home/gradle/app

CMD [ "/bin/bash" ]