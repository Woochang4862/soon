# Soon

**"요즘 볼 영화가 없다"** 는 일상의 불편에서 시작해, 2019년부터 혼자 만들고 운영해 온 상영예정 영화 개봉 알림 서비스입니다.

[![Google Play](https://img.shields.io/badge/Google_Play-com.lusle.android.soon-3DDC84?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.lusle.android.soon)
[![License](https://img.shields.io/badge/License-PolyForm_Noncommercial_1.0.0-blue)](LICENSE.md)

## 구성

| 경로 | 내용 |
|---|---|
| [`android/`](android/) | Android 앱 (Kotlin/Java, MVVM, Retrofit, FCM) |
| [`server/`](server/) | API 서버 (Node.js Express, MySQL, Redis, FCM, docker-compose) |
| [`mydocs/`](mydocs/) | 작업 기록 — 오늘할일·계획·단계 보고·피드백·기술 조사·트러블슈팅 |

2026-09-04 에 두 저장소([Soon](https://github.com/Woochang4862/soon-legacy-android), [soon-server-side](https://github.com/Woochang4862/soon-legacy-server))의 이력을 보존한 채 이 모노레포로 합쳤습니다.

## 동작 방식

서버가 1분마다 cron 으로 TMDB 의 제작사별 영화 목록을 확인하고, 변경이 감지되면 해당 제작사를 구독한 기기에 Firebase Cloud Messaging 푸시를 보냅니다. 앱은 상영예정·인기 영화 탐색, 제작사 팔로우, 개봉 알림 설정을 제공합니다.

## 개발

- 앱: `cd android && ./gradlew :app:assembleDebug` — 상세는 [`android/README.md`](android/README.md)
- 서버: `cd server && docker-compose up` — 상세는 [`server/README.md`](server/README.md)
- AI 에이전트와 함께 작업할 때의 절차는 [`AGENTS.md`](AGENTS.md), 문서 지도는 [`mydocs/README.md`](mydocs/README.md)

## 현재 상태 (2026-09)

API 서버가 내려가 있어 앱의 목록·검색·알림 기능이 동작하지 않습니다. 복구와 유지보수 체계 재건을 [마스터 계획](mydocs/plans/soon_recovery_master_plan.md)에 따라 진행 중이며, 원인 분석은 [여기](mydocs/tech/soon_maintenance_root_cause_analysis.md)에 있습니다.

## 라이선스

[PolyForm Noncommercial 1.0.0](LICENSE.md). 개인·학습·비영리 목적의 사용과 수정은 허용하고, 상업적 이용(앱 스토어 재배포 포함)은 금지합니다. 상업적 이용은 별도 협의가 필요합니다.
