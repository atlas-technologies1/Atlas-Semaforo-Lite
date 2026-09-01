package com.atlas.semaforo

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class SemaforoOverlay(
    private val context: Context,
    private val staleAfterMs: Long = 4000L
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var view: TextView? = null
    private var generation: Long = 0

    fun show(decision: SemaforoDecision, rating: Double?) {
        main.post {
            if (!Settings.canDrawOverlays(context)) {
                hideInternal()
                return@post
            }

            val symbol = when (decision.band) {
                SemaforoBand.GREEN -> "🟢"
                SemaforoBand.YELLOW -> "🟡"
                SemaforoBand.RED -> "🔴"
            }
            val text = buildString {
                append(symbol)
                append("\n")
                append("%,.0f COP/km".format(decision.metrics.copPerKm))
                append("\n")
                append("%,.0f COP/h".format(decision.metrics.copPerHour))
                append("\nConf. ${decision.confidence}%")
                rating?.let { append("\n★ %.2f".format(it)) }
            }

            val current = view
            if (current != null) {
                current.text = text
            } else {
                val tv = TextView(context).apply {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(0xD0202020.toInt())
                    textSize = 18f
                    setPadding(18, 12, 18, 12)
                    this.text = text
                }

                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SECURE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    x = 24
                    y = 180
                }

                try {
                    wm.addView(tv, lp)
                    view = tv
                } catch (_: Throwable) {
                    view = null
                    return@post
                }
            }

            // Independent fail-safe: even if frames/OCR stop unexpectedly,
            // an old recommendation cannot remain on screen indefinitely.
            val myGeneration = ++generation
            main.postDelayed({
                if (generation == myGeneration) hideInternal()
            }, staleAfterMs)
        }
    }

    fun hide() = main.post {
        generation++
        hideInternal()
    }

    private fun hideInternal() {
        val v = view ?: return
        try { wm.removeView(v) } catch (_: Throwable) {}
        view = null
    }
}
