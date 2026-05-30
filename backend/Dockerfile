# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM tomcat:10.1-jdk17
# Clean default webapps
RUN rm -rf /usr/local/tomcat/webapps/*
# Copy compiled WAR and rename to ROOT.war to serve at the root context path (/)
COPY --from=build /app/target/api.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
