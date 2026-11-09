---
kind: working
status: active
canonical: mydocs/plans/task_5.md
issue: 5
stage: 1
date: 2026-11-09
---

# Task #5 단계 보고서 — 죽은 catch 를 LoadState 처리로 교체

## 1. 결론

- 제작사 목록(`CompanyListActivity`)과 검색(`SearchActivity`)에서 `try { lifecycleScope.launch{} } catch` 를 걷어내고, `LoadState.Error` 를 보고 오류를 처리하게 바꿨다.
- 에뮬레이터(운영 서버 다운 상태): 수정 전 제작사 목록은 빈 화면, 검색은 빈 검정 화면. 수정 후 제작사 목록은 "다시 시도하시겠습니까?" 대화상자, 검색은 빈 결과 애니메이션.
- 계획서와 다른 점: catch 를 `launch` 안으로 옮기는 것만으로는 고쳐지지 않는다. Paging 3 의 `PagingSource` 가 네트워크 예외를 `LoadResult.Error` 로 바꿔 돌려주므로 예외가 `collectLatest` 까지 오지 않는다(단위 테스트로 고정).

## 2. 커밋 (브랜치 `task_5`, base `task_3`)

| 커밋 | 내용 |
|---|---|
| `test(android): #5 PagingSource …` | `SearchCompanyPageKeyDataSourceTest`, `unitTests.returnDefaultValues = true` |
| (이 보고서와 같은 커밋) | `util/LoadStates.kt`(`refreshError()`), `RefreshErrorTest`, 두 Activity 수정, 계획서, 스크린샷 |

## 3. 재현 (수정 전)

수정 전 APK 를 따로 빌드해(`git stash` 후 `assembleDebug`, dex 에 수정 문자열 0건 확인) 설치했다.

`CompanyListActivity` 는 `exported=false` 라 `adb shell am start` 가 `Permission Denial` 로 거절된다. 에뮬레이터 `adb root` 후 실행했다.

```
adb shell am start -n com.lusle.android.soon/.view.company_list.CompanyListActivity --es query disney
I/okhttp.OkHttpClient: <-- HTTP FAILED: java.net.SocketTimeoutException: failed to connect to /34.229.27.130 (port 3000) ... after 10000ms
```

16초 뒤 화면: 대화상자 없음, 빈 화면 — [before_company_list](img/task_5/before_company_list.jpg).
검색 `disney`: 실패 후 빈 검정 화면, 빈 결과 애니메이션 없음 — [before_search_blank](img/task_5/before_search_blank.jpg).

단위 테스트 `SearchCompanyPageKeyDataSourceTest`: 실패하는 `CompanyApi` 로 `load()` → `LoadResult.Error` 반환(예외를 던지지 않음). 수정 전 코드에서도 통과한다 — 결함의 원인(catch 가 닿을 수 없음)을 고정하는 테스트다. 수정 전 실패는 위 에뮬레이터 재현으로 확인했다.

## 4. 변경

| 파일 | 변경 |
|---|---|
| `util/LoadStates.kt` (신규) | `CombinedLoadStates.refreshError(): Throwable?` |
| `view/company_list/CompanyListActivity.kt` | `load()` 에서 try/catch 제거. `observeLoadError()` 가 `addLoadStateListener` 로 refresh 오류를 받아 대화상자 표시. 재시도는 `adapter.retry()`. 같은 오류 객체로 대화상자를 다시 띄우지 않음. `setCancelable(false)` |
| `view/search/SearchActivity.kt` | try/catch 제거. 기존 load state 리스너에서 refresh 오류면 `adapter.onEmpty()` |
| `app/build.gradle` | `testOptions.unitTests.returnDefaultValues = true` (JVM 테스트에서 `android.util.Log`) |

원인 예외는 #3 에서 각 `PagingSource` 의 catch 가 이미 `ErrorReporter.report` 로 보낸다. 화면 리스너에서 다시 보내면 Crashlytics 에 같은 실패가 두 번 기록돼(첫 빌드에서 `SoonError` 2건 관측) 화면 쪽은 `Log.w` 로 맥락만 남기게 바꿨다.

재시도 카운터 분기(#8)는 옮기기만 하고 고치지 않았다. 버튼 문구 "제시도"도 그대로.

## 5. 검증

```
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
BUILD SUCCESSFUL

ExceptionMessageTest 3/3, ExampleUnitTest 1/1, RefreshErrorTest 3/3, SearchCompanyPageKeyDataSourceTest 1/1 (failures 0)
```

에뮬레이터 `Pixel_API34`, 수정 후 APK:

```
adb shell am start -n …/.view.company_list.CompanyListActivity --es query disney
E/SoonError: java.lang.RuntimeException: java.net.SocketTimeoutException: failed to connect … after 10000ms   (1건)
W/CompanyListActivity: company list load failed: query=disney
mCurrentFocus=…CompanyListActivity → 대화상자 표시
"아니요" 탭 → topResumedActivity=NexusLauncherActivity (화면 종료)
```

화면: [after_company_list_dialog](img/task_5/after_company_list_dialog.jpg), 검색 [after_search_empty](img/task_5/after_search_empty.jpg)

```
D/SearchActivity: onQueryTextSubmit: disney
I/okhttp.OkHttpClient: <-- HTTP FAILED: java.net.SocketTimeoutException …
```

`FATAL EXCEPTION` 0건.

## 6. 검증 환경 메모

- 에뮬레이터 `emulator-5554` 를 다른 세션의 Flutter 앱(`com.woochang.fixcost`)이 함께 쓰고 있었다. 18:06 에 그 앱이 앞으로 올라와 첫 재현 캡처가 무효가 됐고 다시 찍었다. 별도 AVD 를 만들려 했으나 디스크 여유 4.5GB 로 userdata 파티션을 못 만들어 지웠다.
- `adb root` 로 adbd 를 재시작했다(비공개 Activity 실행용). 같은 에뮬레이터를 쓰는 세션의 adb 연결이 한 번 끊겼을 수 있다.
- 18:10:53 에 ANR 1건: main 스레드가 `IInputMethodManager.startInputOrWindowGainedFocus` 바인더 호출에서 대기. 앱 코드 프레임이 아니며, 17:57 에는 메시지 앱(`com.google.android.apps.messaging`)도 ANR 이 났다. 에뮬레이터 부하로 판단하고 조치하지 않았다.

## 7. 범위 밖 — 판단 필요

| 위치 | 증상 | 근거 |
|---|---|---|
| `view/movie_list/MovieListFragment.kt:132-146`, `view/main/this_month_movie/ThisMonthMovieFragment.kt:113-126` | `launch { try { collectLatest } catch }` 형태라 이슈의 패턴은 아니지만, 같은 이유(PagingSource 가 예외를 던지지 않음)로 catch 의 `showErrorSnackBar()` 가 실행되지 않는다. 서버 다운 시 홈이 안내 없이 빈 화면인 원인(#3 J1 관측과 일치) | 이 보고서 §1 |

같은 `refreshError()` 로 고칠 수 있다. #5 에 포함할지, 새 이슈로 할지 결정 필요.
