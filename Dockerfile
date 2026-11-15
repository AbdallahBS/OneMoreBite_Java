
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/onemorebite.jar app.jar

EXPOSE 10000

CMD ["java", "--add-modules", "jdk.httpserver", "-jar", "app.jar"]
