# DDUK ERP TiDB + Docker 배포 가이드

이 문서는 발표/내부 시연 배포를 기준으로 작성한 가이드입니다.

배포 기준은 아래로 고정합니다.

```text
Database: TiDB
배포 방식: Dockerfile 사용
대상 플랫폼: Render 또는 AWS
```

이 문서는 Dockerfile을 실제로 생성하지 않고, **Render/AWS에서 TiDB와 Dockerfile을 기준으로 어떻게 배포해야 하는지**를 설명합니다.

## 1. 전체 구조

DDUK ERP는 하나의 서버만 실행하는 구조가 아닙니다.

| 구성 | 폴더 | 역할 | 기본 포트 |
| --- | --- | --- | --- |
| Backend | `backend/` | Spring Boot API, 로그인, ERP 데이터 처리, 화면 제공 | `8080` |
| Frontend | `frontend/` | HTML/CSS/Vanilla JS 화면 | Backend에서 같이 제공 |
| AI Server | `ai-server/` | Flask 기반 AI/OCR API | `5000` |
| RPA Server | `rpa/` | Flask + Playwright 기반 자동화 API | `5050` |
| Database | TiDB | ERP 데이터 저장 | `4000` |

배포 시에는 보통 Docker 이미지가 3개 필요합니다.

```text
Backend Docker image
AI Server Docker image
RPA Server Docker image
```

TiDB는 직접 Docker로 띄우기보다 **TiDB Cloud**를 사용하는 것을 권장합니다. 발표/내부 시연 기준에서는 TiDB Cloud가 가장 관리하기 쉽습니다.

## 2. TiDB를 써도 되는가?

사용 가능합니다.

TiDB는 MySQL 프로토콜을 지원하므로 현재 백엔드의 MySQL JDBC 드라이버로 접속할 수 있습니다.

현재 백엔드는 아래 드라이버를 사용합니다.

```text
com.mysql.cj.jdbc.Driver
```

따라서 TiDB 접속 URL도 `jdbc:mysql://...` 형식을 씁니다.

TiDB 접속 예시:

```text
DB_URL=jdbc:mysql://<tidb-host>:4000/dduk_erp?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<tidb-user>
DB_PASSWORD=<tidb-password>
```

주의할 점:

| 항목 | 설명 |
| --- | --- |
| 포트 | TiDB는 보통 `4000`을 사용 |
| SSL | TiDB Cloud는 보통 `useSSL=true` 권장 |
| MySQL 호환성 | 대부분 호환되지만 완전히 같은 DB는 아님 |
| Foreign Key | TiDB 버전이 낮으면 MySQL과 동작 차이가 있을 수 있음 |
| CHECK 제약 | TiDB 설정에 따라 적용 여부가 달라질 수 있음 |
| 초기 시드 | 첫 배포 후 로그 확인 필수 |

발표용 첫 배포에서는 아래 설정을 권장합니다.

```text
JPA_DDL_AUTO=update
SQL_INIT_MODE=always
SQL_INIT_CONTINUE_ON_ERROR=true
ACCOUNTING_SEED=true
```

데이터가 들어간 뒤 재배포할 때는 아래처럼 바꾸는 것이 안전합니다.

```text
SQL_INIT_MODE=never
SQL_INIT_CONTINUE_ON_ERROR=false
```

## 3. 필요한 환경변수

### 3.1 Backend

```text
PORT=8080

DB_URL=jdbc:mysql://<tidb-host>:4000/dduk_erp?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<tidb-user>
DB_PASSWORD=<tidb-password>

JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false

SQL_INIT_MODE=always
SQL_INIT_CONTINUE_ON_ERROR=true
ACCOUNTING_SEED=true

JWT_SECRET=<길고-랜덤한-JWT-시크릿>
JWT_EXPIRATION=86400000

AI_SERVER_URL=<AI 서버 URL>
RPA_SERVER_URL=<RPA 서버 URL>
RPA_CALLBACK_TOKEN=<Backend와 RPA가 같이 쓰는 랜덤 토큰>
```

### 3.2 AI Server

```text
AI_SERVER_HOST=0.0.0.0
AI_SERVER_PORT=5000
BACKEND_API_BASE_URL=<Backend URL>

GEMINI_API_KEY=<Gemini API Key>
GEMINI_MODEL=gemini-1.5-flash
OPENAI_API_KEY=<OpenAI Key를 쓰는 경우>
OPENAI_MODEL=gpt-4o-mini
```

### 3.3 RPA Server

