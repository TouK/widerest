FROM java:8-jre

EXPOSE 8080

VOLUME /tmp
ADD spring-instrument-4.1.7.RELEASE.jar
ADD widerest-app-1.0.jar
ENTRYPOINT ["java", "-javaagent:spring-instrument-4.1.7.RELEASE.jar", "-Djava.security.egd=file:/dev/./urandom","-jar","-Druntime.environment=production", "widerest-app-1.0.jar", "--spring.profiles.active=production", "--logging.path=/var/log/widerest"]
