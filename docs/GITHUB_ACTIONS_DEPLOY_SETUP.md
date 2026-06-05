# GitHub Actions Deploy Setup

이 문서는 `kmh` 브랜치에 push 했을 때 EC2로 자동 배포되도록 GitHub 쪽 설정을 설명한다.

자동 배포 대상:

- 브랜치: `kmh`
- 서버 방식: EC2 + Docker Compose
- 서버 주소 기준: `3.36.71.14`
- pem 파일명 기준: `dduk-erp-key.pem`

## 1. 워크플로우 개요

저장소에는 아래 워크플로우가 들어간다.

- `.github/workflows/deploy-kmh.yml`

동작 순서:

1. `kmh` 브랜치에 push
2. GitHub Actions가 EC2로 SSH 접속
3. 서버에서 `git pull --ff-only origin kmh`
4. `docker compose --env-file .env.aws up -d --build`
5. `backend`, `ai-server`, `rpa-server` health 확인

## 2. GitHub에 넣어야 할 Secrets

GitHub 저장소에서 아래 값을 만든다.

경로:

`Settings -> Secrets and variables -> Actions -> New repository secret`

필수 secrets:

### `EC2_HOST`

값:

```text
3.36.71.14
```

### `EC2_USER`

값:

```text
ubuntu
```

주의:

- Ubuntu AMI면 보통 `ubuntu`
- Amazon Linux면 보통 `ec2-user`
- 실제 서버 로그인 계정이 다르면 그 값으로 넣어야 한다

### `EC2_PORT`

값:

```text
22
```

옵션이지만 넣는 걸 권장한다.

### `EC2_DEPLOY_PATH`

값 예시:

```text
/home/ubuntu/dduk
```

설명:

- EC2 안에서 저장소가 실제로 clone 되어 있는 경로
- 워크플로우는 이 경로로 들어가서 `git pull`과 `docker compose`를 실행한다

### `EC2_SSH_KEY`

값:

- `C:\kmh\aws dduk\dduk-erp-key.pem` 파일의 **전체 내용**

방법:

1. `dduk-erp-key.pem` 파일을 텍스트로 연다
2. 아래처럼 시작부터 끝까지 전부 복사한다

```text
-----BEGIN ...
...
-----END ...
```

3. 그 전체 문자열을 GitHub Secret `EC2_SSH_KEY` 값으로 넣는다

주의:

- pem 파일을 저장소에 commit 하면 안 된다
- pem 경로를 secret에 넣는 게 아니라 **파일 내용 자체**를 넣어야 한다

## 3. 서버 쪽 선행 준비

GitHub Actions가 성공하려면 EC2에 이것들이 미리 준비돼 있어야 한다.

- Docker 설치
- Docker Compose plugin 설치
- 저장소 clone 완료
- 배포 경로에 `.env.aws` 존재

예시:

```bash
cd /home/ubuntu
git clone <repo-url> dduk
cd dduk
git checkout kmh
cp .env.aws.example .env.aws
# .env.aws 실제 값 입력
docker compose --env-file .env.aws config
```

## 4. `.env.aws`는 어디서 관리하나

현재 워크플로우는 `.env.aws`를 **EC2 서버 내부 파일**로 유지하는 방식이다.

즉 GitHub Secrets에는 아래만 넣는다.

- 접속용 정보
- 배포 경로
- SSH 개인키

애플리케이션 운영값은 서버의 `.env.aws`가 담당한다.

장점:

- GitHub Secrets에 DB/JWT/Gemini 값을 전부 다시 넣지 않아도 됨
- 현재 `docker compose --env-file .env.aws ...` 구조와 가장 잘 맞음

## 5. 자동 배포가 실제로 하는 명령

워크플로우는 EC2에서 대략 이 명령을 실행한다.

```bash
cd /home/ubuntu/dduk
git fetch origin
git checkout kmh
git pull --ff-only origin kmh
docker compose --env-file .env.aws config
docker compose --env-file .env.aws up -d --build
docker compose --env-file .env.aws ps
```

그 다음 health check:

```bash
curl -fsS http://localhost:8080/index.html >/dev/null
curl -fsS http://localhost:5000/health
curl -fsS http://localhost:5050/health
```

## 6. 첫 실행 전에 수동으로 꼭 확인할 것

- EC2에 `git`, `docker`, `docker compose`가 설치돼 있는지
- `EC2_USER`로 SSH 접속이 실제 되는지
- `EC2_DEPLOY_PATH`가 실제 경로와 맞는지
- `.env.aws` 값이 실제 운영용으로 채워져 있는지
- `docker compose --env-file .env.aws config`가 서버에서 통과하는지

## 7. 배포 테스트 방법

워크플로우 파일이 올라간 뒤에는 아래 둘 중 하나로 테스트한다.

### 방법 1. `kmh` 브랜치에 push

```bash
git push origin kmh
```

### 방법 2. GitHub Actions 수동 실행

- `Actions`
- `Deploy kmh to EC2`
- `Run workflow`

## 8. 실패하면 먼저 볼 곳

### SSH 접속 실패

- `EC2_HOST`
- `EC2_USER`
- `EC2_PORT`
- `EC2_SSH_KEY`

### 서버 경로 오류

- `EC2_DEPLOY_PATH`

### 배포는 됐는데 앱이 안 뜸

- EC2의 `.env.aws`
- `docker compose --env-file .env.aws logs --tail=100 backend`
- `docker compose --env-file .env.aws logs --tail=100 ai-server`
- `docker compose --env-file .env.aws logs --tail=100 rpa-server`

## 9. 보안 주의

- pem 파일 자체는 절대 저장소에 commit 하지 않는다
- `EC2_SSH_KEY`는 GitHub Actions Secret에만 넣는다
- 운영 DB 비밀번호나 JWT secret은 `.env.aws`에서만 관리하고, 필요하지 않으면 GitHub Secrets로 중복 저장하지 않는다
