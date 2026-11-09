---
kind: canonical
status: active
canonical: mydocs/plans/task_5.md
last_verified: 2026-11-09
---

# 수행계획 — Task #5 죽은 catch (`try { lifecycleScope.launch{} } catch`)

- **이슈**: [#5](https://github.com/Woochang4862/soon/issues/5)
- **브랜치**: `task_5` (`task_3` 기준 — 마스터 계획의 "관측성 뒤에 착수" 순서와 `ErrorReporter` 사용 때문. PR base 도 `task_3`)
- **작성일**: 2026-11-09 KST
- **상태**: 작업지시자 사전 승인(2026-11-09 채팅)

## 1. 현재 코드 (문제)

| 위치 | 내용 |
|---|---|
| `view/company_list/CompanyListActivity.kt:64-87` | `try { lifecycleScope.launch { getFlow().collectLatest { submitData } } } catch` — `launch` 는 즉시 반환하므로 코루틴 안 예외를 이 catch 가 잡을 수 없다 |
| `view/search/SearchActivity.kt:139-152` | 같은 구조. catch 의 `adapter.onEmpty()` 는 실행되지 않는다 |

게다가 Paging 3 의 `PagingSource.load` 는 네트워크 예외를 `LoadResult.Error` 로 바꿔 돌려주므로(`SearchCompanyPageKeyDataSource.kt:41-46` 등), 예외는 `collectLatest` 까지 오지도 않는다. 오류는 `LoadState.Error` 로만 관측된다. 즉 catch 를 `launch` 안으로 옮기는 것만으로는 고쳐지지 않는다.

결과: 서버가 다운되면 제작사 목록 화면은 아무 안내 없이 빈 화면이고, "다시 시도" 대화상자는 한 번도 뜬 적이 없다.

## 2. 변경 계획

### Stage 1 — 수정 전 재현

1. 단위 테스트 `SearchCompanyPageKeyDataSourceTest`: 실패하는 `CompanyApi` 가짜로 `load()` 를 부르면 예외를 던지지 않고 `LoadResult.Error` 를 돌려준다 → `collectLatest` 바깥 catch 는 원리상 닿지 않음을 고정.
2. 에뮬레이터 Journey (서버 다운 상태 그대로): `CompanyListActivity` 를 `query` extra 와 함께 실행 → 대화상자 없음, 빈 화면을 기록.

### Stage 2 — 수정

1. 두 곳 모두 `try/catch` 를 걷어내고, `adapter.addLoadStateListener` 에서 `refresh is LoadState.Error` 를 처리한다.
   - 제작사 목록: `ErrorReporter.report` + 기존 "다시 시도하시겠습니까?" 대화상자. 재시도는 `adapter.retry()` (같은 Pager 재사용).
   - 검색: `ErrorReporter.report` + 기존 의도대로 `adapter.onEmpty()`.
2. 오류 판정은 순수 함수 `refreshError(CombinedLoadStates): Throwable?` 로 빼 단위 테스트한다.
3. 재시도 카운터 결함(#8)은 이 타스크에서 고치지 않는다. 대화상자 코드는 옮기기만 한다.

## 3. 검증

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- 에뮬레이터: Stage 1 Journey 재실행 → 대화상자 표시, "아니요"로 종료, logcat `SoonError` 태그에 원인 예외. 검색 화면에서 검색 → 빈 화면 애니메이션 표시, 크래시 없음.

## 4. 범위 밖

- 재시도 횟수 제한(#8)
- `MovieListFragment.kt:140`, `ThisMonthMovieFragment.kt:121` 의 같은 형태 try/catch — 이슈 본문에 없는 위치다. 단계 보고서에 사실만 적고 별도 판단을 요청한다.
