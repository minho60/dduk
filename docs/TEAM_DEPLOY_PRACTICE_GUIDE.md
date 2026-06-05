# 뚝 ERP - 팀원용 GitHub Actions & AWS EC2 배포 실습 가이드

이 문서는 팀원들이 개인 GitHub 저장소(Fork)와 개인 AWS EC2 인스턴스를 활용하여, 빌드부터 배포까지의 CI/CD 흐름을 직접 구축하고 실습할 수 있도록 안내하는 단계별 가이드라인입니다.

---

## 🎯 실습 목표
1. 본인 GitHub 계정으로 프로젝트 저장소를 **Fork**합니다.
2. 개인 **AWS EC2 인스턴스**에 Docker 및 배포 환경을 세팅합니다.
3. GitHub Actions **Secrets**를 설정하고 빌드/배포 워크플로우를 직접 실행합니다.
4. 배포된 컨테이너 서비스들이 정상적으로 작동하는지 확인합니다.

---

## 🛠️ Step 1. GitHub 저장소 Fork & 패키지 설정
GitHub Actions에서 Docker 이미지를 빌드한 뒤, 본인 계정의 GitHub Container Registry(GHCR)에 푸시하기 위해 준비하는 과정입니다.

1. **저장소 Fork**
   - 원본 저장소 우측 상단의 **Fork** 버튼을 눌러 본인 개인 계정(예: `github.com/내계정/dduk`)으로 가져옵니다.
2. **GHCR 패키지 접근 권한 확인**
   - 본인 계정에 생성된 Docker 패키지의 Visibility가 기본적으로 Private 상태로 유지될 수 있습니다. 
   - 실습 과정 중 EC2 서버가 본인 계정의 GHCR에서 패키지를 정상적으로 `pull` 할 수 있도록 GitHub 패키지 설정을 확인해야 합니다.

---

## 🔑 Step 2. GitHub Personal Access Token (PAT) 발급
EC2 인스턴스가 GHCR(`ghcr.io`)에 안전하게 로그인하여 이미지를 다운로드받을 수 있도록 읽기 권한을 가진 토큰을 발급받아야 합니다.

1. GitHub 우측 상단 프로필 이미지 클릭 -> **Settings**로 이동합니다.
2. 좌측 하단의 **Developer settings**를 선택합니다.
3. **Personal access tokens** -> **Tokens (classic)**를 클릭합니다.
4. **Generate new token (classic)**을 선택합니다.
5. 토큰 설정:
   - **Note**: `dduk-ec2-read-token` (본인이 알아볼 수 있는 이름)
   - **Expiration**: `No expiration` 또는 원하는 기간 설정
   - **Select scopes (권한)**: **`read:packages`** 체크 (패키지 읽기 권한만 주어 보안을 유지합니다.)
6. **Generate token**을 누르고 생성된 토큰 값(`ghp_...`)을 **반드시 따로 메모장 등에 복사해 둡니다.** (페이지를 벗어나면 다시 확인할 수 없습니다.)

---

## 🖥️ Step 3. AWS EC2 인스턴스 초기 환경 세팅
배포 대상이 될 본인만의 EC2 인스턴스(추천 사양: Ubuntu 22.04 LTS, t2.micro 또는 t3.micro)를 준비하고 배포에 필요한 기본 도구를 설치합니다.

### 1. 보안 그룹(Security Group) 인바운드 규칙 설정
AWS EC2 관리 콘솔로 이동하여, 인스턴스의 보안 그룹에 다음 포트들을 오픈해 줍니다:
* **SSH (22 포트)**: 본인 IP 혹은 전체(0.0.0.0/0)
* **백엔드 웹 UI (8080 포트)**: 전체(0.0.0.0/0)
* **AI 서버 (5000 포트)**: 전체(0.0.0.0/0)
* **RPA 서버 (5050 포트)**: 전체(0.0.0.0/0)

### 2. EC2에 접속하여 패키지 설치
SSH를 통해 EC2 서버에 접속한 후, 아래 명령어를 순서대로 실행하여 **Docker와 Git**을 설치합니다.

```bash
# 1. 패키지 업데이트
sudo apt-get update && sudo apt-get upgrade -y

# 2. Docker 공식 GPG 키 추가 및 저장소 등록
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 3. Docker 엔진 및 Compose 플러그인 설치
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin git

# 4. ubuntu 계정에 docker 실행 권한 부여 (실행 후 SSH 재접속 필요)
sudo usermod -aG docker ubuntu
```
> [!NOTE]
> 권한 설정(`usermod`)을 마친 후 SSH 세션을 종료하고 **다시 재접속**해야 `sudo` 없이 `docker` 명령어를 사용할 수 있습니다.

