---
kind: working
status: active
canonical: AGENTS.md
issue: 없음 (PR #17 사후 검증. 이슈 번호가 정해지면 파일명을 task_{issue}_stage1.md 로 옮긴다)
stage: 1
date: 2026-10-25
---

# PR #17 사후 검증 — MovieItemDecoration 그리드 간격

## 1. 결론

- master(`dab58fb`) `assembleDebug` 성공. PR #17 코드(`_numberOfNotMovieItem`)가 APK dex 에 들어 있음을 확인.
- 상세 화면 유사 영화 그리드(비영화 0개)와 제작사 결과가 있는 검색(비영화 3개)은 스크롤·재진입 후에도 간격이 정상이다.
- **제작사 결과가 없는 검색은 간격이 틀어진다.** 검색 결과 앞의 비영화 아이템은 3개로 고정이 아니라 1개 또는 3개다. 브랜치 `fix/movie-item-decoration-header-count` 에 수정 커밋 `e810d4f` 를 만들었다. **push 하지 않았다 — 승인 필요.**

## 2. 환경

| 항목 | 값 |
|---|---|
| 에뮬레이터 | `Pixel_API34` (1080×2400, density 2.625 → 12dp = 31px, 25dp ≈ 65px) |
| 서버 | 로컬 `docker-compose up --build -d` (`server/`). 운영 서버 `34.229.27.130` 은 여전히 다운 |
| 측정 방법 | `adb shell uiautomator dump` 의 `transformation_layout`(영화 아이템) bounds 로 열 간격·행 간격 계산. 스크린샷은 `adb exec-out screencap` |

## 3. 검증 중 발견한 서버 문제 — 검증용 임시 변경으로 우회 후 되돌림

로컬 서버는 떴지만(`/message/checking/server` 200) 검색·유사 영화가 모두 0건이었다.

```
curl localhost:3000/api/search/movie?query=dune&region=KR&page=1
{"status_code":7,"status_message":"Invalid API key: You must be granted a valid key.","success":false}
```

원인: `server/.env` 의 `TMDB_API_KEY` 는 244자 `eyJ…` 로 시작하는 **v4 Read Access Token** 인데, 코드는 이를 v3 방식 `api_key` 쿼리로 보낸다. 같은 값을 `Authorization: Bearer` 로 보내면 `total_results 1118` 로 정상 응답한다. → #2(서버 재구축) 범위에서 v3 키를 받거나 코드를 Bearer 방식으로 바꿔야 한다.

## 4. 검증용 임시 변경 (커밋하지 않음, 모두 되돌림)

| 변경 | 목적 | 되돌림 |
|---|---|---|
| `android/.../model/api/ApiClient.kt` baseUrl → `http://10.0.2.2:3000` | 에뮬레이터에서 로컬 서버 접속 | `git checkout --` 로 복원. 현재 `http://34.229.27.130:3000` |
| `server/routes/{genre,movie,search}.js` 의 `import fetch from 'node-fetch'` → `../utils/_tmp_tmdb_fetch_shim.js` | §3 의 v4 토큰을 Bearer 헤더로 보내기 | `git checkout --` 로 복원 |
| `server/utils/_tmp_tmdb_fetch_shim.js` 신규 | 같은 목적 | 파일 삭제 |

복원 후 `git status --short` 출력 0줄. 복원 상태로 `./gradlew :app:assembleDebug` 다시 실행 → `BUILD SUCCESSFUL`. 로컬 서버는 `docker-compose down` 으로 내림(볼륨 삭제 없음).

앱 하드코딩 IP 를 BuildConfig 로 빼는 일은 #14 범위라 하지 않았다.

## 5. master(#17) 검증 결과

```
./gradlew :app:assembleDebug --console=plain
BUILD SUCCESSFUL in 3m 2s        (41 tasks UP-TO-DATE — 이미 #17 소스로 빌드된 산출물)
unzip classes*.dex | strings → classes5.dex 에 _numberOfNotMovieItem 존재
```

| 화면 | 비영화 아이템 | 측정 (px) | 판정 |
|---|---|---|---|
| 홈 "이번 달" (기본값 1) | 1 | 열 간격 31, 행 간격 31 | 정상 |
| 검색 `whiplash` (제작사 4건) | 3 | 첫 행 top 1091(헤더 바로 아래), 열 31, 행 31. 아래로 스크롤 뒤·맨 위 복귀 뒤·상세 갔다 돌아온 뒤 모두 동일 | 정상 |
| 검색 `ratatouille` (제작사 0건) | **1** | 첫 영화 top 443 / 두 번째 영화 top 412, 두 번째 영화 x 525–1017(기대 555–1048), 열 간격 **1** | **결함** |
| 상세 유사 영화 (0) | 0 | 열 31, 행 31. 재진입 후 동일 | 정상 |

스크린샷: [검색 whiplash](img/movie_item_decoration/A_search_whiplash.jpg), [검색 ratatouille — 결함](img/movie_item_decoration/A_search_ratatouille.jpg), [상세 유사 영화](img/movie_item_decoration/A_detail_similar.jpg)

### 원인

`SearchViewModel` 은 `insertSeparators` 로 맨 앞에 "영화 결과" 헤더를 넣고, 제작사 검색 결과가 비어 있지 않을 때만 "제작사 결과" 헤더와 제작사 섹션을 추가한다. 따라서 비영화 아이템은 1개 또는 3개다. #17 은 이를 3으로 고정했다. 1개일 때 영화 position 이 2 작게 계산돼 첫 영화는 row -1(top 여백 31px 추가), 두 번째 영화는 column -1(left = -15px)이 된다.

## 6. 수정 (브랜치 `fix/movie-item-decoration-header-count`, 커밋 `e810d4f`)

- `MovieItemDecoration.getItemOffsets` 에서 어댑터 앞쪽의 비영화 뷰 타입 개수를 세어 쓴다. 헤더류는 모든 화면에서 목록 맨 앞에만 온다(검색 ≤3, 홈·영화 목록 1, 상세 유사 0).
- 생성자 인자 `numberOfNotMovieItem` 을 없애고 `SearchActivity`·`DetailActivity` 호출을 원래 형태로 돌렸다.
- 3 files changed, 13 insertions(+), 6 deletions(-)

수정 후 측정:

| 화면 | 측정 (px) | 판정 |
|---|---|---|
| 홈 | 열 31, 행 31 | 정상 |
| 검색 `ratatouille` | 첫 행 top 412 두 아이템 모두, x 32–524 / 555–1048, 열 31, 행 31 | 정상 |
| 검색 `whiplash` | 첫 행 top 1091, 스크롤 뒤·복귀 뒤 열 31·행 31 | 정상 |
| 상세 유사 영화 | 열 31, 행 31. 재진입 후 동일 | 정상 |

`adb logcat | grep -c "FATAL EXCEPTION"` → 0.

스크린샷: [홈](img/movie_item_decoration/F_home.jpg), [검색 ratatouille](img/movie_item_decoration/F_search_ratatouille.jpg), [검색 whiplash](img/movie_item_decoration/F_search_whiplash.jpg), [상세 유사 영화](img/movie_item_decoration/F_detail_similar.jpg), [상세 재진입](img/movie_item_decoration/F_detail_similar_reentry.jpg)

## 7. 이번에 고치지 않은 것 (범위 밖, 판단 필요)

| # | 증상 | 근거 | 제안 |
|---|---|---|---|
| 1 | 마지막 행 하단 여백(outerMargin 25dp)이 대부분 빠진다 | `lastRow = (itemCount - 1) / spanCount` 가 헤더를 포함한 전체 개수로 계산됨. whiplash 마지막 아이템 bottom 2311, RecyclerView bottom 2337(26px). #17 이전부터 있던 식 | 같은 함수에서 `itemCount - 앞쪽 비영화 개수` 로 바꾸면 된다. 별도 커밋 또는 이 브랜치 포함 여부 결정 필요 |
| 2 | **인기 영화 상세 화면이 열리지 않는다** (라따뚜이 2062, 위플래쉬 244786 등) | `NumberFormatException: Expected an int but was 3.334 … $.images.posters[0].vote_average`. TMDB 가 이 필드를 실수로 준다. 모델이 `Int` | 모델 필드를 `Double` 로. 별도 이슈 필요 (운영 서버 복구 시 실사용자도 같은 결함을 겪는다) |
| 3 | 상세 `fetch()` 의 한 단계가 실패하면 뒤 단계(credits → providers → 유사 영화)가 모두 생략된다 | 재진입 1회차에서 `watch/providers` 500(서버 컨테이너 DNS `getaddrinfo` 오류, 8초) → `WatchProviderNotFoundException` → 유사 영화 요청 없음. 2회차는 정상 | #3(관측성·에러 처리) 계획에 포함 |
| 4 | PM2 `watch: true` 가 파일 3개 동시 변경 시 `Callback was already called` 로 pm2-runtime 자체가 죽고, restart 정책이 없어 컨테이너가 내려간 채로 남았다 | §4 임시 변경 적용 중 관측 | C 단계(`restart: unless-stopped`)에서 다룬다. 운영에서 watch 를 끌지도 C 보고서에 적는다 |
| 5 | 에뮬레이터 DNS 가 불안정해 포스터가 Picasso HTTP 504 로 비었다 | `svc wifi/data` 재설정 뒤 정상 | 환경 문제, 조치 없음 |

## 8. 승인 요청

1. 브랜치 `fix/movie-item-decoration-header-count` push 와 PR 생성
2. §7-1 을 같은 브랜치에 넣을지 여부
3. §7-2 를 새 이슈로 등록할지 여부
4. 이 보고서의 이슈 번호(파일명) 지정
