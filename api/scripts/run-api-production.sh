#!/bin/bash

# Stop any existing instances
docker rm -f widerest-db-prod
docker rm -f widerest-api

# Run database and application
docker run -d --name widerest-db widerest-db-prod
docker run -d -p 80:8080 --name widerest-api --link widerest-db:widerestdb touk/widerest-api
