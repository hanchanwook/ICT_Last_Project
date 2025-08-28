FROM openjdk:21
WORKDIR /app
COPY build/libs/aa-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 19091

ENTRYPOINT [ "java","-jar","app.jar","--spring.profiles.active=prod" ]