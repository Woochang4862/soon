---
kind: investigation
status: active
canonical: mydocs/troubleshootings/README.md
last_verified: 2026-09-04
---

# `android layout` 이 "Server version mismatch: expected X but was X" 로 실패

## 증상

Android CLI 1.0.16251017, 에뮬레이터 `Pixel_API34`(android-34 google_apis arm64) 에서 `android layout` 을 부르면 계측 서버 설치 후 다음을 출력하고 종료한다. 기대값과 실제값이 같은데 불일치라고 한다.

```
Installing layout instrumentation server...
Error connecting to instrumentation server: Server version mismatch: expected 1.0.16251017 but was 1.0.16251017
timestamp=1788398805
```

## 재현

```
android --no-metrics emulator start Pixel_API34
android --no-metrics run --apks android/app/build/outputs/apk/debug/app-debug.apk
android --no-metrics layout --pretty
```

2026-09-04 세 번 재현. `adb uninstall com.android.cli.interact.instrumentation` 후 재시도해도 같다.

## 원인

미확정. 버전 문자열은 같고 `timestamp` 가 함께 찍히는 것으로 보아 CLI 가 빌드 타임스탬프까지 비교하는데 설치된 계측 APK 의 값이 어긋나는 것으로 보인다(가설). CLI 자체 결함으로 판단하며 앱 코드와는 무관하다.

## 대응

`layout` 대신 두 가지 폴백을 쓴다.

1. `android screen capture --annotate -o <png>` 는 정상 동작한다. 번호 라벨을 `android screen resolve` 로 좌표로 바꿔 탭한다.
2. 요소 id·텍스트가 필요하면 uiautomator 를 직접 쓴다.
   ```
   adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml \
     | grep -oE 'resource-id="[^"]+"|content-desc="[^"]+"|text="[^"]+"' | sort -u
   ```

## 검증

2026-09-04 폴백 2 로 홈 화면 요소를 얻었다: content-desc 홈·장르별·관심·설정·검색, resource-id `floatingActionButton`, `company`, `blank`.

## 재발 방지

`android update` 후 `layout` 을 다시 시도하고, 고쳐지면 이 문서를 `historical` 로 바꾼다. 스킬 `soon-android` §5 에 폴백을 적어 두었다.
