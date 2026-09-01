from pathlib import Path
r = Path(__file__).resolve().parents[1]
manifest = (r/'app/src/main/AndroidManifest.xml').read_text()
gradle = (r/'app/build.gradle.kts').read_text()
workflow = (r/'.github/workflows/android-debug.yml').read_text()
fg = r/'app/src/main/res/drawable-nodpi/ic_launcher_foreground.png'
adaptive = (r/'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml').read_text()
adaptive_round = (r/'app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml').read_text()
checks = {
    'Version026Contract': 'versionName = "0.26"' in gradle and 'versionCode = 2600' in gradle,
    'ManifestLauncherIconContract': 'android:icon="@mipmap/ic_launcher"' in manifest,
    'ManifestRoundIconContract': 'android:roundIcon="@mipmap/ic_launcher_round"' in manifest,
    'AdaptiveIconContract': '@drawable/ic_launcher_foreground' in adaptive and '@color/launcher_background' in adaptive,
    'AdaptiveRoundIconContract': '@drawable/ic_launcher_foreground' in adaptive_round,
    'ForegroundAssetPresent': fg.is_file() and fg.stat().st_size > 50_000,
    'AllLegacyDensitiesPresent': all((r/f'app/src/main/res/mipmap-{d}/ic_launcher.png').is_file() for d in ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']),
    'WorkflowArtifactVersioned': 'Atlas-Semaforo-Lite-v0.26-debug' in workflow,
    'NoAccessibilityService': 'AccessibilityService' not in manifest,
}
for k,v in checks.items():
    print(f'{k}: {"PASS" if v else "FAIL"}')
    assert v, k
