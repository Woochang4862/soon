---
kind: canonical
status: active
canonical: mydocs/plans/task_11.md
last_verified: 2026-11-09
---

# 수행계획 — Task #11 Gson non-null 필드의 지연 NPE

- **이슈**: [#11](https://github.com/Woochang4862/soon/issues/11)
- **브랜치**: `task_11` (`task_3` 기준 — `task_3` 이 컴파일 안 되는 기존 테스트를 지워야 단위 테스트를 돌릴 수 있다. PR base 도 `task_3`)
- **작성일**: 2026-11-09 KST
- **상태**: 작업지시자 사전 승인(2026-11-09 채팅)

## 1. 현재 코드 (문제)

`model/schema/Movie.kt` 의 `title`, `originalTitle`, `originalLanguage`, `genreIds`, `releaseDate` 는 Kotlin non-null 타입이다. 기본 생성자가 없어 Gson 은 `Unsafe` 로 객체를 만들고 null 검사를 하지 않는다. JSON 에 필드가 없거나 `null` 이면 non-null 필드에 null 이 들어가고, 파싱 시점이 아니라 사용 시점(`movie.title.trim()` — `MoviePagedListAdapter.kt:25`, `for (id in movie.genreIds)` — `SearchMovieViewHolder.kt:66` 등)에서 NPE 가 난다. `Movie` 는 `ReleaseAlarmDataSource` 가 SharedPreferences 에 Gson 으로 저장하므로 예전 버전이 저장한 JSON 에서도 같은 일이 생긴다.

## 2. 변경 계획

라이브러리 교체(Moshi/kotlinx.serialization)는 Retrofit 컨버터·SharedPreferences 저장 형식까지 바뀌어 이 이슈 범위를 넘는다. Gson 을 유지하고 다음 두 가지로 막는다.

### Stage 1 — 수정 전 재현 테스트

`MovieGsonTest`: (a) 필드 누락 JSON, (b) 명시적 `null` JSON 을 `Gson().fromJson` 한 뒤 `title.trim()`, `genreIds.size`, `releaseDate.length` 를 호출 → 수정 전 NPE 로 실패.

### Stage 2 — 수정

1. 모든 생성자 인자에 기본값을 준다 → Kotlin 이 무인자 생성자를 만들고 Gson 이 이를 써서 누락 필드는 기본값이 된다.
2. 명시적 `null` 은 1 로 막히지 않는다(Gson 이 null 을 그대로 대입). `@JsonAdapter(NullSkippingTypeAdapterFactory::class)` 로 객체의 `null` 값 항목을 버린 뒤 기본 어댑터에 넘긴다. 어노테이션 방식이라 Retrofit·SharedPreferences 등 모든 `Gson` 인스턴스에 적용된다.
3. 기본값: 문자열 `""`, 목록 `emptyList()`, 수치 0, Boolean false. nullable 필드(`posterPath` 등)는 그대로 null.

## 3. 검증

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- 에뮬레이터: 앱 실행 J1, 알람 목록(저장된 `Movie` 역직렬화 경로) 진입 시 크래시 없음. 서버가 다운이라 네트워크 응답 경로는 단위 테스트로만 확인한다.

## 4. 범위 밖

- `MovieDetail` 등 다른 스키마의 같은 패턴 — 단계 보고서에 목록만 적는다
- `vote_average` 를 `Int` 로 선언해 생기는 `NumberFormatException`(postcheck §7-2) — 별도 이슈 필요
