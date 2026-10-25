---
kind: working
status: active
canonical: mydocs/plans/task_1.md
issue: 1
stage: 2
date: 2026-09-04
---

# Task #1 Stage 2 — 실제 키로 로컬 기동 검증

## 1. 준비

- 작업지시자가 새로 발급한 키를 `server/secrets/firebase-sa.json` 으로 이동(복사본 없음, 권한 600). `git check-ignore` 로 제외 확인. 키 ID `758a5197…`, 계정 `firebase-adminsdk-h7o9r@soon-79c2e`.
- 호스트 node 로 먼저 확인: `getAccessToken()` → `ya29.c…` 1024자 토큰 발급 성공.
- Docker(colima) 데이터 디스크가 98% 라 `docker image prune -f`(태그 없는 이미지만) 로 2.7GB 확보. 다른 프로젝트 컨테이너는 건드리지 않음.

## 2. 기동 중 발견한 범위 밖 문제 2건 — **승인 필요**

검증을 완료하려면 고쳐야 했다. 둘 다 #1 과 무관하게 **현재 상태에서 새로 배포하면 서버가 뜨지 않는** 문제이며, 원인 분석 부록 B §8(호스트 상태 의존 배포)의 실체다.

| # | 증상 | 원인 | 적용한 수정 |
|---|---|---|---|
| 1 | `ReferenceError: File is not defined` (undici) 로 api-server 반복 크래시 | `./:/soon-server-side` bind mount 가 이미지의 `node_modules` 를 호스트 것으로 덮음 | `docker-compose.yml` 에 익명 볼륨 `/soon-server-side/node_modules` 1줄 추가 → 이미지 것을 사용 |
| 2 | 위와 같은 오류가 컨테이너 자체 설치에서도 발생 | `package-lock.json` 이 git 에 없어(`.gitignore`) 매 설치마다 새로 풀림. 현재 `cheerio ^1.0.0-rc.12` → 1.2.0 → `undici 7` 이 Node 20+ 요구. Dockerfile 은 `node:18.17.1`(2025-04 EOL) | Dockerfile `FROM node:20-bookworm-slim` |

거부하면 두 변경을 되돌리고 #2(재구축 런북)로 넘긴다. 단 그 경우 로컬 기동 검증은 불가능하다.

## 3. 검증 결과 (2026-09-04, docker-compose, colima 28.4.0)

```
docker-compose up --build -d
docker-compose ps       → mysql, redis, node-app 모두 Up
```

node-app 로그(오류 필터): `[firebase] 초기화 완료: firebase-adminsdk-h7o9r@soon-79c2e…` ×3 (cron-jobs + api-server 클러스터 2). unhandledRejection, ReferenceError 0건.

| 검증 | 명령 | 결과 |
|---|---|---|
| 헬스체크 | `curl localhost:3000/message/checking/server` | `{"message":"Server is alive!"}` HTTP 200 |
| 토큰 실사용 | `curl localhost:3000/api/alarm/check/fake-token-123` | Firebase IID 응답 `{"error":"InvalidToken"}` → HTTP 400 (가짜 토큰이므로 정상. 액세스 토큰이 유효하지 않았다면 401) |
| 키 제거 후 기동 | `mv secrets/firebase-sa.json …bak && docker-compose up -d node-app` | `[firebase] 서비스 계정 키를 읽지 못했습니다: …/secrets/firebase-sa.json` / `ENOENT` 출력, exit 1. PM2 가 16회 재시작 후 `errored` 로 정지 |
| 복구 | 키 원위치, `docker-compose down -v` | `secrets/` 에 README.md, firebase-sa.json 만 존재 |

## 4. 종료 게이트 대조 (계획 §5)

1. `grep -rn 'adminsdk' server --include=*.js` → 0건 ✔
2. Stage 2 세 검증 명령·출력 기록 ✔ (위 표)
3. 구 저장소 두 개 archived ✔ (작업지시자, 2026-09-04)

## 5. 남은 것

- §2 의 범위 밖 변경 2건 승인 또는 거부
- 기동 초기에 cron 이 MySQL 준비 전 `ECONNREFUSED` 를 한 번 냄(다음 분에 정상). compose `healthcheck`·`depends_on: condition` 은 #2 범위
- PR 생성 (승인 후)
