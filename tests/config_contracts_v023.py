from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = (root/'app/src/main/java/com/atlas/semaforo/MainActivity.kt').read_text()
store = (root/'app/src/main/java/com/atlas/semaforo/PolicyStore.kt').read_text()
service = (root/'app/src/main/java/com/atlas/semaforo/ProjectionService.kt').read_text()
engine = (root/'app/src/main/java/com/atlas/semaforo/SemaforoEngine.kt').read_text()
gradle = (root/'app/build.gradle.kts').read_text()
manifest = (root/'app/src/main/AndroidManifest.xml').read_text()

checks = {
    'Version024InheritedConfigContract': 'versionName = "0.26"' in gradle and 'versionCode = 2600' in gradle,
    'LocalPersistentPolicyContract': 'getSharedPreferences("atlas_semaforo_policy"' in store and '.apply()' in store,
    'PolicyLoadedAtServiceStartContract': 'PolicyStore(this).load()' in service and 'SemaforoEngine(policy)' in service,
    'ConfigurableKmContract': 'minimumCopPerKm' in engine and 'excellentCopPerKm' in engine and 'COP/km mínimo' in main,
    'ConfigurableHourContract': 'minimumCopPerHour' in engine and 'excellentCopPerHour' in engine and 'COP/h mínimo' in main,
    'ConfigurableRatingContract': 'ratingEnabled' in engine and 'Rating mínimo' in main and 'Rating excelente' in main,
    'ResetDefaultsContract': 'Restaurar recomendados' in main and 'PolicyStore(this@MainActivity).reset()' in main,
    'NoInternetPermission': 'android.permission.INTERNET' not in manifest,
    'NoAccessibilityService': 'AccessibilityService' not in manifest and 'BIND_ACCESSIBILITY_SERVICE' not in manifest,
    'NoUberActionAutomation': all(x not in (main+service) for x in ['dispatchGesture', 'performGlobalAction', 'ACTION_CLICK'])
}
for name, ok in checks.items():
    if not ok:
        raise SystemExit(f'{name}: FAIL')
    print(f'{name}: PASS')
