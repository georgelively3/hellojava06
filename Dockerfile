FROM gradle:8.5-jdk17 AS builder
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

FROM openjdk:17-jdk-slim
EXPOSE 8080
RUN mkdir /app
COPY --from=builder /home/gradle/src/build/libs/*.jar /app/hellojava06.jar

# Add environment variable defaults for safer startup
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-default}
ENV DB_PORT=${DB_PORT:-5432}
ENV DB_NAME=${DB_NAME:-hellojava06}

ENTRYPOINT ["java", "-jar", "/app/hellojava06.jar"]
