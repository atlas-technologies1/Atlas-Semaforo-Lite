from pathlib import Path
r=Path(__file__).resolve().parents[1]
src="\n".join(p.read_text() for p in (r/'app/src/main/java/com/atlas/semaforo').glob('*.kt'))
overlay=(r/'app/src/main/java/com/atlas/semaforo/SemaforoOverlay.kt').read_text()
pipeline=(r/'app/src/main/java/com/atlas/semaforo/OfferFramePipeline.kt').read_text()
parser=(r/'app/src/main/java/com/atlas/semaforo/OfferParser.kt').read_text()
manifest=(r/'app/src/main/AndroidManifest.xml').read_text()
gradle=(r/'app/build.gradle.kts').read_text()
checks={
 'Version024Contract':'versionName = "0.26"' in gradle and 'versionCode = 2600' in gradle,
 'PassengerTripsParsed':'passengerTrips' in parser and 'groupValues?.getOrNull(2)' in parser,
 'RamOnlyServiceMemory':'class ServiceMemoryTracker' in src and 'ActiveService(' in src,
 'IndependentOfferAndActiveOverlays':'offerView' in overlay and 'serviceView' in overlay and 'showActive' in overlay,
 'PipelineKeepsScanning':'serviceMemory.onFrame' in pipeline and 'gate.onOffer' in pipeline,
 'ConservativeAcceptance':'requiredActiveReads: Int = 2' in src and 'acceptanceWindowMs: Long = 7000L' in src,
 'ConservativeFinish':'requiredFinishReads: Int = 2' in src,
 'NoInternetPermission':'android.permission.INTERNET' not in manifest,
 'NoAccessibilityService':'AccessibilityService' not in src+manifest and 'BIND_ACCESSIBILITY_SERVICE' not in manifest,
 'NoUberActionAutomation':all(x not in src for x in ['dispatchGesture','performGlobalAction','ACTION_CLICK','performClick(','input tap']),
 'PassThroughOverlay':'FLAG_NOT_TOUCHABLE' in overlay,
 'EphemeralBitmap':'bitmap.recycle()' in pipeline and 'recognizer.close()' in pipeline,
}
for n,ok in checks.items():
    if not ok: raise SystemExit(f'{n}: FAIL')
    print(f'{n}: PASS')
