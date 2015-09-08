#!/bin/bash

# Create main application image
cd ..
mvn clean package docker:build
