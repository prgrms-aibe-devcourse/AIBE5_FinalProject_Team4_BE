# deploy/

EC2에서 BE를 Docker Compose로 실행하기 위한 파일입니다.

## 빠른 시작

```bash
cd deploy
cp env.prod.example .env          # 값 입력
cp ../src/main/resources/application-prod.yml.example application-prod.yml
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

상세 AWS 리소스 생성 순서는 [docs/deploy/aws-setup.md](../docs/deploy/aws-setup.md)를 참고하세요.

## 파일

| 파일 | 설명 |
| --- | --- |
| `env.prod.example` | EC2 `.env` 템플릿 (`.env`는 gitignore) |
| `docker-compose.prod.yml` | app + redis |
| `scripts/deploy.sh` | 빌드 및 기동 |

## 주의

- `deploy/.env`, `deploy/application-prod.yml`은 Git에 올리지 않습니다.
- 운영 프로필: `SPRING_PROFILES_ACTIVE=prod`
- 이미지는 `STORAGE_BACKEND=s3` 시 S3 URL을 DB `imageUrl`에 저장합니다. 로컬 compose는 `local` 기본값을 사용합니다.
