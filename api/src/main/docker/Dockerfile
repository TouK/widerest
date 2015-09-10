FROM java:8-jre

EXPOSE 8080

VOLUME /tmp
ADD spring-instrument-4.1.7.RELEASE.jar instrument.jar
ADD widerest-api-1.0.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java", "-javaagent:instrument.jar", "-Djava.security.egd=file:/dev/./urandom","-jar","-Druntime.environment=production", "/app.jar", "--spring.profiles.active=production", "--logging.path=/var/log/widerest"]
