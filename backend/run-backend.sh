#!/usr/bin/env bash
# macOS / Linux launcher.   ./run-backend.sh   or   ./run-backend.sh local
cd "$(dirname "$0")"
if [ "$1" = "local" ]; then
  exec mvn spring-boot:run -Dspring-boot.run.profiles=local
fi
if [ ! -f .env ]; then echo "backend/.env not found - copy .env.example to .env first"; exit 1; fi
set -a; . ./.env; set +a
exec mvn spring-boot:run
