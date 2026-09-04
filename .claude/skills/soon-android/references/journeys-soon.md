# Soon 앱 기본 Journey 모음

`~/.claude/skills/android-cli/references/journeys.md` 프로토콜로 평가한다. 각 `<action>` 을 순서대로, 쓰인 그대로 수행한다.
"Verify/확인" 으로 시작하는 action 은 상호작용 없이 현재 화면만 검사한다. 앱이 죽거나 멈추면 그 지점에서 FAILED.

서버(`34.229.27.130:3000`)가 다운된 상태에서는 J2 이후가 실패하는 것이 **정상 결과**다. 보고서에 서버 상태를 먼저 적는다.

## J1. 콜드 스타트 (서버 무관)

```xml
<journey name="Soon cold start">
  <description>앱이 크래시 없이 첫 화면까지 뜨는지 확인</description>
  <actions>
    <action>Launch the app com.lusle.android.soon</action>
    <action>Verify that the main screen is displayed without a crash dialog</action>
    <action>Verify that a bottom navigation or tab bar is visible</action>
  </actions>
</journey>
```

## J2. 영화 목록 로딩 (서버 필요)

```xml
<journey name="Soon movie list">
  <description>상영예정/인기 영화 목록이 채워지는지, 실패 시 사용자에게 오류가 보이는지 확인</description>
  <actions>
    <action>Launch the app com.lusle.android.soon</action>
    <action>Wait 5 seconds</action>
    <action>Verify that at least one movie poster or title is visible, OR an error message is visible</action>
    <action>Verify that the screen is not blank and no infinite loading spinner is shown</action>
  </actions>
</journey>
```

## J3. 검색 (서버 필요)

```xml
<journey name="Soon search">
  <description>검색 화면 진입과 결과 표시</description>
  <actions>
    <action>Tap the search icon or search tab</action>
    <action>Tap the search input field</action>
    <action>Type "듄"</action>
    <action>Press Enter</action>
    <action>Wait 5 seconds</action>
    <action>Verify that search results are displayed, OR an error message is visible</action>
  </actions>
</journey>
```

## J4. 제작사 알림 설정 (서버 + FCM 필요)

```xml
<journey name="Soon company alarm toggle">
  <description>제작사 알림 토글이 저장되고 다시 열었을 때 유지되는지 확인</description>
  <actions>
    <action>Navigate to the company (제작사) screen</action>
    <action>Tap the first company in the list</action>
    <action>Tap the alarm toggle or bell icon</action>
    <action>Verify that the toggle state changed</action>
    <action>Press the back key</action>
    <action>Tap the same first company again</action>
    <action>Verify that the alarm toggle keeps the changed state</action>
  </actions>
</journey>
```

## J5. 오프라인 처리 (서버 무관, 회귀용)

```xml
<journey name="Soon offline handling">
  <description>네트워크가 끊겼을 때 앱이 죽지 않고 안내를 보여주는지 확인</description>
  <actions>
    <action>Run adb shell svc wifi disable and adb shell svc data disable</action>
    <action>Launch the app com.lusle.android.soon</action>
    <action>Wait 5 seconds</action>
    <action>Verify that the app did not crash</action>
    <action>Verify that an error or offline message is visible, not a blank screen</action>
    <action>Run adb shell svc wifi enable and adb shell svc data enable</action>
  </actions>
</journey>
```

## 결과 기록 형식

```json
{ "journey": "Soon cold start", "server": "DOWN", "results": [
  { "action": "Launch the app", "status": "PASSED", "commands": ["android run --apks ..."], "comment": "" },
  { "action": "Verify main screen", "status": "FAILED", "commands": ["android layout"], "comment": "FATAL EXCEPTION ... (logcat 첨부)" }
]}
```

새 버그를 재현하면 같은 형식의 Journey 를 `android/journeys/<이슈명>.xml` 로 저장하고 여기 목록에 추가한다.