```text
RPA_SERVER_HOST=0.0.0.0
RPA_SERVER_PORT=5050
BACKEND_API_BASE_URL=<Backend URL>/api/v1
RPA_CALLBACK_TOKEN=<Backend와 같은 값>
```

`RPA_CALLBACK_TOKEN`은 Backend와 RPA에서 반드시 같은 값이어야 합니다.

## 4. Dockerfile 기준으로 필요한 파일

Dockerfile을 사용할 경우 보통 아래 파일 구성이 필요합니다.

```text
backend/Dockerfile
ai-server/Dockerfile
rpa/Dockerfile
.dockerignore
```

AWS에서 한 서버에 같이 올릴 계획이면 아래 파일도 있으면 편합니다.

```text
docker-compose.yml
```

다만 Render는 서비스별로 Dockerfile을 각각 지정하는 방식이 더 자연스럽습니다.

## 5. Render에 배포하는 법

Render에서는 서비스 3개를 따로 만듭니다.

```text
dduk-backend
dduk-ai-server
dduk-rpa-server
```

TiDB는 Render 안에 만들지 않고 TiDB Cloud를 연결합니다.

### 5.1 Render 배포 순서

1. TiDB Cloud에서 데이터베이스를 만든다.
2. GitHub에 최신 코드를 push한다.
3. Render에서 AI Server Docker Web Service를 만든다.
4. Render에서 RPA Server Docker Web Service를 만든다.
5. Render에서 Backend Docker Web Service를 만든다.
6. Backend 환경변수에 TiDB/AI/RPA 주소를 넣는다.
7. Backend URL로 접속해서 시연한다.

Backend가 AI/RPA URL을 필요로 하므로 AI/RPA를 먼저 만들고 Backend를 마지막에 만드는 것이 편합니다.

### 5.2 TiDB Cloud 준비

TiDB Cloud에서 아래 정보를 확인합니다.

```text
Host
Port
Database
Username
Password
SSL 필요 여부
```

DB 이름은 가능하면 아래처럼 맞춥니다.

```text
dduk_erp
```

Render Backend에 넣을 DB URL 예시:

```text
jdbc:mysql://<tidb-host>:4000/dduk_erp?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

### 5.3 Render AI Server 만들기

Render에서:

1. `New` 클릭
2. `Web Service` 선택
3. GitHub 저장소 연결
4. Runtime 또는 Environment에서 Docker 선택
5. Root Directory를 아래처럼 지정

```text
ai-server
```

6. Dockerfile 경로를 아래처럼 지정

```text
ai-server/Dockerfile
```

7. 환경변수 입력

```text
AI_SERVER_HOST=0.0.0.0
AI_SERVER_PORT=5000
BACKEND_API_BASE_URL=https://<backend-service>.onrender.com
GEMINI_API_KEY=<실제 키>
GEMINI_MODEL=gemini-1.5-flash
```

Backend URL이 아직 없으면 임시값으로 넣고, Backend 배포 후 다시 수정합니다.

```text
BACKEND_API_BASE_URL=http://localhost:8080
```

### 5.4 Render RPA Server 만들기

Render에서:

1. `New` 클릭
2. `Web Service` 선택
3. GitHub 저장소 연결
4. Docker 선택
5. Root Directory 지정

```text
rpa
```

6. Dockerfile 경로 지정

```text
rpa/Dockerfile
```

7. 환경변수 입력

```text
RPA_SERVER_HOST=0.0.0.0
RPA_SERVER_PORT=5050
BACKEND_API_BASE_URL=https://<backend-service>.onrender.com/api/v1
RPA_CALLBACK_TOKEN=<Backend와 같은 토큰>
```

RPA는 Playwright 의존성이 있으므로 Dockerfile 안에서 브라우저 설치까지 처리되어야 합니다.

### 5.5 Render Backend 만들기

Render에서:

1. `New` 클릭
2. `Web Service` 선택
3. GitHub 저장소 연결
4. Docker 선택
5. Root Directory 지정

```text
backend
```

6. Dockerfile 경로 지정

```text
backend/Dockerfile
```

7. 환경변수 입력

```text
PORT=8080

DB_URL=jdbc:mysql://<tidb-host>:4000/dduk_erp?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<tidb-user>
DB_PASSWORD=<tidb-password>

JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false
SQL_INIT_MODE=always
SQL_INIT_CONTINUE_ON_ERROR=true
ACCOUNTING_SEED=true

