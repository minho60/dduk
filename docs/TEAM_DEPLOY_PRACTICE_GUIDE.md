# 뚝 ERP - 팀원용 AWS EC2 & GitHub Actions 통합 배포 실습 가이드

이 문서는 팀원들이 개인 GitHub 저장소(Fork)와 개인 AWS EC2 인스턴스를 활용하여, 기초적인 인프라 세팅부터 최종 CI/CD 배포 완료 및 동작 확인까지 모든 과정을 한 단계씩 따라 할 수 있도록 정리한 통합 실습 가이드라인입니다.

---

## 🎯 전체 실습 흐름 (Milestones)
1. **GitHub Fork**: 원본 저장소를 본인 개인 계정으로 Fork합니다.
2. **GHCR 토큰 발급**: EC2가 빌드된 이미지를 다운로드받을 수 있도록 읽기 전용 토큰(PAT)을 발급받습니다.
3. **AWS EC2 인스턴스 생성**: Ubuntu OS 인스턴스를 생성하고 보안 그룹을 오픈합니다.
4. **EC2 원격 환경 구축**: Docker 및 Git 등 배포에 필요한 기본 패키지를 자동 설치합니다.
5. **소스 복사 및 환경 변수 설정**: 소스를 Clone하고 프로젝트 운영 환경 변수(`.env.aws`)를 입력합니다.
6. **GitHub Secrets 등록**: 배포에 연동될 비밀값들을 설정합니다. (공백/개행문자 입력 차단 주의!)
7. **워크플로우 설정 수정**: 본인의 실습 브랜치명(예: `wooree`)에 맞게 배포 스크립트를 변경합니다.
8. **최초 배포 및 서비스 확인**: 배포 성공 여부와 헬스 체크를 최종 수행합니다.

---

## 🛠️ Step 1. GitHub 저장소 Fork
GitHub Actions가 Docker 이미지를 빌드한 뒤, 본인 계정의 GitHub Container Registry(GHCR)에 안전하게 배포하기 위한 첫 단계입니다.

1. 원본 GitHub 저장소 우측 상단의 **Fork** 버튼을 클릭합니다.
2. 본인 개인 계정(예: `내계정/dduk`)으로 가져옵니다.

---

## 🔑 Step 2. GitHub Personal Access Token (PAT) 발급
EC2 인스턴스가 GHCR(`ghcr.io`)에 로그인하여 이미지를 정상적으로 `pull` 할 수 있도록 돕는 안전한 인증 비밀 토큰입니다.

1. GitHub 홈페이지 우측 상단 프로필 이미지 클릭 ➡️ **Settings**로 이동합니다.
2. 좌측 메뉴 가장 하단의 **Developer settings**를 선택합니다.
3. **Personal access tokens** ➡️ **Tokens (classic)**를 클릭합니다.
4. **Generate new token (classic)**을 누릅니다.
5. 설정값 입력:
   - **Note**: `dduk-ec2-read-token`
   - **Expiration**: `No expiration` 또는 원하는 기간 설정
   - **Select scopes**: **`read:packages`**만 체크 (안전을 위해 패키지 읽기 권한만 부여)
6. **Generate token** 버튼을 누르고 생성된 토큰 값(`ghp_...` 형태)을 **반드시 따로 안전한 곳에 복사해 둡니다.** (페이지를 나가면 다시 볼 수 없습니다.)

---

## 🖥️ Step 3. AWS EC2 인스턴스 생성 및 보안 그룹 설정
클라우드 서버 가상 머신을 띄우고 네트워크 접근 포트를 오픈해 주는 과정입니다.

1. **인스턴스 생성**:
   - AWS Console에 로그인한 뒤 EC2 서비스로 이동하여 인스턴스를 생성합니다.
   - **OS 이미지**: `Ubuntu 22.04 LTS` 권장
   - **인스턴스 유형**: 프리티어 제공 사양인 `t2.micro` 또는 `t3.micro` 권장
