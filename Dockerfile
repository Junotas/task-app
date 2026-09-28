FROM eclipse-temurin:25-jdk AS build

WORKDIR /build
COPY backend/mvnw backend/mvnw
COPY backend/.mvn backend/.mvn
COPY backend/pom.xml backend/pom.xml

WORKDIR /build/backend
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

WORKDIR /build
COPY frontend frontend
COPY backend/src backend/src

WORKDIR /build/backend
RUN ./mvnw --batch-mode --no-transfer-progress clean package -DskipUnitTests=true

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /build/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]