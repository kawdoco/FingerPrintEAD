#!/usr/bin/env bash
# Runs the packaged jar (built by ../package.sh).   ./run-jar.sh   or   ./run-jar.sh local
cd "$(dirname "$0")"
JAR=target/attendance-system.jar
[ -f "$JAR" ] || { echo "$JAR not found. Run ../package.sh first."; exit 1; }
if [ "$1" = "local" ]; then exec java -jar "$JAR" --spring.profiles.active=local; fi
[ -f .env ] || { echo "backend/.env not found - copy .env.example to .env first"; exit 1; }
set -a; . ./.env; set +a
exec java -jar "$JAR"
