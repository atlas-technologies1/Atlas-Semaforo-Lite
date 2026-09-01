package com.atlas.semaforo

fun main() {
    check(ProjectionSurfacePolicy.normalized(1080, 2400) == CaptureSize(1080, 2400))
    check(ProjectionSurfacePolicy.normalized(0, 2400) == null)
    check(ProjectionSurfacePolicy.normalized(1080, -1) == null)
    check(ProjectionSurfacePolicy.normalized(20000, 2400) == null)
    check(!ProjectionSurfacePolicy.needsResize(CaptureSize(1080, 2400), CaptureSize(1080, 2400)))
    check(ProjectionSurfacePolicy.needsResize(CaptureSize(1080, 2400), CaptureSize(1080, 2200)))
    println("ProjectionSurfaceNormalization: PASS")
    println("AppOnlyResizeDecision: PASS")
}
