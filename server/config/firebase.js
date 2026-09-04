// Firebase Admin SDK 초기화의 유일한 진입점.
// 서비스 계정 키는 저장소 밖 파일에서 읽는다. 경로는 GOOGLE_APPLICATION_CREDENTIALS 환경변수.
// 키가 없으면 조용히 실패하지 않고 시작 시점에 종료한다. (#1)
import { readFileSync } from 'fs';
import { config } from 'dotenv';
import admin from 'firebase-admin';

config();

const credentialPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
if (!credentialPath) {
  console.error(
    '[firebase] GOOGLE_APPLICATION_CREDENTIALS 환경변수가 없습니다.\n' +
    '  서비스 계정 키 JSON 경로를 .env 또는 docker-compose 환경변수로 지정하세요.\n' +
    '  예: GOOGLE_APPLICATION_CREDENTIALS=/soon-server-side/secrets/firebase-sa.json'
  );
  process.exit(1);
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(readFileSync(credentialPath, 'utf8'));
} catch (err) {
  console.error(`[firebase] 서비스 계정 키를 읽지 못했습니다: ${credentialPath}\n  ${err.message}`);
  process.exit(1);
}
if (!serviceAccount.client_email || !serviceAccount.private_key) {
  console.error(`[firebase] 서비스 계정 키 형식이 아닙니다 (client_email/private_key 없음): ${credentialPath}`);
  process.exit(1);
}

const credential = admin.credential.cert(serviceAccount);

if (!admin.apps.length) {
  admin.initializeApp({
    credential,
    databaseURL: process.env.FIREBASE_DATABASE_URL || 'https://soon-79c2e.firebaseio.com',
  });
  console.log(`[firebase] 초기화 완료: ${serviceAccount.client_email}`);
}

/** FCM/IID REST 호출용 OAuth2 액세스 토큰 */
export async function getAccessToken() {
  const { access_token } = await credential.getAccessToken();
  return access_token;
}

export { admin };
export default getAccessToken;
