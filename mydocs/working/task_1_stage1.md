---
kind: working
status: active
canonical: mydocs/plans/task_1.md
issue: 1
stage: 1
date: 2026-09-04
---

# Task #1 Stage 1 — 서비스 계정 키 로딩 단일화

## 1. 변경

| 파일 | 내용 |
|---|---|
| `server/config/firebase.js` | 재작성. `GOOGLE_APPLICATION_CREDENTIALS` 경로의 JSON 을 읽어 `admin.initializeApp` 을 한 번만 수행. 환경변수 없음·파일 없음·형식 불일치 세 경우 모두 메시지 출력 후 `exit(1)`. `admin` 과 `getAccessToken()` export. 기존 `googleapis` JWT 경로는 제거(사용처 없었음) |
| `server/routes/alarm.js` | `public/*.json` 정적 import 와 `initializeApp` 제거. `getAccessToken()` 3곳 교체 |
| `server/routes/jobs.js` | 동일. `getAccessToken()` 1곳 교체 |
| `server/docker-compose.yml` | `node-app` 에 `env_file: .env`, `./secrets` 읽기 전용 마운트, `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 |
| `server/.env.example` | 신규. 실제 `.env` 의 키 이름과 대조해 작성. 사용처 없는 `FIREBASE_API_KEY` 는 제외 |
| `server/secrets/README.md` | 신규. 키 파일 위치와 발급 경로 안내 |
| `.gitignore` | `server/secrets/*` 제외, README 만 허용 |

동작 변경은 없다. 같은 서비스 계정 자격 증명으로 같은 Admin SDK API 를 호출한다.

## 2. 검증 (2026-09-04, Node v24.16.0, `npm install` 550 패키지)

```
grep -rn 'adminsdk' server --include='*.js'        → 0건
node --check config/firebase.js routes/alarm.js routes/jobs.js   → 모두 OK
```

키 로더의 실패 경로 (`node -e "import('./config/firebase.js')"`):

| 경우 | 출력 | exit |
|---|---|---|
| 환경변수 없음 | `[firebase] GOOGLE_APPLICATION_CREDENTIALS 환경변수가 없습니다.` + 설정 예시 | 1 |
| 파일 없음 | `[firebase] 서비스 계정 키를 읽지 못했습니다: ./secrets/firebase-sa.json` / `ENOENT` | 1 |
| 형식 불일치 (`{"foo":1}`) | `[firebase] 서비스 계정 키 형식이 아닙니다 (client_email/private_key 없음)` | 1 |

형식만 맞는 가짜 키(로컬 생성 RSA, 존재하지 않는 계정):

```
[firebase] 초기화 완료: fake@soon-79c2e.iam.gserviceaccount.com
토큰 발급 실패(가짜 키이므로 정상): Error fetching access token: invalid_grant (Invalid grant: account not found)
```

모듈 그래프: 같은 가짜 키로 `import('./routes/alarm.js')` → router function, `import('./app.js')` → 로드 OK. (`jobs.js` 는 import 즉시 cron 이 시작되므로 로드 검증에서 제외)

gitignore: `server/secrets/x.json` → `server/.gitignore:152:*.json` 및 루트 규칙으로 제외 확인.

## 3. 미실행

- Stage 2 의 `docker-compose up --build` 기동과 헬스체크 200 확인. 실제 서비스 계정 키(`server/secrets/firebase-sa.json`)가 아직 없고, 로컬 Docker(colima) 도 꺼져 있다.
- 실제 키로 `getAccessToken()` 성공 확인.

## 4. 남은 판단

- `googleapis` 패키지는 이제 사용처가 없다. 제거는 #13(의존성 정리)에서 다룬다.
- `FIREBASE_API_KEY` 를 실제 `.env` 에서 지우는 것은 작업지시자가 직접 한다(파일이 저장소 밖).
