# AWS 배포 가이드 (EC2 + RDS + S3)

옷장난감 BE를 AWS에 올릴 때 필요한 리소스와 설정 순서입니다.
운영(`prod`)은 `STORAGE_BACKEND=s3`로 S3에 원본을 저장하고, `imageUrl`은 인증된 `/api/v1/images/**` 프록시 URL로 저장합니다. 로컬 개발은 `local` 기본값으로 디스크 + `/api/v1/images/**`를 사용합니다.

## 아키텍처

```text
[FE] ──HTTPS──▶ [ALB/Nginx] ──▶ [EC2: Spring Boot + Redis(container)]
                                      │
                                      ├──▶ [RDS MySQL]
                                      └──▶ [S3 bucket]  ← private 원본 저장소
```

## 1. RDS (MySQL 8.4)

1. AWS Console → RDS → MySQL 8.4 생성
2. DB name: `closetnangamdb`
3. 퍼블릭 액세스: **아니오** (EC2와 같은 VPC private subnet 권장)
4. 보안 그룹: EC2 보안 그룹에서 **3306** inbound 허용
5. 마스터 계정 또는 앱 전용 계정 생성

최초 스키마는 로컬 DB dump 후 RDS import 합니다. Flyway는 기본 **비활성**(`FLYWAY_ENABLED=false`)이며, 활성화 시 `src/main/resources/db/migration/` SQL을 계속 유지해야 합니다.

```bash
mysqldump -h localhost -P 3307 -u root -p closetnangamdb > closetnangamdb.sql
mysql -h <RDS_HOST> -u <USER> -p closetnangamdb < closetnangamdb.sql
```

Flyway 활성화 절차는 [db/migration/README.md](../../src/main/resources/db/migration/README.md)를 참고하세요. 로컬 누적 변경은 `V1__local_incremental_schema.sql`에 통합되어 있습니다.

## 2. S3

1. 버킷 생성 (예: `closetnangam-images`, region `ap-northeast-2`)
2. Block Public Access는 기본 유지
3. `S3_PUBLIC_BASE_URL`은 비워두면 백엔드 프록시 URL(`/api/v1/images/**`)을 저장합니다.
   CloudFront를 별도로 붙일 경우에만 CloudFront 도메인을 입력합니다.

**현재 코드**: `app.storage.backend=local`이면 `LocalImageStorageService`가 디스크에 저장하고,
`s3`이면 `S3ImageStorageService`가 S3 object key를 `storedPath`에 저장합니다.
두 저장소 모두 브라우저 조회는 `ImageController`의 `/api/v1/images/**` 인증 API를 거칩니다.

## 3. EC2

### 인스턴스

- Amazon Linux 2023 또는 Ubuntu 22.04
- t3.small 이상 (Java 21 + Redis 컨테이너)
- 보안 그룹 inbound:
  - 22 (SSH, 관리 IP만)
  - 80/443 (ALB 또는 Nginx)
  - 8080은 ALB 뒤에서만 열거나 localhost 바인딩

### IAM Role (인스턴스 프로파일)

EC2에 Role 부여 — S3 img_url 연동 시 필요:

- `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` (버킷 ARN scope)
- access key를 `.env`에 넣지 않아도 됨

### 소프트웨어

```bash
sudo yum update -y
sudo yum install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# 재접속 후
docker compose version  # 또는 docker-compose-plugin 설치
```

### 앱 배포

```bash
git clone <BE_REPO> closetnangam-be
cd closetnangam-be/deploy

cp env.prod.example .env
# .env 값 입력 (RDS_HOST, DB_PASSWORD, APP_BASE_URL, FE_BASE_URL, ...)

cp ../src/main/resources/application-prod.yml.example application-prod.yml
# 필요 시 application-prod.yml 미세 조정

chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

헬스체크:

```bash
curl -sf http://127.0.0.1:8080/actuator/health
```

## 4. HTTPS / 도메인

- Route53 + ACM 인증서
- EC2 Nginx reverse proxy + Let's Encrypt, 또는 ALB + ACM 인증서

> **주의**: `docker-compose.prod.yml`의 app 포트는 `127.0.0.1:8080:8080`(loopback)으로만 바인딩됩니다.
> ALB target group을 EC2 private IP:8080으로 직접 연결하면 도달 불가합니다.
> 반드시 EC2 내 Nginx가 loopback으로 프록시하거나, compose 포트를 `0.0.0.0:8080:8080`으로 변경하고
> 보안 그룹을 ALB SG로만 제한한 후 사용하세요.

`application-prod.yml`에 `server.forward-headers-strategy: framework`가 설정되어 있어 OAuth 쿠키 `Secure` 플래그가 HTTPS에서 동작합니다.

### FE/BE 공개 URL 기준

| 배포 형태 | `APP_BASE_URL` | `FE_BASE_URL` / `CORS_ALLOWED_ORIGINS` |
| --- | --- | --- |
| 같은 origin EC2 + Nginx | FE 접속 origin. 예: `https://www.closetnangam.site` | 같은 origin |
| FE/BE 별도 도메인 | BE 공개 origin. 예: `https://api.closetnangam.site` | FE 접속 origin |

