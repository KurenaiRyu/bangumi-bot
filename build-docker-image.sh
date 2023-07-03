#!/bin/bash
./gradlew build -x test --parallel --daemon && docker compose build
