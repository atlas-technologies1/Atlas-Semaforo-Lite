# Atlas Semáforo Lite v0.16

Complete Android source project.

v0.16 closes two runtime risks:
- the informational overlay is now explicitly `FLAG_NOT_TOUCHABLE`, so it cannot consume Uber touches;
- the overlay has an independent 4-second expiration failsafe, so a stale recommendation cannot remain visible if capture/OCR callbacks stop unexpectedly.

It also validates RGBA_8888 ImageReader plane layout before bitmap conversion and fails closed for unexpected pixel stride/row stride.

Truth status is recorded in BUILD_STATUS.json. No AccessibilityService and no Uber action automation.
