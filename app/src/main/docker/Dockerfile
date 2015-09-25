FROM java:8-jre

EXPOSE 8080

ADD spring-instrument-4.1.7.RELEASE.jar /usr/share/java/
ADD widerest-app-1.0.jar /usr/share/java/
ENTRYPOINT ["java", "-javaagent:/usr/share/java/spring-instrument-4.1.7.RELEASE.jar", "-Djava.security.egd=file:/dev/./urandom","-jar","-Druntime.environment=production", "/usr/share/java/widerest-app-1.0.jar", "--spring.profiles.active=production", "--logging.path=/var/log/widerest"]
