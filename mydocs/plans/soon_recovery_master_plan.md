---
kind: canonical
status: active
canonical: mydocs/plans/soon_recovery_master_plan.md
last_verified: 2026-09-04
---

# 수행계획 — Soon 서비스 복구·유지보수 체계 재건 마스터 계획

- **근거**: [`tech/soon_maintenance_root_cause_analysis.md`](../tech/soon_maintenance_root_cause_analysis.md)
- **작성일**: 2026-09-04 KST
- **상태**: 작업지시자 승인 대기
- **원칙**: 개별 버그를 고치기 전에 **버그가 보이게 만드는 일**(관측성)을 먼저 한다. 이 순서를 어기면 몇 년간 반복된 "고쳤는데 또 안 된다"가 재현된다.

## 1. 목표

1. 서비스가 죽었는지 개발자가 사용자보다 먼저 안다.
2. 앱 크래시와 서버 에러가 스택·화면·요청 컨텍스트와 함께 자동 수집된다.
3. 변경 하나가 회귀를 만들면 배포 전에 자동으로 잡힌다.
4. 서버를 새 인스턴스에 재구축하는 절차가 문서와 스크립트로 재현된다.

## 2. 범위 밖

- Jetpack Compose 전환, Kotlin/AGP 대규모 업그레이드 (관측성·테스트가 생긴 뒤 별도 타스크)
- 신규 기능
- 서비스 종료 결정 (작업지시자 판단. 종료를 택하면 Stage 1 만 수행하고 Play 안내문으로 마무리)

## 3. 보호 불변식

1. 비밀(키·서명 파일·.env)을 새 위치로 복사하거나 커밋하지 않는다.
2. 운영 서버 IP 를 앱에 하드코딩한 채 새 버전을 올리지 않는다 (Stage 5 전까지 앱 재배포 없음).
3. 외부 행위(콘솔 변경·push·PR·이슈·Play 업로드)는 단계별 승인 뒤 수행한다.
4. 각 단계는 실행 결과(명령·출력)로 종료를 판정한다.

## 4. 단계별 수행

### Stage 1 — 유출 키 폐기와 이력 정리 (srv)

1. Firebase 콘솔 → 프로젝트 `soon-79c2e` → 서비스 계정 → `firebase-adminsdk-h7o9r` 키 2개 삭제, 새 키 1개 발급. **작업지시자가 직접 수행.**
2. GCP 콘솔 → 서비스 계정 `lusle-425@…` 키 삭제.
3. 서버 저장소: `public/*.json` 삭제, `config/firebase.js` 가 `.env` 의 경로 또는 환경변수에서 키를 읽도록 수정, `.gitignore` 에 `public/*.json` 명시.
4. git 이력에서 키 파일 제거 (`git filter-repo`) 후 force push — 공개 저장소라 이력에 남은 키도 유효 위협.
5. 새 키는 서버 파일시스템의 저장소 밖 경로에 둔다.

**종료 게이트**: GitHub raw 로 세 JSON 이 404. 로컬 `docker-compose up` 에서 FCM 토큰 발급 성공 로그.

### Stage 2 — 서버 생존 여부 확정과 복구 (srv)

1. AWS 콘솔에서 인스턴스 상태 확인 (작업지시자). stopped 면 시작, terminated 면 재구축.
2. 재구축 시 필요한 수동 단계를 `mydocs/tech/server_rebuild_runbook.md` 로 남긴다 (현재는 `node_modules` 가 bind mount 로 호스트에 의존해 문서 없이는 재현 불가).
3. 탄력적 IP 할당 여부 결정. 미할당이면 앱 재배포가 매번 필요하다.
4. `docker-compose.yml` 에 `restart: unless-stopped` 와 `healthcheck` 추가. `pull_and_restart.sh` 에 `--build`.

**종료 게이트**: `curl http://<IP>:3000/message/checking/server` 200. 컨테이너 강제 kill 후 자동 재기동 확인.

### Stage 3 — 관측성 (app + srv)

