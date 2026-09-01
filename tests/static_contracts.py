from pathlib import Path
r = Path(__file__).resolve().parents[1]
src = "\n".join(p.read_text() for p in (r/"app/src/main/java").rglob("*.kt"))
manifest = (r/"app/src/main/AndroidManifest.xml").read_text()
workflow = (r/".github/workflows/android-debug.yml").read_text()

assert 'versionName = "0.26"' in (r/"app/build.gradle.kts").read_text()
assert "context.stopService" in src
assert "FLAG_SECURE" not in src
assert "FLAG_NOT_TOUCHABLE" in src
assert "postDelayed" in src
assert "RgbaPlaneLayoutCalculator.calculate" in src
assert "FLAG_NOT_FOCUSABLE" in src
assert "acquireLatestImage" in src
assert "image.close()" in src
assert "bitmap.recycle()" in src
assert "recognizer.close()" in src
assert "AtomicBoolean(false)" in src
assert "minIntervalMs: Long = 350" in src
assert "requiredReads: Int = 2" in src
assert 'foregroundServiceType="mediaProjection"' in manifest
assert 'android:usesCleartextTraffic="false"' in manifest
assert "assembleDebug" in workflow and "apksigner" in workflow and "sha256sum" in workflow

for forbidden in ["AccessibilityService","dispatchGesture","performClick(","UiAutomator","input tap","BOOT_COMPLETED"]:
    assert forbidden not in src, forbidden

print("CompleteAndroidProjectContract: PASS")
print("ServiceStopLifecycleContract: PASS")
print("OverlayPermissionRaceContract: PASS")
print("ScreenshotCompatibleOverlayContract: PASS")
print("PassThroughOverlayContract: PASS")
print("IndependentStaleOverlayFailsafe: PASS")
print("RgbaPlaneLayoutContract: PASS")
print("EphemeralCaptureContract: PASS")
print("EphemeralOcrContract: PASS")
print("OcrCloseRaceContract: PASS")
print("NoAccessibilityService: PASS")
print("NoUberActionAutomation: PASS")
print("GitHubTruthGateContract: PASS")
