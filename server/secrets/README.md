# secrets/

서비스 계정 키처럼 저장소에 올리면 안 되는 파일을 두는 곳. 이 폴더는 루트 `.gitignore` 로 제외되고, docker-compose 가 컨테이너의 `/soon-server-side/secrets` 에 읽기 전용으로 마운트한다.

| 파일 | 용도 | 어디서 받나 |
|---|---|---|
| `firebase-sa.json` | Firebase Admin SDK 서비스 계정 키. FCM 발송·토픽 구독에 사용 | Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성 |

키가 없으면 서버는 시작 시 `[firebase] …` 오류를 내고 종료한다.
