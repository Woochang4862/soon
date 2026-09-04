---
kind: investigation
status: active
canonical: mydocs/tech/soon_maintenance_root_cause_analysis.md
last_verified: 2026-09-04
---

# Soon 유지보수·버그 추적 실패 원인 분석

- **질문**: 앱과 서버가 몇 년째 유지보수·버그 트래킹·이슈 해결이 되지 않은 이유
- **방법**: 두 저장소 커밋 이력, GitHub 이슈·PR·Dependabot, Play 스토어 페이지, 서버 실측, 코드 정적 분석(읽기 전용). 빌드·실행은 하지 않았다.
- **날짜**: 2026-09-04
- **후속**: [서비스 복구 마스터 계획](../plans/soon_recovery_master_plan.md)

## 0. 요약 — 한 문장

**버그가 "보이지 않는" 구조**가 근본 원인이다. 크래시·에러·서버 다운을 개발자에게 알려주는 장치가 하나도 없고, 그 위에 에러를 삼키는 코드 패턴, 실행 불가능한 테스트, 재현 불가능한 환경, 기록 없는 프로세스가 겹쳐 있다.

### 0.1 이력 근거

| 항목 | 값 |
|---|---|
| 서버 커밋 수 (연도별) | 2019: 31 · 2020: 19 · 2021: 27 · 2022: 0 · 2023: 24 · 2024: 32 · 2025: 17 |
| 앱 커밋 수 (연도별) | 2019: 12 · 2021: 14 · 2023: 4 · 2024: 3 · 2025: 2 · 2026: 2(README) |
| GitHub 이슈 | 두 저장소 모두 0건 (이슈 트래커를 한 번도 쓰지 않음) |
| PR | 서버에 Dependabot PR 6건, 2020년부터 전부 미처리 |
| CI | 두 저장소 모두 없음 |
| 같은 메시지 반복 커밋 | "FCM 인증 방식 변경" 14회 연속(2025-01), "back up" 5회 |
| 마지막 앱 릴리스 | 2025-02-24 (3.10.4) |
| 서버 상태 | 2026-09-04 실측 전 포트 무응답 |

### 0.2 원인 다섯 층

1. **관측성 0** — 앱 Crashlytics 없음, 서버 로깅 라이브러리·모니터·restart 정책 없음. 사용자가 신고해야만 안다.
2. **에러 삼킴** — 앱 catch 60건이 `printStackTrace()` 뿐. 서버는 전부 `console.log` + 500. 죽은 catch 와 무의미한 예외 메시지.
3. **조용히 죽은 기능** — 월간 알림 cron 이 ReferenceError 로 항상 실패. FCM 토큰 갱신 미동기화.
4. **안전망 0** — 실행되는 자동화 테스트 0개. 있는 테스트는 컴파일·실행 불가.
5. **재현 불가 환경과 기록 없음** — 서버 URL 하드코딩, 호스트 상태 의존 배포, 런북 없음, 이슈 0건.

### 0.3 보안 사고 (즉시 조치)

서버 저장소 `public/` 에 Firebase Admin SDK 서비스 계정 키 2개와 Google API 서비스 계정 키 1개가 커밋돼 있고, 공개 저장소라 GitHub raw 로 HTTP 200 으로 받아진다(2026-09-04 실측). `.gitignore` 의 `*.json` 은 이미 tracked 된 파일에 소급되지 않아 무력했다. 조치는 마스터 계획 Stage 1.

---

# 부록 A. 앱(Soon) 코드 분석 원문

> 읽기 전용 정적 분석. 근거는 `android/` 기준 상대 경로.
## Soon 안드로이드 앱 — 버그 발견·재현·수정이 어려운 구조적 원인 분석

읽기 전용 코드 분석 결과입니다. (`/Users/jeong-uchang/archive/projects/soon/android`, build 폴더 제외 소스 기준)

---

### 1. 크래시/에러 리포팅 — 수집 인프라 자체가 없음

**발견**: Crashlytics·Sentry·Bugsnag 등 크래시 수집 SDK가 전혀 없다. Firebase 관련 의존성은 messaging과 analytics뿐이다.

**근거**:
- `app/build.gradle:44-45` — `firebase-messaging:23.2.1`, `firebase-analytics:21.3.0`만 존재. `firebase-crashlytics`, `com.google.firebase.crashlytics` 플러그인 없음.
- `build.gradle:14-18` (buildscript classpath) — crashlytics-gradle 플러그인 classpath 없음.
- `App.kt:1-12` (Application 클래스) — `Stetho.initializeWithDefaults`, `JodaTimeAndroid.init`만 호출. `Thread.setDefaultUncaughtExceptionHandler` 등 전역 예외 핸들러도 없음.

**영향**: 실기기에서 발생한 크래시/논팻탈 예외가 개발자에게 전혀 보고되지 않는다. Play Console의 자동 네이티브 크래시 집계 외에는 어떤 정보도 없어, "언제·어느 화면·어떤 스택트레이스"로 죽었는지 사후에 알 방법이 없다. 이 항목이 없으면 아래 다른 모든 문제의 우선순위도 매길 수 없다.

---

### 2. 네트워크 계층 — 하드코딩 IP, cleartext 허용, 타임아웃/재시도 미설정

**발견**:
- base URL이 소스에 raw IP:port로 하드코딩.
- cleartext(HTTP 평문) 트래픽 전역 허용.
- OkHttpClient에 커스텀 타임아웃/재시도 설정 없음(OkHttp 기본값 10초만 적용).
- 서버 에러 시 사용자에게 보이는 화면은 위치마다 제각각(AlertDialog, Snackbar, 빈 리스트).

**근거**:
- `app/src/main/java/com/lusle/android/soon/model/api/ApiClient.kt:25` — `.baseUrl("http://34.229.27.130:3000")`
- `app/src/main/res/xml/network_security_config.xml` — `<base-config cleartextTrafficPermitted="true" />`
- `ApiClient.kt:18-23` — `OkHttpClient.Builder()`에 `connectTimeout/readTimeout/writeTimeout` 호출 없음.
- `app/src/main/java/com/lusle/android/soon/view/company_list/CompanyListActivity.kt:64-88` — 서버 에러 시 AlertDialog로 "다시 시도" 버튼 제공하지만, 재시도 로직 버그 있음(3번 참조).

