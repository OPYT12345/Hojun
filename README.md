# NFC 수업 시스템 (AI활용 차세대 교육 솔루션)

강의 중 학생 비서 웹 서비스입니다.  
NFC 출석, AI 튜터, 강의 자료 배포, 실시간 Q&A, 과제 제출 기능을 제공합니다.

---

## 기술 스택

- **백엔드:** Spring Boot 3.2, Spring Data JPA, Server-Sent Events
- **프론트엔드:** Vanilla HTML/CSS/JS (별도 빌드 없음)
- **데이터베이스:** MySQL 8.x
- **AI:** OpenAI GPT-4o-mini

---

## 빠른 시작 (Docker — 권장)

Docker만 설치되어 있으면 DB 포함 모든 환경이 자동으로 구성됩니다.

### 사전 요구사항

| 항목 | 버전 |
|------|------|
| Docker Desktop | 최신 버전 |
| Docker Compose | v2 이상 (Docker Desktop에 포함) |

### 실행

```bash
git clone <저장소 URL>
cd login-backend
```

`.env.example` 파일을 복사해 `.env` 파일을 만들고 API 키를 입력합니다:

```bash
cp .env.example .env
```

`.env` 파일을 열어 아래와 같이 수정합니다:

```
OPENAI_API_KEY=발급받은_OpenAI_API_키
```

> AI 기능(AI 튜터, AI 분석)을 사용하지 않는 경우 API 키 없이도 나머지 기능은 정상 동작합니다.

```bash
docker-compose up --build
```

> 첫 실행 시 이미지 빌드 및 의존성 다운로드로 약 3~5분 소요됩니다.  
> 이후 실행은 `docker-compose up` 으로 즉시 시작됩니다.

### 접속

| 페이지 | URL |
|--------|-----|
| 학생 로그인 | https://localhost:8443/student.html |
| 강사 로그인 | https://localhost:8443/instructor.html |
| 회원가입 | https://localhost:8443/signup.html |

> 자체 서명 인증서 경고 → **고급 → localhost(안전하지 않음)으로 이동** 클릭

### 종료

```bash
docker-compose down        # 컨테이너 종료 (데이터 보존)
docker-compose down -v     # 컨테이너 + DB 데이터 완전 삭제
```

---

## 수동 실행 (로컬 환경)

### 사전 요구사항

| 항목 | 버전 |
|------|------|
| Java | 17 이상 |
| Maven | 3.6 이상 |
| MySQL | 8.0 이상 |

### 데이터베이스 초기화

MySQL에 접속하여 데이터베이스 생성:

```sql
CREATE DATABASE IF NOT EXISTS school CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 샘플 계정·강의 데이터는 서버 실행 시 `data.sql` 을 통해 **자동으로 삽입**됩니다.

### 환경변수 설정 (선택)

| 환경변수 | 기본값 | 설명 |
|---------|--------|------|
| `DB_HOST` | `localhost` | MySQL 호스트 |
| `DB_PORT` | `3306` | MySQL 포트 |
| `DB_USERNAME` | `root` | MySQL 사용자 |
| `DB_PASSWORD` | `1234` | MySQL 비밀번호 |
| `OPENAI_API_KEY` | (없음) | AI 기능 사용 시 필요 |

### 서버 실행

```bash
mvn spring-boot:run
```

또는 Windows에서:

```
start.bat 더블클릭
```

---

## 기본 제공 계정

서버 첫 실행 시 아래 계정이 자동으로 생성됩니다.

### 강사 계정
| 항목 | 값 |
|------|-----|
| 이메일 | `sooyeup@gmail.com` |
| 이름 | 홍수엽 |
| 비밀번호 | `school2026` |

### 학생 계정

| 이름 | 이메일 | 비밀번호 |
|------|--------|---------|
| 이호준 | `hojunopyt12345@gmail.com` | `school2026` |
| 홍길동 | `hong@gmail.com` | `school2026` |

### 강사·학생 동시 비교 방법

강사와 학생 화면을 동시에 확인하려면 **시크릿 창(InPrivate)**을 활용하세요.

| 창 | 접속 URL | 계정 |
|----|----------|------|
| 일반 창 | https://localhost:8443/instructor.html | 강사 (`sooyeup@gmail.com`) |
| 시크릿 창 (Ctrl+Shift+N) | https://localhost:8443/student.html | 학생 (`hojunopyt12345@gmail.com`) |

두 창을 나란히 배치하면 강사 화면과 학생 화면을 실시간으로 비교할 수 있습니다.

---

## 주요 기능

- **NFC 출석:** 강의실 자리에 부착된 NFC 태그를 스마트폰으로 찍어 출석 처리, 강사 화면에 실시간 반영
- **강의 자료:** PDF/PPT/영상/링크 등록 및 학생 열람/다운로드
- **AI 튜터:** 학생이 수업 중 모르는 내용을 AI에게 질문 (GPT-4o-mini)
- **AI 분석:** 강사가 제출물·질문 경향 AI 분석 요청
- **실시간 Q&A:** 학생 질문 등록, 강사 답변
- **과제 제출:** 객관식/주관식 문제 출제 및 답안 제출, 결과 실시간 확인

---

## 프로젝트 구조

```
src/main/java/com/example/login/
├── controller/     API 엔드포인트
├── service/        비즈니스 로직
├── entity/         JPA 엔티티 (DB 테이블 자동 생성)
├── repository/     데이터 접근
└── dto/            요청/응답 객체

src/main/resources/
├── static/         HTML 프론트엔드
├── data.sql        서버 시작 시 자동 삽입되는 샘플 데이터
├── init.sql        수동 DB 초기화용 (레거시)
└── application.properties
```
