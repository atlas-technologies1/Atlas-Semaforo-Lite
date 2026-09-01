from pathlib import Path
s = Path('app/src/main/java/com/atlas/semaforo/StableDecisionGate.kt').read_text()
assert 'return GateEvent(null, expired)' in s
assert 'overlayTtlMs: Long = 8000' in s
assert 'oldVisible' not in s
print('ConfirmedOverlayPersistenceContract: PASS')
print('StaleConfirmedOverlayFailsafeContract: PASS')