**영향**: 서버가 완전히 다운돼도, 느려도, JSON이 깨져도 클라이언트 입장에선 전부 "동일한 예외"로 도착한다(아래 3, 8번과 결합). 환경별(dev/staging/prod) URL 분리가 없어 디버그 빌드로 다른 서버를 가리켜 재현하는 것도 불가능 — 항상 운영 서버에 직접 붙어서 디버깅해야 한다.

---

### 3. 에러 처리 패턴 — 대부분 `printStackTrace()`로 삼켜짐 + 구조적 버그

**발견 A (일괄 무시)**: `catch` 블록 40여 곳 중 절대다수가 `e.printStackTrace()` 한 줄만 실행하고 끝난다. 릴리즈 빌드에서는 이 출력이 어디에도 남지 않는다(1번과 결합하면 완전히 증발).

**근거**: `grep -rn "printStackTrace"` 60건 (예: `model/source/MoviePageKeyDataSource.kt:44`, `view/detail/DetailActivity.kt:276,279,283,286`, `view/main/company/CompanyFragment.kt:122,160,219,266` 등).

**발견 B (구조적 버그 — 코루틴 예외를 못 잡는 try/catch)**: 똑같이 생긴 코드 패턴인데 어떤 곳은 `try` 안에 `lifecycleScope.launch{}`를 넣어 실제로는 launch 블록 내부에서 던져지는 예외를 **catch 하지 못한다**. 반면 다른 파일들은 올바르게 `launch{}` 안에 `try`를 둔다.

**근거(잘못된 패턴 — catch가 사실상 죽은 코드)**:
- `view/company_list/CompanyListActivity.kt:64-71`
  ```kotlin
  try {
      lifecycleScope.launch {
          viewModel.getFlow(query).collectLatest { ... }
      }
  } catch (e:Exception){ ... }
  ```
- `view/search/SearchActivity.kt:139-152` — 동일 패턴.

**근거(올바른 패턴 — 대조군)**:
- `view/main/this_month_movie/ThisMonthMovieFragment.kt:114-126`
- `view/movie_list/MovieListFragment.kt:132-144`
- `view/main/genre/GenreFragment.kt:99-102`

**발견 C (스택트레이스 메시지가 무의미함)**: 커스텀 예외의 `message` getter가 `e.stackTrace`(배열 타입)를 문자열 템플릿에 그대로 넣어, 실제로는 `[Ljava.lang.StackTraceElement;@xxxxx` 같은 해시값만 찍힌다.

**근거**: `view/detail/DetailViewModel.kt:14-32` (`MovieDetailNotFoundException` 등 4개 클래스 전부 `${e.stackTrace}` 사용), `view/main/setting/company_alarm/CompanyAlarmSettingViewModel.kt:136,142,153,158`.

**발견 D (재시도 카운터 버그)**: `retryNumber` 증가 직후 무조건 0으로 리셋 — `MAX_RETRY_NUM` 체크가 사실상 무력화됨.

**근거**: `view/company_list/CompanyListActivity.kt:76-82`
```kotlin
if (retryNumber <= MAX_RETRY_NUM) {
    retryNumber += 1
    load(query)
}
retryNumber = 0   // 조건 안팎 상관없이 항상 리셋
```

**발견 E (응답 성공여부 미확인)**: FCM 구독취소 콜백의 `onResponse`가 빈 바디 — 서버가 4xx/5xx를 반환해도 클라이언트는 성공으로 간주.

**근거**: `view/alarm/AllCompanyAlarmRemoveService.kt:43-48`
```kotlin
.enqueue(object : Callback<ResponseBody?> {
    override fun onResponse(...) {}   // isSuccessful 체크 없음
    override fun onFailure(...) { t.printStackTrace() }
})
```

**영향**: 개발자가 "에러 처리를 해뒀다"고 믿는 코드의 상당수가 실제로는 예외를 못 잡거나(B), 잡아도 정보가 없거나(A, C), 재시도가 논리적으로 의미 없다(D). 같은 모양의 코드가 어떤 화면에서는 동작하고 어떤 화면에서는 동작하지 않아, 재현 시도할 때마다 "왜 여기선 되는데 저기선 안 되지"라는 혼란이 반복될 수밖에 없는 구조다.

---

### 4. 아키텍처 일관성 — MVP 잔재, README와 실제 구현 불일치

**발견**: Kotlin 112 / Java 45 파일 혼재. `ViewModel`은 18개 있지만 `Presenter` 클래스는 main 소스에 **하나도 없다**. 그런데 테스트 파일 하나가 존재하지 않는 Presenter 계열 클래스들을 import한다.

**근거**:
- `app/src/test/java/com/lusle/android/soon/CompanyAlarmSettingPresenterTest.java:5-6,17-23` — `com.lusle.android.soon.view.main.setting.company_alarm.Presenter.CompanyAlarmSettingContractor`, `CompanyAlarmSettingPresenter`, `SubscribeCheckDataRemoteSource`, `CompanyAlarmManagerRemoteSource`, `FavoriteCompanyDataLocalSource`, `CompanyAlarmSettingAdapterContract`를 import.
- `find app/src/main -iname "CompanyAlarmSettingPresenter*"` 등 6개 클래스명 전부 검색 결과 0건 — **main 소스 어디에도 존재하지 않음**. 현재 구현은 `view/main/setting/company_alarm/CompanyAlarmSettingViewModel.kt`(순수 MVVM, ViewModel + Repository).
- **결론**: 이 테스트 파일은 컴파일이 되지 않는다(참조 클래스가 삭제됨). MVP → MVVM 전환 시 테스트가 갱신/삭제되지 않고 방치된 것으로 보인다.

**발견 (README ↔ 실제 구현 불일치)**: README는 기술 스택에 "Room"을 명시하지만, 실제로는 Room 의존성도 `@Entity/@Dao/@Database` 사용도 전혀 없다. 로컬 저장은 순수 SharedPreferences + Gson 수동 직렬화다.

**근거**: `README.md` 기술스택 배지("Library-Room"), `grep -n room app/build.gradle` → 결과 없음, `model/source/FavoriteCompanyRepository.kt:12` — `context.getSharedPreferences("pref", MODE_PRIVATE)`.

