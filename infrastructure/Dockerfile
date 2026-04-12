FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app
COPY id-generation/build/libs/app.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
