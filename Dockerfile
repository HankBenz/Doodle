FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && cp build/libs/*-SNAPSHOT.jar /workspace/application.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 doodle
COPY --from=build /workspace/application.jar /app/application.jar
USER doodle

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
