# Soon 작업 지침

이 파일은 이 작업 공간에서 재현 가능한 작업 부트스트랩이다. 세부 절차는 아래 권위 문서를 우선한다.
방식의 출처는 edwardkim/rhwp 의 Hyper-Waterfall 문서 체계다. 그 저장소의 규모(729+ 문서)를 흉내내는 것이 목적이 아니라, **세션이 끊겨도 기억이 이어지고, 사람의 판단이 보존되고, 방향 오류를 코드가 쌓이기 전에 잡는** 세 가지 효과를 얻는 것이 목적이다.

## 문서 로딩 순서

1. `CLAUDE.md`
2. `mydocs/README.md` — 문서 지도, 폴더 역할, 파일명 규칙
3. `mydocs/orders/` 최신 날짜 파일 — 지금 무엇을 해야 하는가
4. 진행 중 타스크가 있으면 `mydocs/plans/` 의 해당 계획서와 `mydocs/working/` 의 마지막 단계 보고서
5. 새 문제를 만나면 `mydocs/troubleshootings/` 에 같은 증상이 있는지 먼저 본다
6. Android 빌드·실행·검증은 `.claude/skills/soon-android/SKILL.md`

더 구체적인 문서가 이 요약과 다르면 그 문서를 따른다.

## 역할

- **작업지시자(사람)**: 방향, 범위, 우선순위, 품질 기준, 작업 시작과 종료를 결정한다. 실기기 관찰과 사용자 신고 같은 도메인 판단을 제공한다.
- **AI**: 구현, 분석, 검증, 문서화를 맡는다. 스스로 작업 범위를 넓히거나 종료를 선언하지 않는다. "이번 타스크에 포함", "별도 이슈로", "우회 금지" 같은 지시를 그대로 따른다.

## 공통 원칙

- 구현 전에 관련 이슈, 기존 계획서, 트러블슈팅을 확인한다.
- 사용자 또는 다른 도구가 만든 변경은 임의로 되돌리거나 삭제하지 않는다.
- 우회하지 않고 근본 원인을 고친다. 우회가 불가피하면 계획서에 그 사유와 갚을 시점을 적는다.
- 작업 단계가 바뀌면 현재 단계의 변경을 커밋한 뒤 다음 단계를 시작한다.
- **승인 게이트**: 계획서 승인 → 각 단계 보고서 승인 → 최종 보고서 승인 → 이슈 close. 승인 전에는 다음 단계로 넘어가지 않는다.
- **외부로 나가는 행위는 승인 후에만**: GitHub 이슈·코멘트 작성, `git push`, PR 생성, Play Console 업로드, Firebase 콘솔 변경, 메일 발송.
- 검증은 주장이 아니라 실행 결과로 한다. 돌린 명령과 출력을 보고서에 붙인다. 실행하지 않은 것은 "미실행"이라고 쓴다.
- 태도를 문장으로 선언하지 않는다 ("꼼꼼히 확인했습니다" 금지). 사실만 남긴다.

## 타스크 진행 절차

```
1. GitHub Issue 확인 또는 생성 (동일 증상 선행 검색: gh search issues --repo <repo> "<키워드>")
2. 오늘할일(mydocs/orders/YYYYMMDD.md)에 타스크 등록
3. 작업 브랜치 생성 — master 기준 `task_{issue}`
4. 수행계획서 mydocs/plans/task_{issue}.md 작성        [승인]
5. 필요 시 구현계획서 task_{issue}_impl.md              [승인]
6. 3~6단계로 나눠 구현. 단계마다 mydocs/working/task_{issue}_stage{N}.md 작성 + 커밋   [승인]
7. 최종 보고서 mydocs/report/task_{issue}_report.md — 계획과 결과의 차이를 적는다   [승인]
8. push · PR · 이슈 close                                     [승인]
```

이슈는 모노레포 하나의 GitHub 저장소에서 관리하므로 번호만으로 타스크가 식별된다. 브랜치 이름도 `task_{issue}` 다.

## 이 프로젝트 고유 규칙

- **비밀 취급**: `soon-release-key-store.jks`, `.pem`, `google-services.json`, Firebase 서비스 계정 JSON, `.env` 는 절대 새 위치로 복사하거나 커밋 대상에 넣지 않는다. 서버 저장소의 `public/*.json` 노출 건은 [`mydocs/tech/soon_maintenance_root_cause_analysis.md`](mydocs/tech/soon_maintenance_root_cause_analysis.md) 를 본다.
- **서버 주소**: `android/app/src/main/java/com/lusle/android/soon/model/api/ApiClient.kt` 의 baseUrl 을 로컬 테스트용으로 바꾼 채 커밋하지 않는다.
- **NAS 규칙**: `~/NAS/HADD Science/` 규칙은 이 작업 공간과 무관하다.

## 검증

| 변경 범위 | 최소 게이트 |
|---|---|
| 앱 소스 | `./gradlew :app:assembleDebug` 성공 + 에뮬레이터에서 `android run` 후 관련 Journey PASSED (`.claude/skills/soon-android/references/journeys-soon.md`) |
| 앱 테스트 추가 | `./gradlew :app:testDebugUnitTest` 통과 |
| 서버 소스 | `docker-compose up --build` 로 기동 + `GET /message/checking/server` 200 + 변경 라우트 curl 결과 |
| 문서만 | 링크 대상 파일 존재 확인 |

게이트를 줄이거나 생략하면 그 범위와 사유를 보고서에 적는다.

## 문서 언어

모든 문서는 한국어로 쓴다. 코드 식별자·명령·경로는 원문 그대로 둔다.
