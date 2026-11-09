---
kind: working
status: active
canonical: mydocs/plans/task_3.md
issue: 3
stage: 1
date: 2026-11-09
---

# Task #3 단계 보고서 — Crashlytics 도입, printStackTrace 치환, 예외 메시지, ApiClient

## 1. 결론

- Crashlytics 18.4.3 도입. 디버그 빌드에서 초기화되고, 서버 다운으로 생긴 네트워크 예외가 non-fatal 로 기록돼 다음 실행 때 `crashlyticsreports-pa.googleapis.com` 으로 전송됐다(HTTP 200).
- **종료 게이트(대시보드 도착)는 사용자 확인 필요.** Crashlytics 설정 응답에 `"firebase_crashlytics_enabled":false` 가 있다 — 콘솔에서 Crashlytics 를 아직 "사용 설정"하지 않은 상태로 보인다. 콘솔 확인 전에는 도착 여부를 판단할 수 없다.
- `printStackTrace()` 앱 소스 66곳 중 65곳을 `ErrorReporter.report()` 로 바꿨다. 남긴 1곳은 `CountryCodePicker.java:481` 의 `printStackTrace(pw)`(디버그 빌드 화면 표시용).
- 커스텀 예외 7종의 메시지에서 `${e.stackTrace}` 를 걷어내고 `cause` 를 넘긴다. 재현 테스트가 수정 전 3건 실패 → 수정 후 통과.
- `ApiClient`: 타임아웃 명시, 로깅은 디버그 BODY / 릴리스 NONE.

## 2. 커밋

| 커밋 | 내용 |
|---|---|
| `d38dddb` | 재현 테스트 `ExceptionMessageTest`, 컴파일 불가 `CompanyAlarmSettingPresenterTest.java` 삭제 |
| (이 보고서와 같은 커밋) | Crashlytics, `ErrorReporter`, 치환, 예외 메시지, `ApiClient`, 계획서 |

## 3. 선행 처리 — 기존 테스트 삭제

master 에서 `./gradlew :app:testDebugUnitTest`:

```
CompanyAlarmSettingPresenterTest.java:42: error: cannot find symbol
        presenter = new CompanyAlarmSettingPresenter();
16 errors
> Task :app:compileDebugUnitTestJavaWithJavac FAILED
BUILD FAILED in 1m 47s
```