**발견 (커밋 이력)**: 의미 없는 커밋 메시지가 다수("back up" 5회: `5faf029`, `228430f`, `c40e138`, `1ba8d32`, `273a6ad`), 이슈 트래커 연동 없음.

**영향**: 문서를 믿고 코드를 찾으면 존재하지 않는 아키텍처(Room, Presenter)를 찾아 헤매게 된다. 컴파일 안 되는 테스트가 방치돼 있다는 것 자체가 "테스트를 안 돌린 지 오래됐다"는 강한 방증이다.

---

### 5. 테스트 — 사실상 전무, 그나마 있는 것도 컴파일 불가

**발견**:
- `app/src/test/java/com/lusle/android/soon/ExampleUnitTest.java` — Android Studio 기본 생성 보일러플레이트(`assertEquals(4, 2+2)`), 한 줄도 수정 안 됨.
- `app/src/test/.../CompanyAlarmSettingPresenterTest.java` — 위 4번 항목에서 확인했듯 참조 클래스가 삭제되어 **컴파일 불가**.
- `app/src/androidTest/.../ExampleInstrumentedTest.java` — 역시 보일러플레이트(패키지명 assert만).
- `app/build.gradle:53` — `mockito-core:1.10.19`(2014년경 버전, 현재 코드 구조와 무관).

**영향**: 130개+ 소스 파일 중 실제로 동작하는 자동화 테스트는 0개다. 어떤 리팩터/의존성 업그레이드도 회귀를 잡아줄 안전망이 없어, 매 변경이 곧바로 운영 서버·실사용자 대상 수동 테스트가 된다.

---

### 6. 빌드 환경 — Kotlin/AGP 버전 격차, 미사용 의존성

**발견**: Kotlin 1.7.10(2022.7) + AGP 8.7.3(2024~2025) + compileSdk/targetSdk 34 조합.

**근거**: `build.gradle:6` (`kotlin_version = '1.7.10'`), `build.gradle:14` (`com.android.tools.build:gradle:8.7.3`), `app/build.gradle:9,12` (`compileSdk 34`, `targetSdk 34`).

**흥미로운 불일치**: 커밋 `1115594`의 메시지는 "Uprade to AGP 8.9"이지만 실제 diff는 `7.4.2 → 8.7.3`으로 변경했다 — 커밋 메시지 자체가 실제 변경 내용과 다르다(사소하지만 "커밋 로그를 못 믿는다"는 근거).

**추정(실행하지 않고 판단만)**: Kotlin 1.7.10은 AGP 8.7.x 세대보다 2년 이상 오래됐고, 함께 딸려온 `navigation-compose:2.4.0-beta01`(`app/build.gradle:93`)은 2022년 베타 버전인데 `buildFeatures { compose true }`나 `composeOptions`가 선언되어 있지 않아(app/build.gradle:31-34 buildFeatures 블록에 viewBinding만 있음) 실제로 Compose를 쓰지 않으면서 의존성만 얹혀 있는 것으로 보인다(`@Composable` 사용처 검색 결과 0건). 빌드가 지금 실제로 되는지는 실행하지 않아 판단하지 않았으나, `app/build/intermediates`가 2025-09-29자로 존재해 최근 한 시점엔 빌드 성공 이력이 있다.

**영향**: 오래된 서드파티(Kotlin/의존성) 버전 조합은 IDE/빌드 경고, lint 미스매치, 의존성 해석 실패 가능성을 늘려 "내 컴퓨터에선 되는데" 류 문제의 온상이 되기 쉽다.

---

### 7. 설정 하드코딩

**발견**:
- 서버 base URL이 소스에 IP:포트 그대로 하드코딩, BuildConfig/gradle property/product flavor 분리 없음 → `ApiClient.kt:25` (2번과 동일 근거).
- TMDB API 키는 **클라이언트에는 없음** — 이미지 URL(`https://image.tmdb.org/t/p/w500...`)만 직접 참조하고(예: `adapter/holder/MovieViewHolder.kt:34`), 실제 TMDB API 호출은 자체 서버(`34.229.27.130:3000`)가 대행한다(`MovieApi.kt:19-44`의 엔드포인트가 전부 자체 서버 경로). 따라서 TMDB 키 유출 위험은 없지만, TMDB 연동 로직 자체가 이 저장소 밖(서버)에 있어 그쪽 문제는 클라이언트 코드만 봐서는 전혀 진단할 수 없다.
- AdMob 앱 ID는 매니페스트에 하드코딩(`AndroidManifest.xml` `com.google.android.gms.ads.APPLICATION_ID` = `ca-app-pub-2329923322434251~4419072683`) — 이건 AdMob 표준 방식이라 문제라 보기 어려움.
- `google-services.json`이 git에 커밋돼 있음(`git ls-files`로 확인) — Firebase 앱에서는 흔한 관행이라 치명적이진 않으나 참고 사항.

**영향**: 서버 주소를 바꾸려면 소스 수정+재빌드가 필요하고, dev/QA 환경 분리가 안 돼 있어 "운영 서버를 건드리지 않고 재현/실험"이 구조적으로 불가능하다.

---

### 8. 외부 의존 — 서버 응답 스키마에 강결합된 파싱

**발견**: `Movie` 데이터 클래스의 필드 대부분이 non-null 타입(`String`, `Boolean`, `Double`, `List<Int>`)으로 선언돼 있는데, Gson은 리플렉션으로 역직렬화하면서 Kotlin의 null-safety를 강제하지 않는다. 서버가 필드를 누락하거나 필드명을 바꾸거나 null을 보내면, 파싱 시점에는 조용히 통과하고 그 필드를 나중에 사용하는 지점(문자열 연결, `.toString()` 등)에서야 NPE로 터진다.

**근거**: `model/schema/Movie.kt:1-56` — `title: String`, `originalLanguage: String`, `releaseDate: String`, `genreIds: List<Int>` 등이 `@SerializedName`만 있고 기본값/nullable 처리 없음(반면 `posterPath`, `backdropPath`, `overview`는 `String?`로 이미 nullable 처리돼 있어, 작성자가 "서버가 null을 줄 수 있다"는 걸 일부 필드에서는 인지했지만 나머지 필드에서는 놓쳤다는 정황).

