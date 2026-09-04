---
name: soon-android
description: Soon 안드로이드 앱(com.lusle.android.soon, ~/archive/projects/soon/android)을 유지보수할 때 쓰는 워크플로우 스킬. 빌드, 에뮬레이터 실행, 앱 설치·실행, 화면 검증(layout/screen), 버그 재현과 로그 수집, Journey 회귀 테스트, 공식 Android 스킬 선택까지 `android` CLI 기반으로 진행한다. 사용자가 "Soon 앱 빌드해줘", "에뮬레이터에서 돌려봐", "이 버그 재현해줘", "크래시 로그 봐줘", "화면 확인해줘", "UI 테스트/저니 돌려줘", "AGP 올려줘", "테스트 셋업해줘", "Play 정책 점검" 등 Soon 앱을 실제로 빌드·실행·검증·수정하려는 맥락이면 반드시 이 스킬을 먼저 읽는다. 안드로이드 문서를 찾을 때(WebSearch 대신 `android docs`)도 사용한다.
---

# Soon 앱 유지보수 워크플로우

`android` CLI(Google 공식, 1.0.16251017 기준)를 중심으로 **Explore → Plan → Implement → Verify → Commit** 순서로 일한다.
모든 `android` 호출은 `--no-metrics` 를 붙인다 (사용 통계 전송 차단).

## 1. 프로젝트 사실 (2026-09-04 기준)

| 항목 | 값 |
|---|---|
| 앱 소스 | `~/archive/projects/soon/android` (모듈 `:app` 하나) |
| 패키지 / 런처 | `com.lusle.android.soon` / `.view.main.MainActivity` |
| Play 배포 버전 | 3.10.4 (versionCode 51), 최종 업데이트 2025-02-24 |
| 빌드 스택 | AGP 8.7.3, Gradle 8.9, Kotlin 1.7.10, compileSdk/targetSdk 34, minSdk 28, JDK 17 (`/opt/homebrew/opt/openjdk@17`) |
| 언어 | Kotlin 112 파일 + Java 45 파일 혼재 |
| 서버 주소 | `app/src/main/java/com/lusle/android/soon/model/api/ApiClient.kt:25` 에 `http://34.229.27.130:3000` 하드코딩 |
| 서버 상태 | **다운** (2026-09-04 실측: 모든 포트 무응답). 서버 저장소 `~/archive/projects/soon/server` |
| 크래시 수집 | 없음 (Crashlytics·Sentry 미도입). 버그 근거는 logcat 뿐 |
| 서명 키 | `~/archive/projects/soon/soon-release-key-store.jks` (릴리스 빌드 시에만) |
| 로컬 미커밋 | `master` 가 `origin/master` 보다 2 커밋 뒤 (README 변경만) |

서버가 죽어 있으므로 **앱 단독 실행 시 목록·검색·알림 API는 전부 실패한다.** 네트워크 관련 버그를 재현할 때는 "서버 다운 상황"과 "서버 정상 상황"을 구분해서 기록한다. 서버 정상 상황이 필요하면 `server/` 를 로컬에서 `docker-compose up` 으로 띄우고 `ApiClient.kt` 의 baseUrl 을 에뮬레이터 기준 `http://10.0.2.2:3000` 으로 잠시 바꾼다 (커밋 금지).

## 2. 환경 준비 (한 번만)

```bash
android --no-metrics info                 # sdk 경로 확인: ~/Library/Android/sdk
android --no-metrics sdk list             # platform-tools, platforms;android-34, build-tools;34.0.0, emulator, system-images;android-34;google_apis;arm64-v8a 가 있어야 함
android --no-metrics emulator list        # AVD 없으면 아래로 생성
android --no-metrics emulator create --help
```

`adb` 는 `~/Library/Android/sdk/platform-tools/adb` (PATH 에 이미 들어 있음). Android Studio 는 `/Applications/Android Studio.app`.

## 3. 빌드

```bash
cd ~/archive/projects/soon/android
./gradlew :app:assembleDebug --console=plain 2>&1 | tail -40
# 산출물: app/build/outputs/apk/debug/app-debug.apk
android --no-metrics describe --project_dir .   # 빌드 타깃·APK 경로 JSON 이 필요할 때
```

- 빌드는 오래 걸리니 `run_in_background: true` 로 돌리고 결과를 받는다.
- 빌드 실패 시 컴파일 에러 한 파일만 빨리 보려면 Android Studio 에 프로젝트를 열어둔 상태에서:
  `android --no-metrics studio analyze-file --project Soon app/src/main/java/.../X.kt`
  (Gradle 없이 IntelliJ 인스펙션으로 오류를 잡는다. Studio 가 안 열려 있으면 `android studio check` 로 먼저 확인.)

## 4. 실행

```bash
android --no-metrics emulator start <AVD이름>      # 완전히 부팅될 때까지 기다렸다가 반환 (최대 300초)
android --no-metrics run --apks app/build/outputs/apk/debug/app-debug.apk   # 설치 + 런처 액티비티 자동 감지 + 실행
```

실기기를 쓸 때는 `--device=<serial>` 을 붙인다. `adb devices` 로 serial 확인.

## 5. 화면 검증 (토큰 절약 규칙)

