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
    private val staleAfterMs: Long = 3000L
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var offerView: LinearLayout? = null
    private var serviceView: LinearLayout? = null
    private var generation: Long = 0

    fun show(decision: SemaforoDecision, rating: Double?) = main.post {
        if (!Settings.canDrawOverlays(context)) { hideOfferInternal(); return@post }
        val bandTitle = when (decision.band) { SemaforoBand.GREEN -> "BUENA"; SemaforoBand.YELLOW -> "MEDIA"; SemaforoBand.RED -> "MALA" }
        val bandColor = when (decision.band) { SemaforoBand.GREEN -> Color.rgb(22,163,74); SemaforoBand.YELLOW -> Color.rgb(202,138,4); SemaforoBand.RED -> Color.rgb(220,38,38) }
        val card = offerView ?: createCard().also { created ->
            val lp = baseParams(dp(286), dp(118))
            try { wm.addView(created, lp); offerView = created } catch (_: Throwable) { return@post }
        }
        card.background = cardBackground(bandColor)
        (card.getChildAt(0) as TextView).text = "ATLAS  •  $bandTitle  •  ${decision.economicScore}/100"
        (card.getChildAt(1) as TextView).text = "%,.0f  $/km   •   %,.0f  $/h".format(decision.metrics.copPerKm, decision.metrics.copPerHour)
        (card.getChildAt(2) as TextView).text = buildString {
            append("${decision.metrics.totalKm.format1()} km  •  ${decision.metrics.totalMinutes} min")
            rating?.let { append("  •  ★ %.2f".format(it)) }
            append("  •  ${decision.confidence}%")
            append("\nKm ${decision.components.km}  •  Hora ${decision.components.hour}")
            decision.components.rating?.let { append("  •  Cliente $it") }
            append("  •  ${decision.reason}")
        }
        val mine = ++generation
        main.postDelayed({ if (generation == mine) hideOfferInternal() }, staleAfterMs)
    }

    fun showActive(service: ActiveService) = main.post {
        if (!Settings.canDrawOverlays(context)) { hideActiveInternal(); return@post }
        val card = serviceView ?: createActiveCard().also { created ->
            val lp = baseParams(dp(286), dp(36))
            try { wm.addView(created, lp); serviceView = created } catch (_: Throwable) { return@post }
        }
        card.background = cardBackground(Color.rgb(31, 41, 55))
        (card.getChildAt(0) as TextView).text = "SERVICIO ACTUAL  •  COP ${service.fareCop.formatCop()}"
        (card.getChildAt(1) as TextView).text = buildString {
            service.rating?.let { append("★ %.2f".format(it)) }
            service.passengerTrips?.let { if (isNotEmpty()) append("  •  "); append("$it viajes") }
            if (isEmpty()) append("Datos del cliente no visibles")
        }
    }

    fun hideOffer() = main.post { generation++; hideOfferInternal() }
    fun clearActive() = main.post { hideActiveInternal() }
    fun hideAll() = main.post { generation++; hideOfferInternal(); hideActiveInternal() }

    private fun createCard() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(18),dp(14),dp(18),dp(14))
        addView(text(24f, true)); addView(text(20f, true).apply { setPadding(0,dp(6),0,dp(4)) }); addView(text(14f,false))
    }
    private fun createActiveCard() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(14),dp(10),dp(14),dp(10))
        addView(text(17f,true)); addView(text(14f,false).apply { setPadding(0,dp(3),0,0) })
    }
    private fun text(size: Float, bold: Boolean) = TextView(context).apply {
        setTextColor(Color.WHITE); textSize=size; gravity=Gravity.CENTER
        if (bold) typeface=android.graphics.Typeface.DEFAULT_BOLD
    }
    private fun baseParams(width: Int, yPos: Int) = WindowManager.LayoutParams(
        width, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL; y=yPos }
    private fun cardBackground(color: Int) = GradientDrawable().apply {
        shape=GradientDrawable.RECTANGLE; cornerRadius=dp(20).toFloat()
        setColor(Color.argb(245,Color.red(color),Color.green(color),Color.blue(color))); setStroke(dp(2),Color.WHITE)
    }
    private fun dp(v:Int)=(v*context.resources.displayMetrics.density).toInt()
    private fun Double.format1()=String.format("%.1f",this)
    private fun Int.formatCop()=String.format("%,d",this)
    private fun hideOfferInternal(){ val v=offerView?:return; try{wm.removeView(v)}catch(_:Throwable){}; offerView=null }
    private fun hideActiveInternal(){ val v=serviceView?:return; try{wm.removeView(v)}catch(_:Throwable){}; serviceView=null }
}
