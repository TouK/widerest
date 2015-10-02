# Widerest

### Setting up development environment
Use with IntelliJ. Compile `Application.java` in widerest-api. \
PostgreSQL database is required to be run, launch it using `devel-database.sh` \
Widerest will be available on port 8080.

### Creating production docker image
```bash
mvn clean install # Optionally run with -DskipTests
cd api
mvn package docker:build # Optionally run with -DskipTests
```
`touk/widerest:api` image will be generated
