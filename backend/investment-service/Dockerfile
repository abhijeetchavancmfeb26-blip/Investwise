# Build context is ./backend so Maven can see the parent pom.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY investment-service/pom.xml investment-service/
COPY investment-service/src investment-service/src
RUN mvn -B -q -pl investment-service clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/investment-service/target/investment-service.jar app.jar
USER app
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --retries=10 \
  CMD wget -qO- http://localhost:8082/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
