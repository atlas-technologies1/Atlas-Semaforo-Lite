from pathlib import Path
r=Path(__file__).resolve().parents[1]
engine=(r/'app/src/main/java/com/atlas/semaforo/SemaforoEngine.kt').read_text()
store=(r/'app/src/main/java/com/atlas/semaforo/PolicyStore.kt').read_text()
ui=(r/'app/src/main/java/com/atlas/semaforo/MainActivity.kt').read_text()
overlay=(r/'app/src/main/java/com/atlas/semaforo/SemaforoOverlay.kt').read_text()
gradle=(r/'app/build.gradle.kts').read_text()
checks={
'Version025Contract':'versionName = "0.26"' in gradle and 'versionCode = 2600' in gradle,
'WeightedScore45_45_10':'0.45 * kmScore' in engine and '0.45 * hourScore' in engine and '0.10 * ratingContribution' in engine,
'HardFloorKmConfig':'hardFloorCopPerKm' in engine and 'floor_cop_km' in store and 'COP/km piso duro' in ui,
'HardFloorHourConfig':'hardFloorCopPerHour' in engine and 'floor_cop_hour' in store and 'COP/h piso duro' in ui,
'ScoreThresholds':'score >= 80' in engine and 'score >= 60' in engine,
'CompensationReason':'hora compensa km' in engine and 'km compensa hora' in engine,
'OverlayExplainsScore':'economicScore' in overlay and 'decision.components.km' in overlay and 'decision.components.hour' in overlay,
'NoAccessibilityService':'AccessibilityService' not in '\n'.join(p.read_text(errors='ignore') for p in (r/'app/src').rglob('*') if p.is_file()),
}
for k,v in checks.items():
 print(f'{k}: {"PASS" if v else "FAIL"}')
 assert v,k