반드시 `~/.claude/skills/android-cli/references/interact.md` 의 규칙을 따른다. 요약:

1. **1차 수단은 `android layout`** (JSON, 상호작용 가능한 요소만). 화면 전체 PNG 보다 훨씬 작다.
2. 화면이 바뀐 뒤에는 `android layout --diff` 로 변화만 본다.
3. WebView·애니메이션·이미지 내용을 봐야 할 때만 `android screen capture --annotate -o <png>` 를 쓰고, **PNG 를 반드시 눈으로 확인**한 뒤 `android screen resolve --screen <png> --string "tap #N"` 으로 좌표를 얻는다.
4. 입력은 `adb shell input tap X Y`, `adb shell input swipe ...`, `adb shell input text "..."` (공백은 `%s`).

```bash
android --no-metrics layout --pretty
adb shell input tap $(android --no-metrics screen resolve --screen s.png --string "#12")
```

## 6. 버그 재현·로그 수집 프로토콜

크래시 수집 SDK 가 없으므로 **재현 → logcat → 스택 확보 → Journey 로 고정** 이 유일한 경로다.

```bash
adb logcat -c                                             # 로그 비우기
adb logcat --pid=$(adb shell pidof -s com.lusle.android.soon) -v time > /tmp/soon.log &   # 앱 프로세스만
# ... 재현 조작 (layout/screen 으로 진행) ...
grep -nE 'FATAL EXCEPTION|AndroidRuntime|Exception' /tmp/soon.log | head
```

- 크래시면 `FATAL EXCEPTION` 이후 스택 전체를 보고서에 붙인다.
- 서버 오류면 Retrofit/OkHttp 로그와 응답 코드를 함께 남긴다. 서버 다운 상황이면 그렇게 명시한다.
- 재현에 성공한 조작 순서는 곧바로 `references/journeys.md` 형식의 Journey XML 로 옮겨 `android/journeys/<이슈명>.xml` 에 저장한다. 수정 후 같은 Journey 를 다시 돌려 회귀를 확인한다.
- 메모리 누수·ANR·jank 가 의심되면 `android-profiler` 스킬을 쓴다.

## 7. Journey 회귀 테스트

`~/.claude/skills/android-cli/references/journeys.md` 프로토콜대로 평가한다. Soon 용 기본 Journey 는 `references/journeys-soon.md` 에 있다. 각 action 을 PASSED/FAILED 로 판정하고, 실행한 adb 명령을 함께 기록한다. 앱이 죽거나 멈추면 그 지점에서 FAILED 로 끝낸다.

## 8. 문서 조회

WebSearch 대신 로컬 Knowledge Base 를 쓴다 (Android, Firebase, Kotlin 공식 문서 4,900건).

```bash
android --no-metrics docs search "retrofit timeout error handling"
android --no-metrics docs fetch kb://android/...
android --no-metrics studio version-lookup <artifact>     # 라이브러리 최신 버전 확인 (Studio 실행 중일 때)
```

## 9. 설치된 공식 스킬과 용도

`~/.claude/skills/` 에 설치돼 있다 (`android skills update` 로 갱신).

| 스킬 | 이 프로젝트에서 쓰는 순간 |
|---|---|
| `android-cli` | CLI 명령 레퍼런스. layout/screen/journey 전에 references 를 읽는다 |
| `testing-setup` | 단위·UI·스크린샷 테스트 인프라 만들 때 (현재 테스트는 사실상 2개뿐) |
| `android-profiler` | 메모리 누수, ANR, 시작 시간, jank 조사 |
| `android-intent-security` | Manifest 의 exported 컴포넌트·딥링크·Intent 처리 점검 |
| `play-policy-insights` | Play 정책(권한, 데이터 안전, 계정) 준수 점검. 스토어 등록 유지에 필요 |
| `r8-analyzer` | 릴리스 빌드 keep 규칙 정리, 앱 크기 축소 |
| `agp-9-upgrade` | AGP 8.7.3 → 9 이전 시. Kotlin 1.7.10 이 먼저 올라가야 하므로 순서 주의 |

Compose 전환(`migrate-xml-views-to-jetpack-compose`)과 edge-to-edge 는 설치하지 않았다. 필요해지면 `android skills add <이름>`.

## 10. 2026-09 이후 주의사항

- **Android Developer Verification**: 2026년 9월부터 인증된 개발자 계정으로 등록된 앱만 인증 기기에 설치된다. Play Console 에서 개발자 인증과 패키지 네임스페이스 등록 상태를 확인한 뒤 배포한다.
- Play 의 targetSdk 요구치가 올라가면 `targetSdk 34` 로는 업데이트 업로드가 막힐 수 있다. 업로드 전 Play Console 경고를 확인한다.
- `android update` 를 주기적으로 실행한다. 설치된 스킬 갱신은 `android skills update`.

## 11. 하지 말 것

- `ApiClient.kt` 의 서버 주소를 바꾼 채 커밋하지 않는다.
- 릴리스 키(`.jks`, `.pem`)와 `google-services.json` 을 옮기거나 커밋 대상에 넣지 않는다.
- `android` 를 `--no-metrics` 없이 반복 호출하지 않는다.
- 화면 확인을 PNG 캡처로 시작하지 않는다. `layout` 먼저.
