FROM gradle:9.8.2-jdk17-noble AS build
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle settings.gradle build.gradle ./

RUN gradle dependencies --no-daemon

COPY --chown=gradle:gradle src ./src

RUN gradle bootJar --no-daemon -x test \
    && cp build/libs/*-SNAPSHOT.jar /home/gradle/application.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 doodle
COPY --from=build /home/gradle/application.jar /app/application.jar
USER doodle

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
