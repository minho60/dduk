# AWS Deploy Checklist

이 문서는 `kmh` 브랜치를 EC2에 `pull-only` 방식으로 배포할 때 필요한 최소 점검 항목을 정리한다.

핵심 전제:

- GitHub Actions가 이미지를 빌드해 GHCR에 push한다.
- EC2는 이미지를 직접 빌드하지 않는다.
- EC2는 `docker-compose.deploy.yml`로 이미지를 pull해서 실행만 한다.

## 1. 준비물

- Docker Engine
- Docker Compose plugin
- GHCR pull 권한이 있는 GitHub 계정과 토큰
- TiDB Cloud 또는 MySQL 호환 외부 DB
- 실제 운영값이 채워진 `.env.aws`

## 2. 서버에 있어야 하는 파일

- 저장소 전체
- [docker-compose.deploy.yml](/C:/kmh/dduk/docker-compose.deploy.yml:1)
- `.env.aws`

`.env.aws`는 루트에서 `.env.aws.example`를 복사해서 만든다.

## 3. 반드시 채워야 하는 값

아래 값이 비어 있으면 컨테이너가 떠도 정상 동작하지 않는다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- `JWT_SECRET`
- `GEMINI_API_KEY`
- `RPA_CALLBACK_TOKEN`

권장 예시:

```env
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://<EC2_PUBLIC_IP>:*,https://<YOUR_DOMAIN>
JWT_SECRET=<64자 이상 랜덤 문자열>
RPA_CALLBACK_TOKEN=<backend와 rpa가 공유하는 랜덤 문자열>
```

## 4. GHCR 로그인

EC2에서 먼저 GHCR pull 로그인이 되어야 한다.

```bash
echo "<GHCR_READ_TOKEN>" | docker login ghcr.io -u "<GHCR_USERNAME>" --password-stdin
```

토큰은 `read:packages` 권한이 있는 GitHub PAT를 사용한다.

## 5. EC2에서 수동 기동

```bash
cd /home/ubuntu/dduk
git checkout kmh
git pull --ff-only origin kmh
docker compose --env-file .env.aws -f docker-compose.deploy.yml config
docker compose --env-file .env.aws -f docker-compose.deploy.yml pull
docker compose --env-file .env.aws -f docker-compose.deploy.yml up -d --force-recreate
docker compose --env-file .env.aws -f docker-compose.deploy.yml ps
```

`config` 단계에서 환경변수 누락이나 compose 문법 오류를 먼저 잡는다.

## 6. 로그 확인

```bash
docker compose --env-file .env.aws -f docker-compose.deploy.yml logs --tail=100 backend
docker compose --env-file .env.aws -f docker-compose.deploy.yml logs --tail=100 ai-server
docker compose --env-file .env.aws -f docker-compose.deploy.yml logs --tail=100 rpa-server
```

확인 포인트:

- `backend`가 DB 연결 실패 없이 기동하는지
- `ai-server`가 `GEMINI_API_KEY` 오류 없이 기동하는지
- `rpa-server`가 포트 `5050`에서 기동하는지

## 7. 1차 스모크 체크

EC2에서 실행:

```bash
curl http://localhost:8080/index.html
curl http://localhost:5000/health
curl http://localhost:5050/health
```

기대값:

- backend 정적 페이지 응답
- ai-server health 응답
- rpa-server health 응답

## 8. 2차 컨테이너 내부 통신 체크

```bash
docker compose --env-file .env.aws -f docker-compose.deploy.yml exec backend sh -c "wget -qO- http://ai-server:5000/health || curl -fsS http://ai-server:5000/health"
docker compose --env-file .env.aws -f docker-compose.deploy.yml exec backend sh -c "wget -qO- http://rpa-server:5050/health || curl -fsS http://rpa-server:5050/health"
```

여기서 실패하면 Docker 네트워크나 서비스 기동 순서를 먼저 본다.

## 9. 브라우저 체크

브라우저에서 아래 중 하나로 접속:

- `http://<EC2_PUBLIC_IP>:8080`
- `https://<YOUR_DOMAIN>`

확인 포인트:

- 로그인 페이지가 열리는지
- 로그인 후 대시보드 진입이 되는지
- 브라우저 콘솔에 CORS 에러가 없는지
- API 요청이 `localhost:8080`이 아니라 실제 배포 주소 기준으로 나가는지

## 10. 문제 생기면 먼저 볼 것

### GHCR pull 실패

- `GHCR_USERNAME`
- `GHCR_READ_TOKEN`
- GHCR package visibility

### CORS 에러

- `.env.aws`의 `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- 실제 접속 origin과 값이 정확히 맞는지

### DB 연결 실패

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- TiDB Cloud allowlist

### AI 기능 실패

- `GEMINI_API_KEY`
- `GEMINI_MODEL`

### RPA callback 실패

- `RPA_CALLBACK_TOKEN`이 backend와 rpa에서 같은지

## 11. 재배포

```bash
git pull --ff-only origin kmh
docker compose --env-file .env.aws -f docker-compose.deploy.yml pull
docker compose --env-file .env.aws -f docker-compose.deploy.yml up -d --force-recreate
```

중지:

```bash
docker compose --env-file .env.aws -f docker-compose.deploy.yml down
```
