---
kind: guide
status: active
canonical: mydocs/troubleshootings/README.md
last_verified: 2026-09-04
---

# 트러블슈팅 문서 지도

재현 가능한 증상에 대해 **확정된 원인, 적용한 대응, 검증 방법**을 보존한다. 새 작업에서는 관련 증상이 있는지 먼저 확인한다.

문서 하나의 형식:

```markdown
---
kind: investigation
status: active
canonical: mydocs/troubleshootings/README.md
last_verified: YYYY-MM-DD
---
# <증상 한 줄>
## 증상        — 무엇이 어떻게 보였나 (로그·스크린샷·명령 출력)
## 재현        — 명령 또는 Journey
## 원인        — 확정된 것만. 가설은 "가설" 이라고 표시
## 대응        — 바꾼 파일과 이유
## 검증        — 돌린 명령과 출력
## 재발 방지   — 테스트·CI·모니터 중 무엇을 남겼나
```

## 항목

- [`android layout` 이 같은 버전끼리 version mismatch 로 실패](android_cli_layout_version_mismatch.md) (2026-09-04, CLI 결함, 폴백 명령 포함)
