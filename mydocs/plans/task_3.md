---
kind: canonical
status: active
canonical: mydocs/plans/task_3.md
last_verified: 2026-11-09
---

# 수행계획 — Task #3 Crashlytics 도입과 에러 삼킴 제거 (Stage 3-app)

- **이슈**: [#3](https://github.com/Woochang4862/soon/issues/3)
- **브랜치**: `task_3` (master `dab58fb` 기준)
- **작성일**: 2026-11-09 KST
- **상태**: 작업지시자 사전 승인(2026-11-09 채팅) — 계획서→브랜치→재현 테스트→수정→검증→단계 보고서, push·PR 까지. 머지·콘솔 변경 제외.

## 1. 현재 코드 (문제)

| 위치 | 내용 |
|---|---|
| `android/build.gradle`, `app/build.gradle` | 크래시 수집 SDK 없음 |
| `app/src/main/**` | `printStackTrace()` 68곳 (`androidTest` 1곳 별도). 로그 태그도 없고 원격 수집도 없다 |
| `view/detail/DetailViewModel.kt:14-32`, `view/main/genre/GenreViewModel.kt:30-33`, `view/main/setting/company_alarm/CompanyAlarmSettingViewModel.kt:134-144` | 커스텀 예외 `message` 에 `${e.stackTrace}` — 배열 toString(`[Ljava.lang.StackTraceElement;@…`)이 들어간다. `Subscribe/UnsubscribeCompanyException` 은 `cause` 도 넘기지 않아 원인이 사라진다 |
| `model/api/ApiClient.kt` | 타임아웃 미지정(OkHttp 기본 10초씩), `HttpLoggingInterceptor` 가 릴리스에서도 BASIC |

## 2. 변경 계획

### Stage 1 — 수정 전 재현 테스트

`app/src/test/.../ExceptionMessageTest.kt`: 커스텀 예외 5종의 `message` 에 `StackTraceElement;@` 가 들어가지 않고 원인 메시지를 담는지, `cause` 가 원 예외인지 검사. 수정 전 실패를 기록한다.

단, 기존 `CompanyAlarmSettingPresenterTest.java` 가 삭제된 Presenter 를 참조해 `testDebugUnitTest` 컴파일 자체가 실패하는지 먼저 확인한다. 실패하면 이 파일을 지운다 — 마스터 계획 Stage 5-1(#12)에 이미 "삭제"로 정해진 일이고, 지우지 않으면 어떤 단위 테스트도 돌릴 수 없다. #12 보고서에 이 선행 처리를 적는다.

### Stage 2 — Crashlytics 도입

1. 루트 `build.gradle` classpath 에 `com.google.firebase:firebase-crashlytics-gradle`, 앱에 `com.google.firebase.crashlytics` 플러그인과 `firebase-crashlytics` 의존성. 기존 firebase 라이브러리(2023년판, messaging 23.2.1)와 같은 시기 버전을 고른다. Kotlin 1.7.10 과 충돌하면 그 사실을 보고서에 적고 버전을 낮춘다.
2. `util/ErrorReporter.kt` 신규: `report(e, message?)` = `Log.e` + `FirebaseCrashlytics.recordException`. Firebase 가 초기화되지 않은 환경(JVM 단위 테스트)에서는 로그만 남긴다.
3. `google-services.json` 은 손대지 않는다(이미 `android/app/` 에 있음).

### Stage 3 — printStackTrace 치환, 예외 메시지, ApiClient

1. `e.printStackTrace()` → `ErrorReporter.report(e)` (Kotlin·Java). 예외: `CountryCodePicker.java:480` 의 `printStackTrace(pw)` 는 디버그 화면 표시용이라 유지.
2. 커스텀 예외: `message` 는 고정 문구 + 원인의 `message`, `cause` 로 원 예외 전달.
3. `ApiClient`: connect 10s / read 20s / write 20s / call 30s. 로깅은 `BuildConfig.DEBUG` 일 때 BODY, 아니면 NONE. `buildFeatures.buildConfig true` 필요(AGP 8 기본값 false).

## 3. 검증

| 게이트 | 방법 |
|---|---|
| 빌드 | `./gradlew :app:assembleDebug` |
| 단위 테스트 | `./gradlew :app:testDebugUnitTest` — Stage 1 테스트가 수정 전 실패, 수정 후 통과 |
| 에뮬레이터 | `Pixel_API34` 에 설치·실행. J1 콜드 스타트, logcat 에 Crashlytics 초기화 로그, 서버 다운 상태에서 네트워크 예외가 `ErrorReporter` 태그로 기록되는지, 다음 실행 시 Crashlytics 업로드 로그 |
| 종료 게이트 (대시보드 도착) | **사용자 확인 필요** — Firebase 콘솔 접근이 필요하다 |

## 4. 범위 밖

- `ApiClient.retrofit` 이 접근할 때마다 `OkHttpClient` 를 새로 만드는 구조 (공유 클라이언트로 바꾸는 건 별도)
- 서버 쪽 관측성(#4)
- 각 catch 의 사용자 안내(빈 화면 등) 개선 — 죽은 catch 는 #5
