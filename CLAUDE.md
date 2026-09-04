# CLAUDE.md

이 파일은 Claude Code 가 이 작업 공간의 권위 문서를 찾기 위한 짧은 부트로더다. 작업 절차와 명령을 여기에 중복 기록하지 않는다.

## 로딩 순서

1. 루트의 [`AGENTS.md`](AGENTS.md)
2. [`mydocs/README.md`](mydocs/README.md) — 문서 지도와 canonical manifest
3. 오늘 할일 [`mydocs/orders/`](mydocs/orders/) 의 최신 날짜 파일
4. 각 지도가 지정한 canonical 문서

이 파일과 canonical 문서가 다르면 canonical 문서를 따른다.

## 작업 공간 개요

Soon 은 상영예정 영화 개봉 알림 안드로이드 앱과 그 API 서버다. 2026-09-04 두 저장소를 이 모노레포로 합쳤다(이력 보존).

| 경로 | 내용 | 원격 |
|---|---|---|
| `android/` | Android 앱 (Kotlin/Java, `com.lusle.android.soon`) | 구 github.com/Woochang4862/Soon |
| `server/` | Node.js Express + MySQL + Redis + FCM, docker-compose | 구 github.com/Woochang4862/soon-server-side |

루트가 git 저장소다. `mydocs/` 는 앱과 서버를 함께 다루는 공용 기록이다.

## Android 작업

앱을 빌드·실행·검증할 때는 프로젝트 스킬 [`.claude/skills/soon-android`](.claude/skills/soon-android/SKILL.md) 를 먼저 읽는다. `android` CLI 는 항상 `--no-metrics` 로 부른다.
