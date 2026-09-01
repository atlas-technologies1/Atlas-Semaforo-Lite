package com.atlas.semaforo

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class SemaforoOverlay(
    private val context: Context,
    private val staleAfterMs: Long = 10000L
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var view: LinearLayout? = null
    private var generation: Long = 0

    fun show(decision: SemaforoDecision, rating: Double?) {
        main.post {
            if (!Settings.canDrawOverlays(context)) {
                hideInternal()
                return@post
            }

            val bandTitle = when (decision.band) {
                SemaforoBand.GREEN -> "BUENA"
                SemaforoBand.YELLOW -> "MEDIA"
                SemaforoBand.RED -> "MALA"
            }
            val bandColor = when (decision.band) {
                SemaforoBand.GREEN -> Color.rgb(22, 163, 74)
                SemaforoBand.YELLOW -> Color.rgb(202, 138, 4)
                SemaforoBand.RED -> Color.rgb(220, 38, 38)
            }

            val card = view ?: createCard().also { created ->
                val lp = WindowManager.LayoutParams(
                    dp(286),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(118)
                }

                try {
                    wm.addView(created, lp)
                    view = created
                } catch (_: Throwable) {
                    return@post
                }
            }

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb(245, Color.red(bandColor), Color.green(bandColor), Color.blue(bandColor)))
                setStroke(dp(3), Color.WHITE)
            }
            card.background = bg

            val title = card.getChildAt(0) as TextView
            val metrics = card.getChildAt(1) as TextView
            val footer = card.getChildAt(2) as TextView

            title.text = "ATLAS  •  $bandTitle"
            metrics.text = buildString {
                append("%,.0f  $/km".format(decision.metrics.copPerKm))
                append("   •   ")
                append("%,.0f  $/h".format(decision.metrics.copPerHour))
            }
            footer.text = buildString {
                append("${decision.metrics.totalKm.format1()} km  •  ${decision.metrics.totalMinutes} min")
                rating?.let { append("  •  ★ %.2f".format(it)) }
                append("  •  ${decision.confidence}%")
            }

            // Refresh the independent fail-safe on every valid confirmed decision.
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

    private fun createCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(14), dp(18), dp(14))

            addView(TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 24f
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })

            addView(TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, dp(6), 0, dp(4))
            })

            addView(TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
            })
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun Double.format1(): String = String.format("%.1f", this)

    private fun hideInternal() {
        val v = view ?: return
        try { wm.removeView(v) } catch (_: Throwable) {}
        view = null
    }
}
