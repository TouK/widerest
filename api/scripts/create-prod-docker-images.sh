#!/bin/bash

# Create production database
docker build -t widerest-db-prod database/production/

# Create main application image
cd ..
mvn clean package docker:build -DskipTests