### 3. 프로젝트 Clone 및 배포 디렉토리 구성
서버 안에서 프로젝트 파일들을 가져옵니다.
```bash
# 1. 홈 디렉토리로 이동 및 Clone
cd /home/ubuntu
git clone https://github.com/내계정/dduk.git

# 2. dduk 폴더로 이동 및 kmh 브랜치 전환
cd dduk
git checkout kmh

# 3. AWS 운영용 환경설정 뼈대 파일 복사
cp .env.aws.example .env.aws
```

> [!IMPORTANT]
> - 복사한 `.env.aws` 파일 내부를 에디터(`nano .env.aws`)로 열어, 실제 DB 연결 정보 및 외부 API Key(Gemini 등) 설정을 알맞게 채워 넣어야 합니다.

---

## 🔒 Step 4. GitHub 저장소 Secrets 등록
Fork한 개인 GitHub 저장소의 `Settings`에서 빌드 및 SSH 접속에 필요한 비밀 변수들을 등록합니다.

* **이동 경로**: Fork한 저장소 상단 탭 `Settings` -> 좌측 메뉴 `Secrets and variables` -> `Actions` -> `New repository secret` 버튼 클릭

> [!CAUTION]
> **중요 (트러블슈팅 방지)**: 
> 비밀값을 복사-붙여넣기 할 때 값의 앞/뒤에 **보이지 않는 공백(Space)**이나 **개행 문자(\r, \n)**가 들어가면 SSH 연결 시 `Permission denied (publickey)` 에러가 발생할 수 있습니다. 
> 값을 입력할 때 공백이 없는지 꼼꼼히 확인해 주세요. (배포 워크플로우에 자체적으로 Trim 처리가 적용되어 있으나 사전 예방이 최선입니다.)

### 필수 등록할 Secrets 7종:

1. **`EC2_HOST`**
   - EC2 인스턴스의 **퍼블릭 IPv4 주소** (예: `3.36.71.14`)
2. **`EC2_USER`**
   - SSH 접속 계정명 (Ubuntu 인스턴스는 기본적으로 `ubuntu`입니다.)
3. **`EC2_PORT`**
   - SSH 포트 번호 (`22`를 입력하거나, Secrets 등록을 아예 생략해도 기본 포트 `22`로 동작합니다.)
4. **`EC2_DEPLOY_PATH`**
   - EC2 서버 내부의 저장소 절대 경로 (`/home/ubuntu/dduk` 입력)
5. **`EC2_SSH_KEY`**
   - EC2 인스턴스 생성 시 다운로드받은 **개인키 pem 파일 내용 전체** (`-----BEGIN RSA PRIVATE KEY-----` 부터 `-----END RSA PRIVATE KEY-----`까지 모두 복사하여 붙여넣기 합니다.)
6. **`GHCR_USERNAME`**
   - 본인의 GitHub 사용자명 (예: `mygithubid`)
7. **`GHCR_READ_TOKEN`**
   - **Step 2**에서 발급받아 두었던 GitHub Personal Access Token (PAT) 값 (`ghp_...` 형태)

---

## 🚀 Step 5. 최초 배포 실행 및 확인
모든 설정이 끝났으므로 GitHub Actions 워크플로우를 트리거해 봅니다.

1. Fork한 저장소 상단 탭의 **Actions**로 이동합니다.
2. 좌측 목록에서 **Deploy kmh to EC2** 워크플로우를 선택합니다.
3. 우측의 **Run workflow** 드롭다운 버튼을 클릭한 뒤, `kmh` 브랜치를 기준으로 **Run workflow**를 실행합니다.
4. 워크플로우 실행 로그를 보며 모든 Step이 초록색(성공)으로 완료되는지 모니터링합니다.

---

## 🔍 Step 6. 서비스 구동 검증
배포 성공 후 브라우저 혹은 EC2 터미널에서 서비스 상태를 최종 점검합니다.

### 1. 브라우저로 접속 확인
* **백엔드 웹 ERP**: `http://내-EC2-IP:8080/index.html`
* **AI 서버 상태**: `http://내-EC2-IP:5000/health`
* **RPA 서버 상태**: `http://내-EC2-IP:5050/health`

### 2. EC2 서버 내부에서 상태 점검
만약 일부 서비스가 구동되지 않거나 오류가 날 경우, EC2 서버에 SSH 접속하여 컨테이너 상태와 로그를 모니터링합니다.
```bash
cd /home/ubuntu/dduk

# 1. 구동 중인 컨테이너 프로세스 리스트 확인
docker compose -f docker-compose.deploy.yml ps

# 2. 특정 컨테이너의 실시간 로그 출력 (예: 백엔드 서버)
docker compose --env-file .env.aws -f docker-compose.deploy.yml logs -f --tail=100 backend
```
