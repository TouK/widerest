FROM java:8-jre

EXPOSE 8080

VOLUME /tmp
ADD widerest-api-1.0.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","-Druntime.environment=production", "/app.jar", "--spring.profiles.active=production"]