삭제된 Presenter 를 참조하는 파일이라 단위 테스트를 하나도 돌릴 수 없었다. 마스터 계획 Stage 5-1(#12)에 "삭제"로 정해진 파일이라 여기서 지웠다. #12 에서는 이 항목을 완료로 본다.

## 4. 재현 테스트 (수정 전)

```
ExceptionMessageTest > message_does_not_contain_stack_trace_array_toString FAILED
ExceptionMessageTest > cause_is_preserved FAILED
ExceptionMessageTest > message_contains_cause_message FAILED
4 tests completed, 3 failed
```

실패 메시지:

```
message_does_not_contain_stack_trace_array_toString :: MovieDetailNotFoundException: 영화 상세정보를 불러오지 못했습니다
[Ljava.lang.StackTraceElement;@f79e
cause_is_preserved :: SubscribeCompanyException expected same:<java.io.IOException: connect timed out> was not:<null>
```

## 5. 변경

| 파일 | 변경 |
|---|---|
| `android/build.gradle` | classpath `firebase-crashlytics-gradle:2.9.9` |
| `android/app/build.gradle` | 플러그인 `com.google.firebase.crashlytics`, `firebase-crashlytics:18.4.3` (기존 messaging 23.2.1·analytics 21.3.0 과 같은 2023년 BoM 시기) |
| `util/ErrorReporter.kt` (신규) | `Log.e("SoonError", …)` + `FirebaseCrashlytics.recordException`. Firebase 미초기화 환경에서는 로그만 |
| 앱 소스 35개 파일 | `X.printStackTrace()` → `ErrorReporter.report(X)` 65곳 |
| `DetailViewModel.kt`, `GenreViewModel.kt`, `CompanyAlarmSettingViewModel.kt` | 메시지 `…: ${e.message}`, `Exception(e)` 로 cause 전달. 구독 해제 예외 문구를 "구독을 해제하는 과정에서"로 수정(구독 예외와 같은 문구였음) |
| `model/api/ApiClient.kt` | connect 10s / read 20s / write 20s / call 30s, 로깅 `BuildConfig.DEBUG` 일 때만 BODY |

`buildFeatures.buildConfig` 는 `gradle.properties` 의 `android.defaults.buildfeatures.buildconfig=true` 로 이미 켜져 있어 손대지 않았다(계획서 §2-3 과 다름).

## 6. 검증

```
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
BUILD SUCCESSFUL in 1m 26s

com.lusle.android.soon.error.ExceptionMessageTest 3 tests, 0 failures, 0 errors
com.lusle.android.soon.ExampleUnitTest 1 tests, 0 failures, 0 errors
app-debug.apk 45,102,770 bytes
```

에뮬레이터 `Pixel_API34`, 운영 서버 다운 상태 그대로, `adb shell setprop log.tag.FirebaseCrashlytics DEBUG`:

```
android --no-metrics run --apks app/build/outputs/apk/debug/app-debug.apk
I/FirebaseCrashlytics: Initializing Firebase Crashlytics 18.4.3 for com.lusle.android.soon
D/FirebaseCrashlytics: Successfully configured exception handler.
D/FirebaseCrashlytics: Loaded settings: {... "collect_logged_exceptions":true,"collect_reports":true, ... "firebase_crashlytics_enabled":false, ... "app":{"status":"activated" ...}
I/okhttp.OkHttpClient: --> GET http://34.229.27.130:3000/api/movie/TMM?region=KR&page=1
I/okhttp.OkHttpClient: <-- HTTP FAILED: java.net.SocketTimeoutException: failed to connect to /34.229.27.130 (port 3000) ... after 10000ms
E/SoonError: java.lang.RuntimeException: java.net.SocketTimeoutException: failed to connect ...
E/SoonError:   at com.lusle.android.soon.model.source.TMMPageKeyDataSource.load(TMMPageKeyDataSource.kt:25)
```

앱 재실행(`am start -n com.lusle.android.soon/.view.main.MainActivity`) 후:

```
D/FirebaseCrashlytics: Sending report through Google DataTransport: 6AF18C2C02B900012495822089035550
D/FirebaseCrashlytics: Crashlytics report successfully enqueued to DataTransport: 6AF18C2C02B900012495822089035550
I/TRuntime.CctTransportBackend: Making request to: https://crashlyticsreports-pa.googleapis.com/v1/firelog/legacy/batchlog
I/TRuntime.CctTransportBackend: Status Code: 200
```

- 연결 타임아웃이 10000ms 로 적용됨(기존 OkHttp 기본값도 10초라 수치는 같고, read/write/call 이 새로 명시됨).
- J1 콜드 스타트: 앱 실행, 하단 내비게이션 5개 표시, `adb logcat | grep -c "FATAL EXCEPTION"` → 0. 본문은 서버 다운으로 빈 화면(기존과 동일).

## 7. 사용자 확인 필요

1. Firebase 콘솔 `soon-79c2e` → Crashlytics 에서 사용 설정 여부 확인, 세션 `6AF18C2C02B900012495822089035550` 의 non-fatal(`SocketTimeoutException`, `TMMPageKeyDataSource.load`)이 보이는지 확인. 보이면 #3 종료 게이트 충족.
2. `android/app/google-services.json` 이 `.gitignore` 에 있지만 **git 에 추적되고 있다**(`git ls-files` 에 나옴, 최초 커밋 `eb26843`). 이 타스크에서는 손대지 않았다. 추적 해제 여부 결정 필요.

## 8. 남은 것

- `ApiClient.retrofit` 이 접근할 때마다 `OkHttpClient`·`Retrofit` 을 새로 만든다(연결 풀 미공유). 범위 밖으로 남김.
- 이미지 로드 실패(Picasso `onError`)도 non-fatal 로 올라간다. 양이 많으면 별도 레벨로 나눌지 판단 필요.
