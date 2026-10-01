FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
	&& apt-get upgrade -y \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system app \
	&& useradd --system --gid app --home-dir /app app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
