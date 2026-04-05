# Build
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests spring-boot:repackage
RUN cp target/*[^original].jar /app/app.jar

# RUN
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
# wait for liquibase starts and then go jar
ENTRYPOINT ["sh", "-c", "sleep 10 && java -jar app.jar"] 