앱:
1. Firebase Crashlytics 도입 (`firebase-crashlytics` + gradle 플러그인). 프로젝트 `soon-79c2e` 는 이미 있다.
2. `printStackTrace()` 60건을 `Crashlytics.recordException` + 로그로 치환. 커스텀 예외의 `${e.stackTrace}` 를 `e.message`/cause 로 수정.
3. `ApiClient` 에 타임아웃·HttpLoggingInterceptor(디버그 빌드) 추가.

서버:
4. `pino` 구조화 로그 + 요청 ID. 전역 에러 핸들러가 스택을 남기도록 수정.
5. 무료 외부 모니터 (UptimeRobot 등) 로 `/message/checking/server` 5분 주기 감시, 실패 시 메일.
6. cron 잡 실패를 같은 채널로 통지.

**종료 게이트**: 디버그 빌드에서 의도적 예외 발생 → Crashlytics 대시보드에 도착. 서버 컨테이너 중지 → 모니터 알림 메일 수신.

### Stage 4 — 조용히 죽어 있던 결함 수정 (app + srv)

관측성이 생긴 뒤에만 착수. 우선순위:

| # | 결함 | 위치 |
|---|---|---|
| 1 | `try { launch{} } catch` 죽은 catch | `CompanyListActivity.kt:64`, `SearchActivity.kt:139` |
| 2 | 월간 알림 cron 의 미선언 변수 `response`/`err` | `routes/jobs.js:251-265` |
| 3 | FCM `onNewToken` 서버 미동기화 | `FirebaseInstanceIDService.kt:30` |
| 4 | 재시도 카운터 항상 리셋 | `CompanyListActivity.kt:76-82` |
| 5 | `company_id` 문자열 결합 SQL | `routes/alarm.js:117,120,156,160,215`, `routes/jobs.js` |
| 6 | cron 중복 실행 방지 없음, 한 회사 실패가 전체 중단 | `routes/jobs.js:26-130` |
| 7 | Gson non-null 필드 NPE 지연 폭발 | `model/schema/Movie.kt` |

각 결함은 별도 이슈·별도 브랜치. 수정 전 재현 Journey 또는 테스트를 먼저 만든다.

### Stage 5 — 회귀 안전망 (app + srv)

1. 앱: 컴파일 안 되는 `CompanyAlarmSettingPresenterTest.java` 삭제, `testing-setup` 스킬로 ViewModel 단위 테스트 + Journey 기반 스모크. GitHub Actions 에서 `assembleDebug` + `testDebugUnitTest`.
2. 서버: 죽은 `test/` 6개 정리, `vitest` 로 라우트 테스트, `supertest` 헬스체크. GitHub Actions 에서 `npm test` + `docker build`.
3. 환경 분리: 앱 `BuildConfig.API_BASE_URL` 을 flavor 별로, 서버는 `.env.example`.
4. Dependabot PR 6건 처리.

**종료 게이트**: 두 저장소 CI green. 일부러 회귀를 넣은 PR 이 CI 에서 실패.

### Stage 6 — 앱 재배포 (app)

1. targetSdk·Kotlin 최소 상향 (Play 요구치 확인). Android Developer Verification(2026-09 시행) 등록 상태 확인.
2. 서버 주소를 도메인 또는 탄력적 IP 로 교체. `play-policy-insights` 스킬로 정책 점검.
3. 내부 테스트 트랙 → 프로덕션.

**종료 게이트**: Play 프로덕션에 새 버전 게시, Crashlytics 에 crash-free 지표 수집 시작.

## 5. 산출물

| 산출물 | 위치 |
|---|---|
| 단계별 계획서 | `mydocs/plans/task_{issue}.md` |
| 단계별 보고서 | `mydocs/working/task_{issue}_stage{N}.md` |
| 서버 재구축 런북 | `mydocs/tech/server_rebuild_runbook.md` |
| 트러블슈팅 | `mydocs/troubleshootings/*.md` |
| 최종 보고서 | `mydocs/report/task_{issue}_report.md` |

## 6. 승인 요청 사항

1. 이 순서(관측성 우선)로 진행하는가, 아니면 서비스 종료를 택하는가.
2. Stage 1 의 콘솔 작업을 작업지시자가 직접 수행할 시점.
3. `mydocs/` 의 git 관리 방식 (오늘할일 참조).
4. 각 Stage 를 GitHub 이슈로 등록하는 것에 대한 승인 (공개 저장소에 글이 남는다).