**근거(엔드포인트 강결합)**: `model/api/MovieApi.kt:19-44` — `/api/movie/company`, `/api/movie/genre`, `/api/movie/date`, `/api/search/movie`, `/api/movie/detail`, `/api/movie/TMM`, `/api/movie/watch/providers`, `/api/movie/credits`, `/api/movie/similar` 등 9개 엔드포인트 전부 자체 서버(이 저장소 밖)와 경로/쿼리파라미터로 결합. 서버 쪽 응답 필드 하나만 바뀌어도 이 클라이언트는 컴파일 경고 없이 런타임에만 깨진다.

**추정**: 다른 스키마 클래스(`MovieDetail.kt`, `CreditsResult.kt`, `WatchProviderResult.kt` 등)도 같은 `@SerializedName` + data class 패턴을 쓰고 있어(직접 전수 검사는 안 했으나 파일명·패턴 일관성으로 볼 때) 동일한 위험을 공유할 가능성이 높다 — 이 부분은 추정이다.

**영향**: 서버 팀(=1인 개발자 본인)이 서버 응답 필드를 하나 바꾸면, 클라이언트는 컴파일도 되고 파싱도 성공한 것처럼 보이다가 화면을 스크롤하거나 특정 상세페이지에 들어갈 때만 지연 크래시가 난다 — "어느 커밋이 원인인지" 역추적하기 가장 어려운 유형의 버그다.

---

### 9. FCM 처리 — 토큰 갱신이 서버에 동기화되지 않음

**발견**: `onNewToken` 콜백이 로그만 남기고 서버에 새 토큰을 보내지 않는다. 반면 회사별 알림 구독/해제는 매번 `FirebaseMessaging.getInstance().token`을 새로 가져와 `token+company_id` 쌍으로 서버에 등록하는 구조다.

**근거**:
- `view/alarm/FirebaseInstanceIDService.kt:30-33`
  ```kotlin
  override fun onNewToken(s: String) {
      super.onNewToken(s)
      Log.d("Firebase", "FirebaseInstanceIDService : $s")   // 서버 전송 없음
  }
  ```
- `view/main/setting/company_alarm/CompanyAlarmSettingViewModel.kt:76-110` — `subscribeCompanyAlarm`/`unsubscribeCompanyAlarm`이 `token.value`(설정 화면 진입 시점에 가져온 토큰)를 그대로 서버에 전송.
- `view/alarm/AllCompanyAlarmRemoveService.kt:32-50` — 전체 알림 해제 시에도 그 시점 토큰만 사용, 실패는 `printStackTrace()`만.

**추정(서버 코드는 이 저장소에 없어 추정)**: FCM 토큰이 백그라운드에서 회전(재설치, 앱 데이터 삭제, 주기적 갱신 등)하면, 서버에는 예전 토큰으로 구독이 남아 있는데 그 토큰으로는 더 이상 푸시가 도달하지 않는다. 사용자가 다시 "회사 알림 설정" 화면을 열어 토글을 재저장하기 전까지는 복구되지 않고, 화면상 어떤 표시도 없어 사용자가 재진입할 이유도 없다.

**영향**: "알림을 설정했는데 안 온다"는 신고가 들어와도, 개발자 본인 기기에서는 토큰이 잘 안 바뀌므로 재현이 거의 불가능한 유형의 버그를 만든다.

---

### 10. 문서/이슈 — README 외 전무

**발견**: `CHANGELOG`, `docs/`, 아키텍처 문서, 알려진 이슈 목록, GitHub Issue 템플릿 등 어떤 것도 저장소에 없다. `README.md`는 기능 소개/스토어 링크/기술 스택 배지 위주의 마케팅성 문서이며, 알려진 버그·제약사항 섹션이 없다.

**근거**: `find ... -iname "CHANGELOG*" -o -iname "*.md"` → `README.md` 1건만 검출. `.github/` 디렉토리 자체가 없음(CI 워크플로우도 없음 — `find -iname "*.yml"` 결과 0건).

**영향**: 과거에 어떤 문제를 어떻게 고쳤는지, 어떤 트레이드오프를 감수했는지에 대한 기록이 전혀 없어, 시간이 지난 뒤 본인이 다시 봐도 왜 이렇게 짜여 있는지 알 수 없다. CI가 없으므로 5번(테스트)의 컴파일 실패조차 자동으로 감지되지 않았을 것이다.

---

### 가장 치명적인 상위 5개

1. **크래시 리포팅 SDK 부재 + 대부분의 catch가 `printStackTrace()`뿐** — 운영 중인 실사용자 기기에서 무엇이 왜 깨지는지 개발자가 원천적으로 볼 수 없다. 이게 없으면 나머지 문제들의 우선순위도 매길 수 없는, 근본 원인 중의 근본 원인.
2. **`try { lifecycleScope.launch{} } catch` 구조적 버그** (`CompanyListActivity.kt:64-71`, `SearchActivity.kt:139-152`) — 겉보기엔 에러 처리가 있는데 실제로는 코루틴 내부 예외를 못 잡아 앱이 그냥 죽는다. 똑같이 생긴 코드가 다른 파일에서는 정상 동작해 혼란을 가중시킨다.
3. **네트워크 에러가 전부 `*NotFoundException`으로 뭉개짐 + 메시지에 스택트레이스 배열 toString()만 찍힘** (`DetailViewModel.kt:14-32`) — 서버 다운, 타임아웃, 실제 데이터 없음이 클라이언트/로그 어디서도 구분되지 않는다.
4. **실행 가능한 자동화 테스트 0개, 그나마 있던 테스트는 삭제된 MVP 클래스를 참조해 컴파일조차 안 됨** (`CompanyAlarmSettingPresenterTest.java`) — 회귀를 잡아줄 안전망이 전혀 없다.
5. **FCM 토큰 갱신이 서버에 동기화되지 않음** (`FirebaseInstanceIDService.kt:30-33`) — "알림이 안 온다"는, 개발자 본인 환경에서는 거의 재현되지 않는 유형의 버그를 구조적으로 만들어낸다.
##### 서버 보고서 (긴 텍스트 블록 2개, 마지막 것 출력) #####

# 부록 B. 서버(soon-server-side) 코드 분석 원문

