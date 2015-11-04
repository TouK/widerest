#!/bin/bash

# Push generated images to TouK repository

# docker login -u mbr https://docker.touk.pl
echo "Just a reminder: login to repository and build images before pushing"
read -p "Press any key to continue"

# Push production database
docker push touk/widerest:api