JWT_SECRET=<길고-랜덤한-JWT-시크릿>
JWT_EXPIRATION=86400000

AI_SERVER_URL=https://<ai-service>.onrender.com
RPA_SERVER_URL=https://<rpa-service>.onrender.com
RPA_CALLBACK_TOKEN=<RPA와 같은 토큰>
```

8. 배포 완료 후 Backend URL로 접속

```text
https://<backend-service>.onrender.com
```

### 5.6 Render 배포 후 꼭 확인할 것

Render 로그에서 Backend가 정상 시작되는지 확인합니다.

```text
Started ... in ... seconds
```

TiDB 연결 실패가 나면 아래를 확인합니다.

| 증상 | 확인할 것 |
| --- | --- |
| DB 연결 실패 | `DB_URL`, TiDB host, port `4000`, SSL 옵션 |
| Access denied | `DB_USERNAME`, `DB_PASSWORD` |
| Unknown database | TiDB에 `dduk_erp` DB가 있는지 |
| 시드 일부 실패 | `SQL_INIT_CONTINUE_ON_ERROR=true`인지, 로그상 치명 오류인지 |
| AI 호출 실패 | `AI_SERVER_URL` |
| RPA 호출 실패 | `RPA_SERVER_URL`, `RPA_CALLBACK_TOKEN` |

## 6. AWS에 Docker로 배포하는 법

AWS에서는 두 가지 방식이 있습니다.

| 방식 | 설명 | 추천도 |
| --- | --- | --- |
| EC2 + Docker Compose | EC2 한 대에서 컨테이너 3개 실행 | 발표/내부 시연 추천 |
| ECS + ECR | 이미지를 ECR에 올리고 ECS 서비스로 운영 | 운영에 가까움 |

발표/내부 시연은 **EC2 + Docker Compose**가 가장 쉽습니다.

## 7. AWS EC2 + Docker Compose 배포 순서

1. TiDB Cloud를 만든다.
2. EC2 Ubuntu 서버를 만든다.
3. 보안 그룹에서 `22`, `8080`을 연다.
4. EC2에 Docker와 Docker Compose를 설치한다.
5. GitHub에서 프로젝트를 clone한다.
6. `.env`를 작성한다.
7. Docker 이미지를 빌드한다.
8. 컨테이너를 실행한다.
9. `http://<EC2-IP>:8080`으로 접속한다.

## 8. AWS EC2 만들기

AWS Console에서:

```text
EC2 > Launch instance
```

권장값:

```text
AMI: Ubuntu Server 22.04 LTS
Instance type: t3.small 또는 t3.medium
Storage: 20GB 이상
```

RPA까지 시연하면 `t3.medium`을 권장합니다.

## 9. AWS 보안 그룹

Inbound rules:

| Port | 용도 | Source |
| --- | --- | --- |
| `22` | SSH 접속 | 본인 IP |
| `8080` | ERP 접속 | 발표 접속 IP 또는 `0.0.0.0/0` |

외부에 열지 않는 것을 권장:

```text
5000
5050
4000
```

AI/RPA는 EC2 내부 Docker 네트워크로 Backend와 통신하게 두는 것이 좋습니다.

TiDB는 TiDB Cloud에 있으므로 EC2에서 TiDB Cloud로 outbound 접속만 되면 됩니다.

## 10. EC2에 Docker 설치

EC2 접속:

```bash
ssh -i ./dduk-demo.pem ubuntu@<EC2_PUBLIC_IP>
```

Docker 설치:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu
```

설치 후 SSH를 한 번 끊고 다시 접속합니다.

확인:

```bash
docker --version
docker compose version
```

## 11. 프로젝트 내려받기

```bash
cd ~
git clone <GitHub 저장소 URL> dduk
cd dduk
```

## 12. AWS용 `.env`

프로젝트 루트에 `.env`를 만듭니다.

```bash
nano .env
```

예시:

```text
PORT=8080

DB_URL=jdbc:mysql://<tidb-host>:4000/dduk_erp?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<tidb-user>
DB_PASSWORD=<tidb-password>

JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false
SQL_INIT_MODE=always
SQL_INIT_CONTINUE_ON_ERROR=true
ACCOUNTING_SEED=true

JWT_SECRET=<길고-랜덤한-JWT-시크릿>
JWT_EXPIRATION=86400000

AI_SERVER_URL=http://ai-server:5000
RPA_SERVER_URL=http://rpa-server:5050
RPA_CALLBACK_TOKEN=<랜덤 토큰>

