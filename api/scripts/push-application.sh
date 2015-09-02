#!/bin/bash

# Push generated images to TouK repository

# docker login -u mbr https://docker.touk.pl
echo "Just a reminder: login to local repository and build images before pushing"
read -p "Press any key to continue"

# Push production database
docker tag widerest-db-prod docker.touk.pl/widerest-db-prod
docker push docker.touk.pl/widerest-db-prod

# Push application
docker tag touk/widerest-api docker.touk.pl/widerest-api
docker push docker.touk.pl/widerest-api
