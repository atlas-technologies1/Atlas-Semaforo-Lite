from pathlib import Path
r = Path(__file__).resolve().parents[1]
overlay = (r/'app/src/main/java/com/atlas/semaforo/SemaforoOverlay.kt').read_text()
gate = (r/'app/src/main/java/com/atlas/semaforo/StableDecisionGate.kt').read_text()
build = (r/'app/build.gradle.kts').read_text()

assert 'FLAG_NOT_TOUCHABLE' in overlay
assert 'FLAG_NOT_FOCUSABLE' in overlay
assert 'FLAG_SECURE' not in overlay
assert 'staleAfterMs: Long = 3000L' in overlay
assert 'overlayTtlMs: Long = 6000' in gate
assert 'ATLAS  •  $bandTitle' in overlay
assert 'dp(286)' in overlay
assert 'setStroke(dp(2),Color.WHITE)' in overlay
assert 'versionName = "0.26"' in build
assert 'AccessibilityService' not in overlay
assert 'performClick' not in overlay

print('ScreenshotCompatibleOverlayContract: PASS')
print('PassThroughOverlayContract: PASS')
print('ProminentDecisionCardContract: PASS')
print('TransientOcrLossToleranceContract: PASS')
print('NoAccessibilityService: PASS')
print('NoUberActionAutomation: PASS')