> 읽기 전용 정적 분석. 근거는 `server/` 기준 상대 경로.

## 분석 결과: soon-server-side 유지보수 저해 요인

### 1. 에러 처리

**발견 1: 전역 에러 핸들러가 에러를 로깅하지 않고 삼킨다**
- 근거: `app.js:70-78` — 에러 핸들러가 `res.locals.message/error`를 세팅하지만 `res.render()`가 아니라 `res.sendStatus(err.status || 500)`만 호출한다. `console.log`/`console.error` 호출이 전혀 없다.
- 영향: 라우터에서 잡히지 않고 넘어온 에러는 클라이언트에 상태 코드만 전달되고 서버 로그 어디에도 남지 않는다. 장애가 나도 "무슨 일이 있었는지" 추적할 방법이 원천적으로 없다.

**발견 2: 모든 라우트의 catch가 `console.log(error)` 후 `res.sendStatus(500)`만 한다 (동일 패턴 반복)**
- 근거: `routes/movie.js:82-86,127-131,173-177,217-221,258-262,319-323,357-361,393-397`, `routes/search.js:73-77,120-124,149-153,168-172,187-191`, `routes/genre.js:78-82`, `routes/alarm.js:52-55,84-87,109-112,166-176,198-201,247-250,262-265,285-294,327-330`
- 클라이언트에는 에러 메시지·요청 컨텍스트·구분 가능한 에러 코드가 전혀 전달되지 않는다(그냥 HTTP 500). 안드로이드 앱 개발자(1인 개발자 본인)가 사용자 문의를 받아도 "500번 떴다" 이상의 정보를 얻을 수 없다.
- `console.log`만 사용하고 `console.error`는 쓰지 않아 stderr/stdout 스트림 구분도 안 된다 → 로그 필터링·심각도 구분 불가능.

**발견 3: 처리되지 않는 Promise rejection이 실제 코드에 존재 (재현 어려운 침묵 실패)**
- 근거: `bin/www.js:12-14` — `createTable().then(...)`에 `.catch()`가 없다. `utils/create_db.js`의 `createTable()`이 reject하면(예: MySQL 컨테이너가 아직 안 떴을 때) 처리되지 않은 rejection이 발생한다. `docker-compose.yml:11`의 주석 `# TODO: bin/www.js > creat_db.js 에서 connection error 발생 : healthcheck 로 해결해보기`가 이 문제를 개발자 스스로 인지하고도 방치했음을 보여준다.
- 근거: `routes/jobs.js:251-265` — 월간 알림 cron에서 `response = await admin.messaging().send(message);`가 **`let`/`const` 선언 없이** 대입된다. ES 모듈은 항상 strict mode이므로 이는 `ReferenceError: response is not defined`를 던진다. 그런데 catch 블록에서도 `catch (error)`로 잡아놓고 `"...\n" + err`로 **선언되지 않은 `err`**를 참조해 또 다른 `ReferenceError`를 던진다(에러 처리 코드 자체가 깨져 있음). 이 두 번째 에러는 어디서도 잡히지 않아 unhandled rejection이 된다.
- 영향: 월별 "이번 달 개봉작" 푸시 알림 cron(매월 1일 자정)이 사실상 **한 번도 정상 동작하지 않았을 가능성이 높다** — 그리고 그 실패가 크래시도, 로그도 남기지 않고 조용히 묻힌다. 이런 구조에서는 "알림이 안 온다"는 사용자 신고를 받아도 코드만 봐서는 원인을 못 찾고, 실제로 실행해봐야만 드러난다.

**발견 4: 동적 SQL에 사용자 입력이 그대로 문자열 결합됨 (SQL Injection + 예측 불가능한 실패)**
- 근거: `routes/alarm.js:117,120,156,160,215` — `req.body.company_id`가 이스케이프 없이 백틱 테이블명으로 직접 삽입(`` `" + req.body.company_id + "` ``). `routes/jobs.js:43,46,84,113,120,169`도 동일 패턴(`TABLE_NAME` 결합).
- 영향: 특정 company_id 값(따옴표·백틱 포함 등)이 들어오면 SQL 문법 에러가 나거나 의도치 않은 쿼리가 실행된다. 재현하려면 정확히 어떤 입력값이었는지 알아야 하는데 에러 로그에 입력값이나 스택트레이스가 남지 않으므로(발견 1,2) 재현이 사실상 불가능하다.

### 2. 로깅

**발견 1: 로그는 전부 `console.log`뿐 — 파일도, 구조화도, 외부 서비스도 없음**
- 근거: routes/utils/config/bin 전체에서 `console.log` 100회 이상 사용. winston/pino/sentry/newrelic/datadog/bugsnag/rollbar 등을 검색했으나 **0건** — 로깅 라이브러리도 에러 트래킹 서비스도 전혀 도입되어 있지 않다.
- 요청 ID, 타임스탬프(구조화된 형태로는 없음), 구조화된 JSON 로그 전혀 없음. 여러 요청이 동시에 들어오면 어떤 로그 줄이 어떤 요청에 속하는지 구분할 방법이 없다.
- 로그는 docker 컨테이너 stdout으로만 나가고 `docker-compose.yml:40-44`에서 `max-size: "60m", max-file: "1"` — **로그 파일이 1개만 유지되고 60MB 넘으면 가장 오래된 내용부터 사라진다.** 장애 발생 몇 시간/며칠 뒤에 로그를 보러 오면 이미 회전되어 사라졌을 가능성이 높다.

**발견 2: cron 잡 실패를 알 방법이 없음**
- 근거: `routes/jobs.js:126-130,182-186`에서 catch가 `console.log(error)` 후 `connection.rollback()`만 하고 종료. 실패해도 알림/메일/슬랙 어떤 채널로도 통지되지 않는다. cron은 `pm2-runtime`을 통해 별도 프로세스(`process.json:2-6`, name: "cron-jobs")로 돈다 — 이 프로세스가 죽거나 매 분 실패해도 개발자가 알 수 있는 유일한 방법은 서버에 직접 SSH로 들어가 docker logs를 확인하는 것뿐이다.

### 3. 관측성 (헬스체크/모니터링/알림)

