FROM node:20-alpine AS frontend
WORKDIR /frontend
COPY frontend-angular/package.json frontend-angular/package-lock.json ./
RUN npm ci
COPY frontend-angular/ ./
RUN npm run build -- --configuration production

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY apiPenduraAi/pom.xml ./
COPY apiPenduraAi/src/main ./src/main
COPY --from=frontend /frontend/dist/frontend-angular/browser/ ./src/main/resources/static/
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/target/apiPenduraAi-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
