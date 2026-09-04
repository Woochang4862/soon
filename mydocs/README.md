---
kind: guide
status: active
canonical: mydocs/README.md
last_verified: 2026-09-04
---

# Soon 문서 지도와 canonical manifest

이 문서는 `mydocs/` 의 진입점과 권위 관계를 기록한다. 절차의 정본은 루트 [`AGENTS.md`](../AGENTS.md) 다.

## 시작 순서

1. 루트 `AGENTS.md` 와 `CLAUDE.md` 를 읽는다.
2. `orders/` 최신 파일로 오늘 무엇을 하는지 본다.
3. 이 문서의 manifest 에서 작업 종류에 맞는 canonical 문서를 고른다.

## 메타 규칙

`manual/`, `tech/`, `troubleshootings/` 의 모든 Markdown 과 이 문서는 front matter 를 가진다.

- `kind`: `canonical`, `guide`, `reference`, `investigation`, `decision`, `snapshot`, `memory`, `working` 중 하나
- `status`: `active`, `historical`, `superseded` 중 하나
- `canonical`: 이 문서가 따르는 권위 문서의 저장소 상대 경로
- `last_verified`: 역할·권위 관계·명시된 사실을 마지막으로 확인한 날짜

## 폴더 역할

```
mydocs/
├── orders/           ← 오늘 무엇을 할 것인가      (YYYYMMDD.md, 전월분은 archives/)
├── plans/            ← 어떻게 할 것인가          (task_{issue}.md, _impl.md)
├── working/          ← 어떻게 했는가            (task_{issue}_stage{N}.md)
├── report/           ← 결과가 무엇인가          (task_{issue}_report.md)
├── feedback/         ← 작업지시자가 무엇을 교정했는가 (AI 가 스스로 만들 수 없는 문서)
├── tech/             ← 무엇을 발견했는가        (조사·분석·아키텍처 사실)
└── troubleshootings/ ← 어디서 넘어졌는가        (재현 가능한 증상 → 확정 원인 → 대응 → 검증)
```

`{issue}` 는 모노레포 GitHub 저장소의 이슈 번호다. PR 번호는 쓰지 않는다.

## Canonical manifest

| 경로 | kind | status | canonical | last_verified |
| --- | --- | --- | --- | --- |
| [작업 지침](../AGENTS.md) | canonical | active | `AGENTS.md` | 2026-09-04 |
| [Android 워크플로우 스킬](../.claude/skills/soon-android/SKILL.md) | canonical | active | `.claude/skills/soon-android/SKILL.md` | 2026-09-04 |
| [Soon Journey 모음](../.claude/skills/soon-android/references/journeys-soon.md) | reference | active | `.claude/skills/soon-android/SKILL.md` | 2026-09-04 |
| [유지보수 실패 원인 분석](tech/soon_maintenance_root_cause_analysis.md) | investigation | active | `tech/soon_maintenance_root_cause_analysis.md` | 2026-09-04 |
| [서비스 복구 마스터 계획](plans/soon_recovery_master_plan.md) | canonical | active | `plans/soon_recovery_master_plan.md` | 2026-09-04 |
| [트러블슈팅 지도](troubleshootings/README.md) | guide | active | `troubleshootings/README.md` | 2026-09-04 |
| [피드백 기록 안내](feedback/README.md) | guide | active | `feedback/README.md` | 2026-09-04 |

## 이동 규칙

문서 이동은 독립 커밋에서만 하고 모든 내부 참조를 같은 커밋에서 갱신한다. 완료된 계획서는 최종 보고서 승인 뒤 `plans/archives/` 로 옮긴다.