**발견: 형식적인 헬스체크 엔드포인트만 존재, 실사용 흔적 없음**
- 근거: `routes/message.js:4-9` — `GET /message/checking/server`가 `{message:'Server is alive!'}`를 반환. `app.js:63`에서 `/message`로 마운트.
- 이 엔드포인트를 호출하는 외부 모니터링(UptimeRobot, Pingdom, cron curl 등)이 코드베이스 어디에도 없다. `docker-compose.yml`에도 `healthcheck:` 블록이 어느 서비스에도 없다(mysql/redis/node-app 모두 없음 — 오히려 주석으로 "healthcheck 로 해결해보기"라고만 적어놓고 실제 구현은 안 함).
- (추측) 서버가 죽어도 자동으로 알아차릴 방법이 없고, 사용자가 앱에서 안 되는 걸 신고해야만 알아차리는 구조로 보인다.
- `docker-compose.yml` 어느 서비스에도 `restart: always`/`unless-stopped` 정책이 없다 — 컨테이너가 죽으면 Docker가 자동 재시작도 안 해준다(기본값 `no`).

### 4. Cron 잡 구조 (routes/jobs.js)

- 매분(`* * * * *`, `jobs.js:26`) `unsubscribeInvalidToken` → `checkDifferenceOfCompanyTable` 순서로 실행. 각각 별도 MySQL 커넥션을 매 실행마다 새로 열고(`jobs.js:27`) `connection.end()`로 닫는다.
- **중복 실행 방지 없음**: node-cron은 이전 실행이 끝나지 않아도 다음 분에 새 콜백을 실행한다. `checkDifferenceOfCompanyTable`은 회사 테이블마다 TMDB API를 페이지네이션하며 순회하고 FCM 발송까지 하므로(`jobs.js:58-124`), 1분을 넘기면 동시에 두 실행이 같은 테이블에 대해 트랜잭션을 걸게 된다 — 락 경합/중복 INSERT/의도치 않은 rollback으로 이어질 수 있다(추측: 실제 발생 여부는 로그가 없어 확인 불가, 구조적으로는 가능).
- **한 회사(TABLE_NAME) 처리 중 에러가 나면 나머지 회사는 그 사이클에서 건너뛴다**: `try{ for(...){...} } catch`가 for 루프 전체를 감싸고 있어(`jobs.js:39-130`) 첫 실패 지점에서 전체 함수가 종료된다. 특정 회사 데이터 하나가 이상해지면 다른 모든 회사의 알림 갱신이 함께 막히는데, 이게 "왜 어떤 회사는 알림이 안 오는지"를 재현하기 어렵게 만든다(다음 분에 또 처음부터 순회하므로 랜덤하게 보일 수 있음).
- **외부 API(TMDB) 실패 시 처리 없음**: `fetch(url)` 응답의 `ok` 여부나 `total_pages` 존재 여부를 검증하지 않고 바로 `.json()`, `.results.map(...)` 을 호출한다(`jobs.js:70-75`). TMDB가 에러 응답이나 rate-limit(429)을 주면 `data.results`가 `undefined`가 되어 `TypeError`가 발생 → catch에서 `console.log(error)`만 하고 롤백 → 원인은 로그에 스택트레이스만 남고 "TMDB가 왜 그 응답을 줬는지"는 알 수 없다.
- **크롤링 의존성**: `utils/get-watch-provider-link.js`는 TMDB 웹페이지를 cheerio로 직접 스크래핑한다(`div.ott_provider`, `h3`, 특정 클래스 셀렉터 하드코딩, `get-watch-provider-link.js:9-13`). TMDB가 HTML 구조를 바꾸면 조용히 빈 배열을 반환하거나 예외를 던지며, 외부 사이트 개편에 따라 코드 변경 없이 어느 날 갑자기 깨지는 전형적 취약점이다.

### 5. 설정/비밀

**발견 1: Firebase 서비스 계정 프라이빗 키가 정적 공개 디렉토리에 있고 실제로 git에 커밋되어 있다**
- 근거: `app.js:23` — `app.use(express.static(path.join(__dirname, 'public')))`. `public/` 안에 `soon-79c2e-firebase-adminsdk-h7o9r-dc2b66a1c8.json`, `soon-79c2e-firebase-adminsdk-h7o9r-4666957f0e.json`(구버전 키, 정리 안 됨), `google-services.json`, `api-6534807506341788950-478396-05ea54bd98b1.json`이 존재.
- `git ls-files` 확인 결과 이 4개 파일 모두 **git에 tracked** 상태다. `.gitignore` 마지막 줄에 `*.json`이 추가돼 있지만, 이미 tracked된 파일은 gitignore 규칙이 소급 적용되지 않아 계속 커밋되고 있었다 — "차단했다고 생각했지만 실제로는 막히지 않은" 전형적 실패 사례.
- 영향: 배포된 서버에서 `GET /soon-79c2e-firebase-adminsdk-h7o9r-dc2b66a1c8.json` 같은 요청으로 **Firebase Admin SDK 프라이빗 키가 그대로 응답될 가능성이 높다**(routes 매칭 전에 static 미들웨어가 먼저 처리하므로 인증 없음). 이 키가 유출되면 제3자가 FCM 메시지를 임의로 발송하거나 Firebase 프로젝트에 접근할 수 있다.

**발견 2: `config/firebase.js`의 git 이력 자체가 디버깅 고통의 증거**
- 근거: `git log -p -- config/firebase.js` — legacy `FIREBASE_API_KEY` 방식 → `require('../public/service-account.json')`(ESM에서 동작 불가) → `import(..., {assert:{type:'json'}})`로 변경 → 파일명을 여러 차례 변경 → 한때 `console.log(key)`(프라이빗 키 전체를 로그에 출력)를 추가했다가 나중에 제거.
- `git log --oneline | grep -c "FCM 인증 방식 변경"` → **14개**의 동일한 커밋 메시지. 같은 문제를 몇 번이고 다시 건드렸다는 뜻으로, FCM 인증 문제의 근본 원인을 못 찾고 임시방편을 반복했음을 강하게 시사한다.

