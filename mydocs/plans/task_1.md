---
kind: canonical
status: active
canonical: mydocs/plans/task_1.md
last_verified: 2026-09-04
---

# 수행계획 — Task #1 유출된 서비스 계정 키 폐기와 코드 정리

- **이슈**: [#1](https://github.com/Woochang4862/soon/issues/1)
- **브랜치**: `task_1` (master 기준)
- **작성일**: 2026-09-04 KST
- **상태**: 작업지시자 승인 대기
- **선행 완료**: 작업지시자가 Firebase·GCP 콘솔에서 키 삭제 완료 (2026-09-04 보고). 모노레포 이력에서는 키 파일 제거 완료.

## 1. 목표

서버가 저장소 안이 아니라 **저장소 밖 경로**에서 서비스 계정 키를 읽게 만들고, 키 파일이 다시 커밋될 수 없는 구조로 바꾼다. 구 공개 저장소는 읽기 전용으로 보관한다.

## 2. 현재 코드 (문제)

| 위치 | 내용 |
|---|---|
| `server/config/firebase.js:9` | `import('../public/soon-79c2e-…-dc2b66a1c8.json')` — 정적 파일 경로 하드코딩 |
| `server/routes/alarm.js:4,13` | 같은 JSON 을 정적 import, `admin.initializeApp` 호출 |
| `server/routes/jobs.js:4,10` | 같은 JSON 을 정적 import, `admin.initializeApp` 호출 |
| `server/.env` | `FIREBASE_API_KEY`(legacy 서버 키, 사용처 없음) 만 있고 서비스 계정 경로 없음 |
| `server/docker-compose.yml` | 키를 컨테이너에 넣는 방법이 없음 (bind mount `./` 로 통째로 들어감) |

세 파일이 각각 키를 읽고 두 파일이 각각 `initializeApp` 한다. 키 경로를 바꾸려면 세 곳을 고쳐야 한다.

## 3. 변경 계획

### Stage 1 — 키 로딩 단일화 (코드)

1. `server/config/firebase.js` 를 **유일한 진입점**으로 재작성:
   - `process.env.GOOGLE_APPLICATION_CREDENTIALS` 경로의 JSON 을 `fs.readFileSync` 로 읽는다. 없으면 시작 시 명확한 오류로 종료한다 (조용히 실패 금지).
   - `admin.initializeApp({ credential: admin.credential.cert(key), databaseURL })` 을 한 번만 호출하고 `admin` 과 `getAccessToken()` 을 export.
2. `alarm.js`, `jobs.js` 의 정적 JSON import 와 `initializeApp` 을 제거하고 `config/firebase.js` 에서 가져온다. 동작 변경은 없다(같은 자격 증명, 같은 API).
3. `server/.env.example` 추가 — 필요한 변수 전부 이름만 나열. `FIREBASE_API_KEY` 는 사용처가 없으므로 제거 대상으로 표시.
4. `server/docker-compose.yml`: `node-app` 에 `env_file: .env`, `volumes: - ./secrets:/soon-server-side/secrets:ro`, `environment: GOOGLE_APPLICATION_CREDENTIALS=/soon-server-side/secrets/firebase-sa.json`.
5. 루트 `.gitignore` 에 `server/secrets/` 추가. `server/secrets/README.md` 에 "여기에 firebase-sa.json 을 둔다, 커밋 금지" 한 줄.

### Stage 2 — 로컬 검증

작업지시자가 **새로 발급한 키**를 `server/secrets/firebase-sa.json` 에 둔 뒤:

```
cd server && docker-compose up --build
```

- 기동 로그에 Firebase 초기화 성공
- `curl localhost:3000/message/checking/server` → 200
- 키 파일을 치우고 다시 기동 → 명확한 오류 메시지로 종료 (조용한 실패가 아님을 확인)

새 키가 아직 없으면 이 단계는 보류하고 Stage 1 만 병합한다.

### Stage 3 — 구 저장소 보관

작업지시자가 실행:
```
gh repo archive Woochang4862/soon-legacy-android --yes
gh repo archive Woochang4862/soon-legacy-server --yes
```
(README 안내 커밋은 로컬에 준비돼 있고 push 가 먼저 필요하다.)

## 4. 범위 밖

- 로깅·에러 핸들러 개선 (#4)
- `alarm.js`/`jobs.js` 의 다른 결함 (#6, #9, #10)
- 이 타스크는 키를 읽는 경로만 바꾼다.

## 5. 종료 게이트

1. `grep -rn 'adminsdk' server --include=*.js` 결과 0건
2. Stage 2 의 세 검증 결과를 `mydocs/working/task_1_stage2.md` 에 명령·출력으로 기록
3. 구 저장소 두 개가 archived

## 6. 승인 요청

1. 이 계획대로 진행하는가.
2. 새 서비스 계정 키를 발급했는가. 발급했다면 `server/secrets/firebase-sa.json` 에 두어 주면 Stage 2 를 바로 돌린다.
