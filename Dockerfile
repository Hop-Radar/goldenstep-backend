FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --gid 10001 goldenstep \
    && useradd --uid 10001 \
        --gid goldenstep \
        --no-create-home \
        --shell /usr/sbin/nologin \
        goldenstep

COPY --chown=goldenstep:goldenstep build/libs/goldenstep.jar /app/app.jar

USER goldenstep

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]