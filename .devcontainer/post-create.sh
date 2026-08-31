#!/usr/bin/env bash
set -e

echo "==> Setting up Classify dev environment"

# --- Backend: pre-fetch Maven dependencies so the first 'mvn spring-boot:run' is fast ---
if [ -f backend/pom.xml ]; then
  echo "==> Pre-fetching backend (Maven) dependencies..."
  (cd backend && mvn -q -DskipTests dependency:go-offline) || echo "!! Dependency pre-fetch failed — 'mvn spring-boot:run' will fetch them on first run instead."
fi

# --- Frontend: install npm dependencies ---
if [ -f frontend/package.json ]; then
  echo "==> Installing frontend (npm) dependencies..."
  (cd frontend && npm ci)
fi

# --- Seed .env files from examples if they don't exist yet ---
[ -f backend/.env ] || { cp backend/.env.example backend/.env; echo "==> Created backend/.env from example"; }
[ -f frontend/.env ] || { cp frontend/.env.example frontend/.env; echo "==> Created frontend/.env from example"; }

echo "==> Setup complete. Postgres + MinIO will start automatically (postStartCommand)."
echo "==> Next: open two terminals —"
echo "      Terminal 1: cd backend && mvn spring-boot:run"
echo "      Terminal 2: cd frontend && npm run dev"
