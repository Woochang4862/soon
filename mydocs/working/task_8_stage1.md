---
kind: working
status: active
canonical: mydocs/plans/task_8.md
issue: 8
stage: 1
date: 2026-11-09
---

# Task #8 단계 보고서 — 재시도 카운터 리셋 수정

## 1. 결론

- 재시도 분기를 `util/RetryLimiter` 로 옮기고(동작 그대로) 재현 테스트 3건이 실패함을 확인한 뒤 고쳤다.
- 카운터는 로드 성공(`refresh is NotLoading`) 때만 되돌린다. 재시도는 5회까지, 6번째 "재시도"는 "잠시 후 다시 시도해 주세요." 토스트 후 화면 종료.
- 에뮬레이터(서버 다운 상태): 실패 6회(최초 1 + 재시도 5) 후 6번째 탭에서 `wm_finish_activity … CompanyListActivity, app-request` + Toast 창 확인.
- 버튼 문구 "제시도" → "재시도".

## 2. 커밋 (브랜치 `task_8`, base `task_5`)

| 커밋 | 내용 |
|---|---|
| `test(android): #8 재시도 분기를 RetryLimiter 로 옮기고 재현 테스트 추가` | 원래 분기(조건 밖 `count = 0`)를 그대로 옮긴 `RetryLimiter`, `RetryLimiterTest` |
| (이 보고서와 같은 커밋) | 수정, 계획서 |

`task_8` 이 `task_5` 위인 이유: #5 전에는 재시도 대화상자가 실행될 수 없는 catch 안에 있어 이 결함 자체를 재현할 수 없다.

## 3. 재현 테스트 (수정 전)

```
./gradlew :app:testDebugUnitTest --console=plain
RetryLimiterTest > reset_after_success_allows_retries_again FAILED   (RetryLimiterTest.kt:31)
RetryLimiterTest > count_accumulates_between_retries FAILED          (RetryLimiterTest.kt:23)
RetryLimiterTest > allows_exactly_max_retries_then_refuses FAILED    (RetryLimiterTest.kt:15)
11 tests completed, 3 failed
```

수정 전 동작의 에뮬레이터 재현(무한 재시도)은 **미실행** — 분기를 바꾸지 않고 옮긴 함수의 단위 테스트로 대신했다. 에뮬레이터 한 번에 재시도마다 10초 이상 걸리고, 같은 에뮬레이터를 다른 세션이 쓰고 있어 실행 횟수를 줄였다.

## 4. 변경

| 파일 | 변경 |
|---|---|
| `util/RetryLimiter.kt` | `tryAcquire()`: `count >= max` 면 false, 아니면 증가 후 true. `reset()` |
| `view/company_list/CompanyListActivity.kt` | `retryNumber`/조건 밖 리셋 제거 → `retryLimiter`. 로드 상태 리스너에서 `refresh is NotLoading` 이면 `reset()`. 한도 초과 시 Toast + `finish()`. `MAX_RETRY_NUM` 을 companion `const` 로. 문구 "재시도" |

계획서와 다른 점: 원래 조건 `<=` 는 의도대로 동작했어도 6회를 허용했다. 이름(`MAX_RETRY_NUM = 5`)대로 5회로 맞췄다.

## 5. 검증

```
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
BUILD SUCCESSFUL in 3s
ExceptionMessageTest 3/3, ExampleUnitTest 1/1, RetryLimiterTest 3/3, RefreshErrorTest 3/3, SearchCompanyPageKeyDataSourceTest 1/1
```

에뮬레이터 Journey (스크립트: 실패 로그가 i 번째로 찍히면 Soon 이 맨 앞인지 확인하고 "재시도" 탭):

```
failure 1 seen -> tap 18:20:56
failure 2 seen -> tap 18:21:09
failure 3 seen -> tap 18:21:22
failure 4 seen -> tap 18:21:35
failure 5 seen -> tap 18:21:48
failure 6 seen -> tap 18:22:01
failures logged: 6   http failed: 6   fatal: 0

I/wm_finish_activity: [0,10610169,28,com.lusle.android.soon/.view.company_list.CompanyListActivity,app-request]   18:22:01.222
D/CoreBackPreview: Window{847b16c u0 Toast} …                                                                   18:22:01.833
```

탭 6회 중 앞 5회는 재시도(HTTP 요청 5회 추가), 6번째는 요청 없이 종료됐다.

첫 시도는 무효: 고정 13초 간격으로 탭했는데 에뮬레이터 부하로 Activity 표시가 57초 걸려 탭이 대화상자보다 먼저 들어갔고, 18:17:57 에 다른 세션 앱(`com.woochang.fixcost`)이 앞으로 올라왔다. 그래서 위 스크립트로 바꿨다.

## 6. 범위 밖

- 재시도 사이 지연(backoff) 없음
- 화면 회전 등으로 Activity 가 다시 만들어지면 카운터가 0 부터 시작한다(ViewModel 에 두지 않음)
