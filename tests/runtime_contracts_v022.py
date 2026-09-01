from pathlib import Path
root = Path(__file__).resolve().parents[1]
gate = (root/'app/src/main/java/com/atlas/semaforo/StableDecisionGate.kt').read_text()
overlay = (root/'app/src/main/java/com/atlas/semaforo/SemaforoOverlay.kt').read_text()
parser = (root/'app/src/main/java/com/atlas/semaforo/OfferParser.kt').read_text()
gradle = (root/'app/build.gradle.kts').read_text()
main = (root/'app/src/main/java/com/atlas/semaforo/MainActivity.kt').read_text()
manifest = (root/'app/src/main/AndroidManifest.xml').read_text()

checks = {
    'FastPathContract': 'fastPathConfidence: Int = 95' in gate and 'offer.confidence >= fastPathConfidence' in gate,
    'QuickDisappearContract': 'noOfferGraceMs: Long = 1200' in gate,
    'IndependentFailsafeContract': 'staleAfterMs: Long = 3000L' in overlay,
    'StandaloneRatingContract': 'ratingStandalone' in parser,
    'Version023Contract': 'versionCode = 2600' in gradle and 'versionName = "0.26"' in gradle,
    'DynamicUiVersionContract': 'BuildConfig.VERSION_NAME' in main,
    'NoAccessibilityService': 'AccessibilityService' not in manifest,
    'OverlayPassThrough': 'FLAG_NOT_TOUCHABLE' in overlay,
}
for k,v in checks.items():
    assert v, k
    print(f'{k}: PASS')