2. **보안 그룹(Security Group) 인바운드 규칙 추가**:
   - 생성한 인스턴스의 보안 그룹 설정으로 이동하여 다음 규칙을 추가합니다:
     - **SSH (22 포트)**: 소스 `내 IP` 또는 `위치 무관(0.0.0.0/0)`
     - **백엔드 웹 ERP (8080 포트)**: 소스 `위치 무관(0.0.0.0/0)`
     - **AI 서버 (5000 포트)**: 소스 `위치 무관(0.0.0.0/0)`
     - **RPA 서버 (5050 포트)**: 소스 `위치 무관(0.0.0.0/0)`

---

## ⚙️ Step 4. EC2 원격 환경 구축 (원라인 자동 세팅)
EC2 인스턴스에 접속하여 Docker 엔진, Docker Compose 플러그인, Git을 한 번에 설치하고 필요한 계정 권한까지 세팅하는 일괄 명령어입니다.

1. 본인의 개인키(`.pem`) 파일과 SSH를 이용해 EC2 터미널에 접속합니다.
   ```bash
   ssh -i "내키.pem" ubuntu@<EC2-퍼블릭-IP>
   ```
2. 접속 후 아래의 **명령어 전체를 복사하여 터미널에 통째로 붙여넣고 실행**합니다:
   ```bash
   sudo apt-get update && sudo apt-get upgrade -y && \
   sudo apt-get install -y ca-certificates curl gnupg git && \
   sudo install -m 0755 -d /etc/apt/keyrings && \
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
   sudo chmod a+r /etc/apt/keyrings/docker.gpg && \
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null && \
   sudo apt-get update && \
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin && \
   sudo usermod -aG docker ubuntu && \
   echo "=== Docker 및 기본 패키지 설치 완료! ==="
   ```
3. 설치 완료 후 **반드시 SSH 세션을 종료(`exit`)했다가 다시 재접속**합니다. 그래야 `sudo` 없이 `docker` 명령을 수행할 수 있는 권한이 활성화됩니다.

---

## 📂 Step 5. 프로젝트 Clone 및 환경 변수 설정
배포할 소스 코드를 서버로 복사하고, 데이터베이스 및 외부 API 설정 파일(`.env.aws`)을 완성합니다.

1. EC2 터미널 홈(`/home/ubuntu`)에서 프로젝트를 클론합니다.
   ```bash
   # URL의 <본인-깃허브-계정> 부분을 본인의 계정명으로 변경하여 실행하세요.
   git clone https://github.com/<본인-깃허브-계정>/dduk.git dduk
   
   cd dduk
   
   # develop 브랜치로 전환합니다.
   git checkout develop
   ```
2. 배포 운영용 환경 변수 파일 생성 및 작성:
   ```bash
   # 템플릿 환경 변수 파일 복사
   cp .env.aws.example .env.aws
   
   # 에디터로 편집
   nano .env.aws
   ```
3. 에디터 안에서 아래 실제 운영 설정값들을 입력한 후 저장(`Ctrl + O` -> `Enter` -> `Ctrl + X` 종료)합니다:
   - `DB_URL` (외부 MySQL 호환 DB 주소 및 스키마명)
   - `DB_USERNAME` / `DB_PASSWORD`
   - `APP_CORS_ALLOWED_ORIGIN_PATTERNS` (CORS 패턴: 예: `http://<EC2-퍼블릭-IP>:*,https://내도메인`)
   - `JWT_SECRET` (임의의 64자 이상 랜덤 문자열)
   - `GEMINI_API_KEY` (Gemini API 키)
   - `RPA_CALLBACK_TOKEN` (Backend와 RPA가 공유할 임의의 비밀키 문자열)

---

## 🔒 Step 6. GitHub 저장소 Secrets 등록
GitHub Actions가 빌드 결과를 원격 EC2 인스턴스로 전달할 수 있도록 Fork한 본인의 저장소 Secrets에 7가지 보안 변수를 직접 등록합니다.

