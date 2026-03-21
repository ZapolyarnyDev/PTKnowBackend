FROM gradle:9.1.0-jdk AS builder

WORKDIR /build

COPY gradlew ./
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre

ARG EXPOSE_PORT
ENV PTKNOW_PORT=${EXPOSE_PORT}

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

WORKDIR /app
COPY --from=builder /build/build/libs/app.jar app.jar

EXPOSE ${EXPOSE_PORT}

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