AI_SERVER_HOST=0.0.0.0
AI_SERVER_PORT=5000
BACKEND_API_BASE_URL=http://backend:8080/api/v1

RPA_SERVER_HOST=0.0.0.0
RPA_SERVER_PORT=5050

GEMINI_API_KEY=<실제 키>
GEMINI_MODEL=gemini-1.5-flash
```

Docker Compose 내부에서는 서비스 이름으로 통신합니다.

```text
Backend -> AI: http://ai-server:5000
Backend -> RPA: http://rpa-server:5050
RPA -> Backend: http://backend:8080/api/v1
```

브라우저에서 접속하는 주소만 EC2 public IP를 사용합니다.

```text
http://<EC2_PUBLIC_IP>:8080
```

## 13. Docker Compose로 실행

`docker-compose.yml`이 준비되어 있다면 아래처럼 실행합니다.

```bash
docker compose up -d --build
```

로그 확인:

```bash
docker compose logs -f backend
docker compose logs -f ai-server
docker compose logs -f rpa-server
```

컨테이너 상태 확인:

```bash
docker compose ps
```

중지:

```bash
docker compose down
```

다시 배포:

```bash
git pull
docker compose up -d --build
```

## 14. AWS 접속 확인

브라우저:

```text
http://<EC2_PUBLIC_IP>:8080
```

서버 내부:

```bash
curl http://localhost:8080
```

컨테이너 내부 네트워크 확인:

```bash
docker compose exec backend curl http://ai-server:5000/health
docker compose exec backend curl http://rpa-server:5050/health
```

## 15. TiDB 연결 확인

TiDB Cloud 콘솔에서 DB가 생성되어 있는지 확인합니다.

가능하면 TiDB에 접속해 아래를 확인합니다.

```sql
SELECT VERSION();
SHOW DATABASES;
USE dduk_erp;
SHOW TABLES;
SHOW VARIABLES LIKE 'foreign_key_checks';
SHOW VARIABLES LIKE 'tidb_enable_check_constraint';
```

Backend 첫 실행 후 테이블이 생성되어야 합니다.

테이블이 안 생기면 Backend 로그에서 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 먼저 확인합니다.

## 16. Render와 AWS 중 선택

| 기준 | Render + Docker | AWS EC2 + Docker Compose |
| --- | --- | --- |
| 시작 난이도 | 쉬움 | 중간 |
| 서비스 구성 | Web Service 3개 필요 | EC2 1대에서 통합 가능 |
| TiDB 연결 | 환경변수로 연결 | 환경변수로 연결 |
| RPA 안정성 | Render 빌드/런타임 영향 있음 | 직접 제어 가능 |
| 발표 준비 | URL 만들기 빠름 | 서버 설정 후 안정적 |
| 추천 상황 | 빠르게 외부 URL 필요 | AI/RPA 포함 전체 시연 |

추천:

```text
화면과 백엔드 중심 발표: Render + Docker
AI/RPA까지 안정적으로 시연: AWS EC2 + Docker Compose
```

## 17. 발표 전 체크리스트

- [ ] TiDB Cloud DB가 생성되어 있다.
- [ ] TiDB 접속 정보가 준비되어 있다.
- [ ] Dockerfile이 `backend`, `ai-server`, `rpa` 기준으로 준비되어 있다.
- [ ] Render 또는 AWS 환경변수에 TiDB 정보가 들어 있다.
- [ ] `JWT_SECRET`이 비어 있지 않다.
- [ ] `RPA_CALLBACK_TOKEN`이 Backend/RPA에서 같은 값이다.
- [ ] Backend 로그에 DB 연결 성공이 보인다.
- [ ] 첫 배포 후 TiDB에 테이블이 생성되었다.
- [ ] Backend URL로 화면 접속이 된다.
- [ ] AI/RPA를 시연한다면 `/health` 또는 관련 기능을 미리 확인했다.
- [ ] 발표장 네트워크에서 접속 URL이 열린다.

## 18. 최종 추천 배포 흐름

Render로 갈 경우:

```text
TiDB Cloud 생성
GitHub push
Render AI Docker Web Service 생성
Render RPA Docker Web Service 생성
Render Backend Docker Web Service 생성
Backend 환경변수에 TiDB/AI/RPA URL 입력
Backend URL로 발표
```

AWS로 갈 경우:

```text
TiDB Cloud 생성
EC2 생성
Docker 설치
GitHub clone
.env 작성
docker compose up -d --build
http://<EC2-IP>:8080 으로 발표
```