**발견 3: 하드코딩된 값들**
- `app.js:45` — Swagger 문서의 서버 URL이 EC2 퍼블릭 IP로 하드코딩(`http://34.229.27.130:3000`). IP가 바뀌면(EC2 재시작 등으로 흔함) API 문서가 즉시 틀린 정보를 안내하게 된다.
- `.env:2-3` — `DB_USER=root`, `DB_PASSWORD=root` (평문, 프로덕션 docker-compose에서 `MYSQL_ROOT_PASSWORD`로 그대로 사용, `docker-compose.yml:7`). `.env`는 git에는 안 잡혀 있지만(`.gitignore`로 확인) 로컬/서버 파일 시스템에 평문으로 존재.
- `routes/jobs.js:10`, `routes/alarm.js:13` 등 — Firebase `databaseURL`이 소스에 하드코딩.

### 6. 테스트

**발견: `test/` 폴더의 6개 파일은 자동화 테스트가 아니라, 지금은 실행조차 안 되는 죽은 수동 스크립트다**
- 근거: `package.json:5` — `"type": "module"`이 설정되어 있어 프로젝트의 `.js`는 기본적으로 ES 모듈로 해석되는데, `test/test_company_alarm.js`, `test/test_search_invalid_token.js`, `test/test_send_company_alarm.js`, `test/test_create_genre_img.js`는 전부 `require(...)`(CommonJS)를 사용한다 → ES 모듈 컨텍스트에서 `require`는 정의되어 있지 않아 **즉시 `ReferenceError`로 죽는다.**
- `require('request')`, `require('mysql')`(2.x 계열)를 사용하지만 `package.json`의 dependencies에는 `request`도 구버전 `mysql`도 없다(현재는 `mysql2`, `node-fetch` 사용) → 설령 CommonJS로 고쳐도 모듈이 없어서 실패한다.
- `test/test_search_invalid_token.js:6` — `require('../conifg/firebase')` **오타**(`conifg` → `config`), 디렉토리 자체가 존재하지 않아 이 파일은 애초에 한 번도 정상 실행된 적이 없을 가능성이 높다(추측이지만 오타가 남아있다는 것 자체가 실행/리뷰가 없었다는 강한 근거).
- `test_send_noti.js`, `test_send_company_alarm.js`는 `fcm-node`의 legacy 서버 키 방식(`config/firebase`의 `.api_key`)을 참조하는데, 현재 `config/firebase.js`는 함수를 default export(`getAccessToken`)할 뿐 `api_key` 프로퍼티가 없다 — 이 두 파일은 애초에 지금 코드베이스와 API 형태가 안 맞는, 리팩터링 이전 시점에 멈춰버린 스크립트다.
- `package.json`에 `test` 스크립트가 없고, CI 설정 파일도 발견되지 않음 → 실행 방법 자체가 문서화되어 있지 않다.
- 결론: 테스트가 회귀를 막아주지 못한다. 코드를 고칠 때마다 수동으로 프로덕션과 비슷한 환경에서 직접 눌러보는 수밖에 없다.

### 7. 의존성

- `package.json:24` — `firebase-admin: ^8.13.0` (2020년경 버전, 현재 major는 12.x대. v8은 여러 보안 패치가 안 됨).
- `redis: ^4.6.8`이지만 모든 라우트에서 요청마다 `client.connect()` → `client.quit()`(`routes/movie.js:48-50,84-87` 등)를 반복 — connection pooling을 안 쓰고 매 요청마다 접속/종료를 반복하는 것은 레이턴시와 경합의 원인이 될 수 있다(성능 이슈지만 "간헐적으로 느려지거나 실패한다"는 증상의 원인이 될 수 있음, 재현 어려움에 기여).
- `node-fetch: ^3.3.2`(ESM 전용)와 `sync-request: ^6.0.0`(동기 블로킹 HTTP, `request` 기반이라 `request`가 deprecated된 이후 유지보수 안 됨), `fcm-node: ^1.6.1`(수년간 미유지보수, legacy FCM HTTP API 기반이라 언젠가 구글이 legacy API를 완전히 끄면 즉시 깨짐), `node-cron: ^2.0.3`(2.x, 최신은 3.x).
- `package.json`에 `engines` 필드 없음 → Node 버전 요구사항이 명시되어 있지 않다. `Dockerfile:2`는 `node:18.17.1`을 쓰지만 로컬 개발 환경에서 다른 Node 버전으로 실행하면 동작이 달라질 수 있음(예: `import ... assert {type:'json'}` 문법은 Node 버전에 따라 지원 여부가 다름).
- `mysql2`(신규, alarm.js/jobs.js/create_db.js)와 테스트 파일의 구버전 `mysql`(패키지에 없음)이 혼재 — 마이그레이션이 라우트/유틸에서는 끝났지만 테스트에서는 반영 안 됨.

### 8. 배포

- `Dockerfile:5,14-15` — `WORKDIR /soon-server-side`에서 `npm install` 후 `CMD ["npm","start"]`. 그런데 `docker-compose.yml:30-31`에서 `volumes: - ./:/soon-server-side`로 **호스트 전체 디렉토리를 컨테이너 WORKDIR에 바인드 마운트**한다. 이는 이미지 빌드 시 생성된 `node_modules`를 호스트 쪽 디렉토리(현재 `node_modules`가 존재하지 않는 것으로 확인됨)로 덮어써 버린다는 뜻이다. 즉 실질적으로 컨테이너가 사용하는 코드/모듈은 이미지가 아니라 **호스트 파일시스템 상태**에 좌우된다 — EC2 인스턴스를 새로 만들어 `docker-compose up`만 하면 `node_modules`가 없어 앱이 뜨지 않을 가능성이 높다(추측: 실제 EC2에는 별도로 `npm install`을 수동으로 해놓았을 가능성이 있으나, 그 사실이 `README.md`/`pull_and_restart.sh`/`Dockerfile` 어디에도 문서화되어 있지 않다).
- `pull_and_restart.sh:1-9` — `git pull` → `docker-compose down` → `up -d`. **`--build` 옵션이 없다** → package.json 의존성이 바뀌어도 이미지가 재빌드되지 않는다(위 바인드 마운트 문제와 겹쳐 더 혼란스러움). 배포는 완전히 수동(SSH 접속 후 스크립트 실행)이고 롤백 메커니즘이 없다 — 이전 상태로 되돌리려면 `git checkout <이전 커밋>` 후 다시 스크립트를 돌려야 하며, 이미지 태그/버전 관리가 전혀 없다.
- `docker-compose.yml` 어디에도 `restart:` 정책이 없어 컨테이너가 죽으면 자동 재시작되지 않는다(3번 항목과 중복이지만 배포 안정성 관점에서도 치명적).
- CI/CD 파이프라인 파일(GitHub Actions 등) 없음 — 배포 전 자동 테스트/빌드 검증 단계가 전혀 없다.

