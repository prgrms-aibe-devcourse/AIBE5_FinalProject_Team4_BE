#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR/deploy"

if [[ ! -f .env ]]; then
  echo "deploy/.env 가 없습니다. env.prod.example 을 복사해 값을 채워 주세요."
  exit 1
fi

if [[ ! -f application-prod.yml ]]; then
  echo "deploy/application-prod.yml 이 없습니다."
  echo "cp ../src/main/resources/application-prod.yml.example application-prod.yml"
  exit 1
fi

docker compose -f docker-compose.prod.yml build app
docker compose -f docker-compose.prod.yml up -d

echo "배포 완료. health: curl -sf http://127.0.0.1:8080/actuator/health"
