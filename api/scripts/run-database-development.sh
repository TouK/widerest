#!/bin/bash

# Stop if there's one up
docker rm -f widerest-db-devel

# Build if it's not available
docker build -t widerest-db-devel database/development

# Run it, expose port
docker run -d -p 5432:5432 --name widerest-db-devel widerest-db-devel
