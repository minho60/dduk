# AWS Deploy Checklist

이 문서는 `kmh` 브랜치를 EC2 한 대에 `docker compose`로 올릴 때 필요한 최소 절차를 고정한다.

## 1. 준비물

- Docker Engine
- Docker Compose plugin
- Java/Python를 직접 설치할 필요는 없음
- TiDB Cloud 또는 MySQL 호환 외부 DB
- 실제 운영값이 채워진 `.env.aws`

## 2. 서버에 둘 파일

- 저장소 전체
- `docker-compose.yml`
- `.env.aws`

`.env.aws`는 루트의 `.env.aws.example`를 복사해서 만든다.

## 3. 꼭 채워야 하는 값

아래 값이 비어 있으면 배포가 정상 완료돼도 앱이 바로 죽거나 일부 기능이 깨진다.

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
RPA_CALLBACK_TOKEN=<백엔드와 RPA가 공유하는 랜덤 문자열>
```

## 4. EC2에서 기동

```bash
git checkout kmh
cp .env.aws.example .env.aws
# .env.aws 실제 값 편집
docker compose --env-file .env.aws config
docker compose --env-file .env.aws up -d --build
docker compose --env-file .env.aws ps
```

`config` 단계에서 변수 누락이나 YAML 오류를 먼저 잡는다.

## 5. 로그 확인

```bash
docker compose --env-file .env.aws logs --tail=100 backend
docker compose --env-file .env.aws logs --tail=100 ai-server
docker compose --env-file .env.aws logs --tail=100 rpa-server
```

아래 조건을 확인한다.

- `backend`가 DB 연결 실패 없이 기동됨
- `ai-server`가 `GEMINI_API_KEY` 오류 없이 기동됨
- `rpa-server`가 포트 `5050`에서 기동됨

## 6. 1차 스모크 체크

EC2에서 실행:

```bash
curl http://localhost:8080/index.html
curl http://localhost:5000/health
curl http://localhost:5050/health
```

기대값:

- backend 정적 페이지 응답
- ai-server `{"status":"UP"}`
- rpa-server `{"status":"UP"}`

## 7. 2차 컨테이너 간 통신 체크

```bash
docker compose --env-file .env.aws exec backend sh -c "wget -qO- http://ai-server:5000/health || curl -fsS http://ai-server:5000/health"
docker compose --env-file .env.aws exec backend sh -c "wget -qO- http://rpa-server:5050/health || curl -fsS http://rpa-server:5050/health"
```

여기서 실패하면 Docker 네트워크나 컨테이너 기동 순서를 먼저 본다.

## 8. 브라우저 체크

브라우저에서 아래 중 하나로 접속:

- `http://<EC2_PUBLIC_IP>:8080`
- `https://<YOUR_DOMAIN>`

체크 포인트:

- 로그인 페이지가 열림
- 로그인 후 대시보드 진입
- 브라우저 콘솔에 CORS 에러 없음
- 관리자/재고 페이지 API 호출이 `localhost:8080`으로 빠지지 않음

## 9. 문제 생기면 먼저 볼 것

### CORS 에러

- `.env.aws`의 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 확인
- 실제 접속 origin과 값이 맞는지 확인

### DB 연결 실패

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- TiDB Cloud IP 허용 정책

### AI 기능 실패

- `GEMINI_API_KEY`
- `GEMINI_MODEL`

### RPA callback 실패

- `RPA_CALLBACK_TOKEN`이 backend/rpa 양쪽에 같은지 확인

## 10. 재배포

```bash
git pull
docker compose --env-file .env.aws up -d --build
```

중지:

```bash
docker compose --env-file .env.aws down
```