* **경로**: Fork한 저장소 상단 탭 **Settings** ➡️ 좌측 메뉴 **Secrets and variables** ➡️ **Actions** ➡️ **New repository secret** 버튼 클릭

> [!CAUTION]
> **⚠️ 윈도우 개행문자(\r) 및 공백 포함 주의 경보**
> Secrets에 pem 키 내용이나 IP 주소 등을 메모장에서 복사-붙여넣기 할 때 값의 끝부분에 보이지 않는 공백(Space)이나 줄바꿈 캐리지 리턴(`\r`), 개행(`\n`)이 들어갈 수 있습니다. 
> 이 경우 SSH 로그인 시 `Permission denied (publickey)` 에러가 발생하여 배포가 중단되므로, 등록할 때 공백이 없는지 꼭 지워주고 입력하십시오.

### 등록할 Secrets 7종 리스트:

1. **`EC2_HOST`**
   - 본인 EC2 인스턴스의 **퍼블릭 IPv4 주소**
2. **`EC2_USER`**
   - SSH 접속 계정명 (`ubuntu`)
3. **`EC2_PORT`**
   - SSH 포트 번호 (`22`)
4. **`EC2_DEPLOY_PATH`**
   - EC2 서버 내부의 프로젝트 절대 경로 (`/home/ubuntu/dduk`)
5. **`EC2_SSH_KEY`**
   - EC2 생성 시 발급받은 **개인키 pem 파일의 텍스트 전체** (`-----BEGIN` 부터 `-----END` 문자열까지 한 글자도 빠짐없이 복사하여 붙여넣기 해야 합니다.)
6. **`GHCR_USERNAME`**
   - 본인의 GitHub 사용자명 (예: `mygithubid`)
7. **`GHCR_READ_TOKEN`**
   - **Step 2**에서 발급받은 GitHub Personal Access Token 클래식 값 (`ghp_...`)

---

## ✏️ Step 7. 워크플로우 설정 수정 및 푸시
팀원 각자가 사용할 실습 브랜치명(예: `wooree` 등)을 워크플로우 감지 브랜치에 맞춤 등록하고 푸시하는 실습 과정입니다.

1. 로컬 개발 환경에서 작업 디렉토리의 **`.github/workflows/deploy-kmh.yml`** 파일을 에디터로 엽니다.
2. `on.push.branches` 설정 아래에 **본인의 실습 브랜치명을 명시적으로 추가**합니다:
   ```yaml
   on:
     push:
       branches:
         - develop
         - <본인-실습-브랜치-이름> (예: wooree)
     workflow_dispatch:
   ```
3. 수정 사항을 저장한 뒤, 본인이 실습할 로컬 브랜치(예: `wooree`)로 커밋하고 푸시합니다.
   ```bash
   git checkout -b wooree
   git add .github/workflows/deploy-kmh.yml
   git commit -m "docs: set custom branch trigger for deployment"
   git push origin wooree
   ```

---

## 🚀 Step 8. 배포 동작 검증
브랜치가 push됨과 동시에 GitHub Actions의 배포 워크플로우가 트리거되며 빌드가 시작됩니다.

1. Fork한 저장소 상단 탭 **Actions**에서 **Deploy kmh to EC2** 워크플로우가 초록색으로 정상 완료되는지 지켜봅니다.
2. 배포 완료 후 브라우저를 띄워 최종 동작 확인을 거칩니다:
   - **백엔드 웹 ERP**: `http://내-EC2-IP:8080/index.html`
   - **AI 서버 상태**: `http://내-EC2-IP:5000/health`
   - **RPA 서버 상태**: `http://내-EC2-IP:5050/health`
3. **배포 문제 시 모니터링 명령어 (EC2)**:
   ```bash
   cd /home/ubuntu/dduk
   
   # 컨테이너 실행 상태 확인
   docker compose -f docker-compose.deploy.yml ps
   
   # 백엔드 기동 로그 실시간 확인 (Ctrl + C로 종료)
   docker compose --env-file .env.aws -f docker-compose.deploy.yml logs -f --tail=100 backend
   ```
