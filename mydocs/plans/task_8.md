---
kind: canonical
status: active
canonical: mydocs/plans/task_8.md
last_verified: 2026-11-09
---

# 수행계획 — Task #8 재시도 카운터가 항상 0 으로 리셋

- **이슈**: [#8](https://github.com/Woochang4862/soon/issues/8)
- **브랜치**: `task_8` (`task_5` 기준 — #5 전에는 재시도 대화상자 코드가 죽은 catch 안에 있어 실행되지 않으므로 재현 자체가 불가능. PR base 도 `task_5`)
- **작성일**: 2026-11-09 KST
- **상태**: 작업지시자 사전 승인(2026-11-09 채팅)

## 1. 현재 코드 (문제)

`CompanyListActivity` 의 "제시도" 버튼 처리:

```kotlin
if (retryNumber <= MAX_RETRY_NUM) {
    retryNumber += 1
    load(query)
}
retryNumber = 0          // 조건 밖 — 매번 0 으로 돌아간다
```

`MAX_RETRY_NUM = 5` 가 무력화돼 무한 재시도가 된다. `<=` 라 의도대로라도 6회다. 버튼 문구 "제시도"는 오타.

## 2. 변경 계획

### Stage 1 — 수정 전 재현 테스트

현재 분기를 그대로 옮긴 `RetryLimiter`(순수 Kotlin)를 만들고, 동작을 바꾸지 않은 채 `RetryLimiterTest` 를 추가한다: 5회 허용 후 6번째는 거절해야 한다 → 현재 로직은 무한 허용이라 실패.

### Stage 2 — 수정

1. 리셋은 **성공했을 때만**(`refresh is LoadState.NotLoading` 이고 아이템이 있을 때) 한다.
2. 허용 횟수는 `MAX_RETRY_NUM` 회 정확히(`<`).
3. 한도에 닿으면 대화상자 대신 "잠시 후 다시 시도해 주세요" 안내 후 종료(`finish()`) — 기존 "아니요" 와 같은 종료 경로.
4. 버튼 문구 "제시도" → "재시도".

## 3. 검증

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- 에뮬레이터(서버 다운 상태): 제작사 목록 진입 → "재시도" 5회 → 6번째에 안내 후 화면 종료. 앱 크래시 없음.

## 4. 범위 밖

- 재시도 간 지연(backoff)
