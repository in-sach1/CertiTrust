#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_DIR"

unset INITIAL_ADMIN_EMAIL INITIAL_ADMIN_PASSWORD

export APP_DEMO_DATA=true
export SPRING_DATASOURCE_URL='jdbc:h2:mem:certitrust-demo;MODE=MySQL;DB_CLOSE_DELAY=-1'
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
export PORT="${PORT:-8080}"

exec mvn spring-boot:run
