FROM gradle:9.8.1-jdk25@sha256:e4f9e3d037cc0c7d8fc42cd97edb976dfd19a5b7314212b88b6de3807d13cd47 AS development
WORKDIR /workspace
ENV GRADLE_USER_HOME=/workspace/.gradle

FROM development AS build
COPY --chown=gradle:gradle gradlew gradlew.bat build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle src ./src
RUN ./gradlew --no-daemon --console=plain test bootJar

FROM eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 AS runtime
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/build/libs/neguentropia-backend-0.0.1-SNAPSHOT.jar ./application.jar
COPY --chown=10001:10001 data ./data
ENV CORPUS_MANIFEST=/app/data/corpus.json \
    GEMINI_EMBEDDINGS=/app/data/gemini.json \
    LOCAL_EMBEDDINGS=/app/data/local.json
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
