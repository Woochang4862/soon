---
kind: working
status: active
canonical: mydocs/plans/soon_recovery_master_plan.md
last_verified: 2026-09-04
---

# GitHub 이슈 초안 — 마스터 계획 Stage 별

작업지시자가 2026-09-04 이슈 등록을 승인했다. 등록 대상 저장소는 **모노레포용 GitHub 저장소**이며 아직 만들지 않았다(이름·공개 여부 결정 대기). 저장소가 생기면 아래 순서로 `gh issue create` 한다. 라벨은 `stage`, `security`, `observability`, `bug`, `test`, `release` 를 먼저 만든다.

| 순서 | 제목 | 라벨 | 본문 요지 |
|---|---|---|---|
| 1 | [Stage 1] 유출된 Firebase·Google 서비스 계정 키 폐기와 코드 정리 | security, stage | 키 3개 폐기(작업지시자 완료 여부 기록), `config/firebase.js` 가 저장소 밖 경로에서 키를 읽도록 수정, 구 공개 저장소 처리(아카이브 또는 이력 정리) |
| 2 | [Stage 2] API 서버 생존 확정과 재구축 런북 | stage | EC2 상태 확인, 재구축 절차 문서화, `restart`·`healthcheck`·`--build` 추가, 탄력적 IP 결정 |
| 3 | [Stage 3-app] Crashlytics 도입과 에러 삼킴 제거 | observability, stage | Crashlytics, `printStackTrace()` 60건 치환, 예외 메시지 수정, OkHttp 타임아웃·로깅 |
| 4 | [Stage 3-srv] 구조화 로그·전역 에러 핸들러·외부 헬스 모니터 | observability, stage | pino + 요청 ID, 에러 핸들러 로깅, UptimeRobot 류 감시, cron 실패 통지 |
| 5 | [Stage 4] 죽은 catch — `try { lifecycleScope.launch{} } catch` | bug | `CompanyListActivity.kt:64`, `SearchActivity.kt:139` |
| 6 | [Stage 4] 월간 알림 cron 의 미선언 변수 ReferenceError | bug | `server/routes/jobs.js:251-265` |
| 7 | [Stage 4] FCM `onNewToken` 이 서버에 토큰을 동기화하지 않음 | bug | `FirebaseInstanceIDService.kt:30` |
| 8 | [Stage 4] 재시도 카운터가 항상 0 으로 리셋 | bug | `CompanyListActivity.kt:76-82` |
| 9 | [Stage 4] `company_id` 문자열 결합 SQL | bug, security | `server/routes/alarm.js`, `server/routes/jobs.js` |
| 10 | [Stage 4] cron 중복 실행 방지 없음, 한 회사 실패가 전체 중단 | bug | `server/routes/jobs.js:26-130` |
| 11 | [Stage 4] Gson non-null 필드의 지연 NPE | bug | `model/schema/Movie.kt` |
| 12 | [Stage 5-app] 단위 테스트·Journey 스모크·GitHub Actions | test, stage | 컴파일 불가 테스트 삭제, testing-setup 스킬, CI |
| 13 | [Stage 5-srv] 라우트 테스트·docker build CI·Dependabot PR 정리 | test, stage | 죽은 test/ 정리, vitest+supertest, CI, dependabot 브랜치 6개 |
| 14 | [Stage 5] 환경 분리 — `BuildConfig.API_BASE_URL`, `.env.example` | stage | flavor 별 서버 주소, 하드코딩 IP 제거 |
| 15 | [Stage 6] targetSdk·Developer Verification·Play 재배포 | release, stage | Play 요구치, 2026-09 인증, 정책 점검, 내부 테스트 트랙 |

각 이슈 본문에는 `mydocs/tech/soon_maintenance_root_cause_analysis.md` 의 해당 절 링크와 종료 게이트(마스터 계획 §4)를 붙인다. 이슈가 번호를 받으면 `plans/task_{issue}.md` 를 만든다.