### 9. 데이터

- `bin/www.js:11-14` — `createTable()`을 서버 시작 시 매번 호출, `.catch()` 없음(1번 항목과 동일 근거).
- `utils/create_db.js:6` — `"CREATE DATABASE IF NOT EXISTS " + dbconfig.database`. 그런데 `config/database.js:4-12`의 default export에는 **최상위 `database` 프로퍼티가 없다**(DB 이름은 `connection.database`에 중첩되어 있음). 따라서 `dbconfig.database`는 `undefined`이고, 실제로 실행되는 SQL은 `CREATE DATABASE IF NOT EXISTS undefined;`이다. MySQL 입장에서 `undefined`는 유효한 식별자이므로 에러 없이 **"undefined"라는 이름의 불필요한 빈 데이터베이스**를 만들 뿐, 원래 의도한 `alarm_db` 생성에는 관여하지 않는다(실제 `alarm_db` 생성은 `docker-compose.yml:8`의 `MYSQL_DATABASE` 환경변수에 의한 컨테이너 최초 부팅 시 초기화에 의존). 코드만 봐서는 정상 동작하는 것처럼 보이지만 실제로는 아무 일도 하지 않는 죽은 코드이며, 이런 종류의 "겉보기엔 맞는데 조용히 틀린" 코드가 쌓이면 다음 사람이 안심하고 지나칠 수 있다는 점에서 유지보수를 어렵게 만든다.
- **마이그레이션 관리 도구 없음**: `create_db.js`가 유일한 스키마 정의 수단이고 `CREATE TABLE IF NOT EXISTS`만 있을 뿐, 스키마 변경 이력을 추적하는 마이그레이션 파일이나 도구(Knex, Sequelize migrate, Flyway 등)가 전혀 없다. 회사별 동적 테이블(`` `${company_id}` ``)도 코드 실행 중(`alarm.js:156`, `jobs.js`) 즉석에서 생성/삭제되어 스키마가 데이터에 따라 계속 변하는 구조 — DB 상태를 재현하거나 스테이징 환경을 로컬에서 똑같이 만들기 어렵다.
- `data/mysql/`, `data/redis/dump.rdb` 실제 DB 데이터 파일이 저장소 디렉토리 안에 그대로 존재(다행히 `.gitignore:4`의 `data/`로 git에서는 제외됨) — 로컬 개발 시 이 실제 데이터 볼륨과 코드가 뒤섞여 있어 "깨끗한 상태에서 재현"이 어렵다.

### 10. 문서

- `README.md` 전체를 확인했으나 설치/실행 방법(그마저도 `.env` 항목 나열 부분이 **중간에 끊겨 있음**, `README.md:36` 이후 내용 없음)만 있을 뿐, 장애 대응 절차·모니터링 방법·롤백 절차·cron 잡 설명·트러블슈팅 가이드 등 운영 런북(runbook)에 해당하는 내용은 전혀 없다.
- 코드 안의 유일한 "운영 지식"은 `docker-compose.yml:11`의 TODO 주석 하나(`# TODO: bin/www.js > creat_db.js 에서 connection error 발생 : healthcheck 로 해결해보기`)뿐이며, 이는 알려진 버그가 문서화 대신 주석 하나로 방치되었다는 뜻이다.

---

### 가장 치명적인 상위 5개

1. **관측성 완전 부재** — 로깅 라이브러리·에러 트래킹·모니터링·알림 중 아무것도 없고(항목 2,3), 전역 에러 핸들러조차 에러를 로깅하지 않는다(`app.js:70-78`). 장애가 나도 "무슨 일이 있었는지" 알 방법 자체가 없다 — 이것이 다른 모든 문제를 "찾기 어렵게" 만드는 근본 원인이다.

2. **처리되지 않는 에러로 조용히 죽는 기능들** — 월간 FCM 알림 cron이 선언되지 않은 변수 참조로 매번 `ReferenceError`를 던지며(`routes/jobs.js:251-265`), 이 에러조차 제대로 안 잡혀서 unhandled rejection이 된다. 몇 년간 이 기능이 동작했는지조차 코드만 봐서는 확인이 안 되고, 아무 데도 로그가 안 남는다.

3. **Firebase 프라이빗 키가 정적 공개 디렉토리에 커밋되어 배포됨** (`public/*.json`, `app.js:23`의 `express.static`) — `.gitignore`에 나중에 `*.json`을 추가했지만 이미 tracked라 무력화됨. 보안 사고이면서 동시에, "막았다고 생각했는데 실제로는 안 막힌" 것의 전형이라 이 프로젝트의 신뢰도 자체를 의심하게 만든다.

4. **배포 구조가 호스트 상태에 암묵적으로 의존** — `docker-compose.yml`의 bind mount(`./:/soon-server-side`)가 이미지 빌드 결과(`node_modules`)를 덮어써 실제 실행은 호스트 파일시스템 상태에 좌우되고, `pull_and_restart.sh`는 `--build`도 안 하고 롤백 수단도 없다. 서버를 재구축해야 하는 상황(EC2 장애 등)이 오면 무엇이 필요한지 아무도(코드도, 문서도) 말해주지 않는다.

5. **테스트가 전부 죽어 있음** — `test/` 6개 파일이 CommonJS/ESM 혼용, 존재하지 않는 패키지, 오타 경로, 구버전 API 형태 참조로 전혀 실행되지 않는다(`package.json`에 test 스크립트도 없음). 회귀를 잡아줄 안전망이 없어 모든 변경은 프로덕션에서 직접 확인하는 수밖에 없고, 이는 SQL 인젝션성 문자열 결합(`alarm.js`, `jobs.js`)이나 `dbconfig.database` undefined 버그처럼 "겉보기엔 동작하는" 잠재적 버그들이 계속 방치되는 이유와 직결된다.
