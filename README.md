# Atlas Semáforo Lite v0.20

v0.20 consolidates the app-only projection hardening from v0.19 and prevents overlay flicker caused by a single transient OCR mismatch.

Changes:
- prominent centered decision card with strong red/yellow/green background, white border, large title and economics;
- overlay remains fully pass-through (`FLAG_NOT_TOUCHABLE`);
- removed `FLAG_SECURE` so the user can take screenshots of the result;
- self-OCR risk is reduced by not printing a standalone `COP...` fare line nor pickup/trip syntax in the overlay;
- transient OCR loss tolerance increased: gate 8 s, independent overlay failsafe 10 s;
- operational regression fixture added for COP14,027 / 3.3 km pickup / 8.2 km trip / 35 min total.

Privacy and safety remain unchanged: MediaProjection consent is explicit, OCR/captures are ephemeral, no AccessibilityService, and no Uber action automation.

Truth status of this source package must be updated only after an actual Android build and device test.


## v0.19 — App-only MediaProjection resize hardening
- Handles Android 14+ `MediaProjection.Callback.onCapturedContentResize`.
- Reuses the same `VirtualDisplay` session; never calls `createVirtualDisplay` twice.
- Replaces the `ImageReader` surface when the shared-app region changes size, then calls `VirtualDisplay.resize()` + `setSurface()`.
- Keeps OCR frames ephemeral; no screenshots or OCR text are persisted.
- Overlay remains informational and `FLAG_NOT_TOUCHABLE`.


## v0.20 — Stable confirmed-overlay gate

- A confirmed informational overlay is no longer hidden by one changed/partial OCR read.
- A replacement offer must independently reach the 2-read consensus before replacing the visible decision.
- The old decision still fails closed after 8 seconds without a fresh confirmation, including while unconfirmed candidates keep arriving.
- No AccessibilityService, no Uber action automation, and no screenshot/OCR persistence.