같은 origin 배포에서는 Nginx가 `/api/`, `/oauth2/`, `/login/oauth2/`, `/actuator/health`만 Spring Boot로 프록시합니다.
`/login`은 FE SPA 경로이므로 Spring Security가 처리하지 않도록 BE로 프록시하지 않습니다.

## 5. OAuth 콘솔 redirect URI

각 개발자 콘솔에 **운영 URL** 등록:

| Provider | Redirect URI |
| --- | --- |
| Kakao | `{APP_BASE_URL}/login/oauth2/code/kakao` |
| Google | `{APP_BASE_URL}/login/oauth2/code/google` |
| Naver | `{APP_BASE_URL}/login/oauth2/code/naver` |

`deploy/.env`의 `APP_BASE_URL`, `FE_BASE_URL`과 일치해야 합니다. 같은 origin 배포라면 두 값은 같은 origin을 사용합니다.

## 6. 환경 변수 요약

| 변수 | 용도 |
| --- | --- |
| `APP_BASE_URL` | OAuth callback과 `/api/**`를 받을 공개 origin |
| `FE_BASE_URL` | OAuth 성공 후 FE redirect origin |
| `CORS_ALLOWED_ORIGINS` | FE origin (쉼표 구분). 같은 origin 배포도 명시 |
| `RDS_*`, `DB_*` | RDS 연결 |
| `REDIS_HOST` | compose 사용 시 `redis`, 단독 Redis면 host |
| `S3_BUCKET`, `S3_PUBLIC_BASE_URL` | img_url S3 연동 예정 |
| `JWT_SECRET` | JWT 서명 |
| `JPA_DDL_AUTO` | 운영 기본 `validate` |
| `FLYWAY_ENABLED` | 기본 `false`. `true` 시 migration SQL 파일 유지 필요 |

전체 목록: [deploy/env.prod.example](../../deploy/env.prod.example)

## 7. Redis

MVP: `deploy/docker-compose.prod.yml`의 Redis 컨테이너 사용.
트래픽 증가 시 ElastiCache Redis로 교체하고 `REDIS_HOST`만 변경.

## 8. 로컬 vs 운영 차이

| 항목 | local | prod |
| --- | --- | --- |
| DB | Docker MySQL :3307 (`ddl-auto: update`) | RDS + `validate` (Flyway 기본 off) |
| Redis | Docker :6379 | EC2 compose 또는 ElastiCache |
| 이미지 | `uploads/` + `/api/v1/images/**` | private S3 + `/api/v1/images/**` 프록시 |
| Swagger | 활성 | 기본 비활성 |
| Mock auth | 활성 | 비활성 (`@Profile("local")`) |
| Batch runner | local만 | 비활성 |

## 9. 이미지 저장소

1. S3 버킷 생성 후 `deploy/.env`에 `S3_BUCKET` 입력
2. `STORAGE_BACKEND=s3` (prod 기본값)
3. EC2 IAM Role에 S3 권한 부여
4. `S3_PUBLIC_BASE_URL`은 기본적으로 비워둡니다. 이 경우 DB의 `imageUrl`은
   `${APP_BASE_URL}/api/v1/images/{prefix}/{userId}/{filename}` 형식으로 저장되고,
   백엔드가 인증 후 S3 object를 읽어 반환합니다.
5. CloudFront 연동 시에만 `S3_PUBLIC_BASE_URL`에 CloudFront 도메인을 사용합니다.

로컬 개발은 `STORAGE_BACKEND=local`(기본)로 EC2 디스크 + `/api/v1/images/**` 서빙을 유지합니다.

관련 요구사항: `DEPLOY-003`, `DEPLOY-004` — [requirements-definition.md](../requirements/requirements-definition.md)
