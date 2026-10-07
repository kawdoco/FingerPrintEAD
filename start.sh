#!/usr/bin/env bash
# One-command launcher (macOS/Linux).  ./start.sh   or   ./start.sh supabase
set -e; cd "$(dirname "$0")"
for t in java mvn node npm; do command -v $t >/dev/null || { echo "Missing '$t' (need JDK 17+, Maven, Node 18+)"; exit 1; }; done
IP=$(hostname -I 2>/dev/null | awk '{print $1}')
[ -d frontend/node_modules ] || (cd frontend && npm install)
if [ "$1" = "supabase" ]; then (cd backend && ./run-backend.sh) & else (cd backend && ./run-backend.sh local) & fi
trap 'kill 0' EXIT
echo -e "\nLive display: http://localhost:5173/\nAdmin: http://localhost:5173/admin/login (admin / ChangeMe123!)\nESP32 URL: http://${IP:-<this-pc-ip>}:8080"
cd frontend && npm run dev -- --host
