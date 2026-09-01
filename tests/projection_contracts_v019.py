from pathlib import Path
r = Path(__file__).resolve().parents[1]
service = (r/'app/src/main/java/com/atlas/semaforo/ProjectionService.kt').read_text()
overlay = (r/'app/src/main/java/com/atlas/semaforo/SemaforoOverlay.kt').read_text()
build = (r/'app/build.gradle.kts').read_text()

assert 'override fun onCapturedContentResize(width: Int, height: Int)' in service
assert 'vd.resize(requested.width, requested.height, densityDpi)' in service
assert 'vd.setSurface(newReader.surface)' in service
assert service.count('createVirtualDisplay(') == 1
assert 'FLAG_NOT_TOUCHABLE' in overlay
assert 'FLAG_SECURE' not in overlay
assert 'AccessibilityService' not in service + overlay
assert 'performClick' not in service + overlay
assert 'versionName = "0.19"' in build
print('CapturedContentResizeContract: PASS')
print('SingleVirtualDisplayPerTokenContract: PASS')
print('SurfaceReplacementContract: PASS')
print('PassThroughOverlayContract: PASS')
print('NoAccessibilityService: PASS')
print('NoUberActionAutomation: PASS')
