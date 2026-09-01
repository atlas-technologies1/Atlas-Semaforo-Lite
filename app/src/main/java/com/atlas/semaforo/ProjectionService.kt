package com.atlas.semaforo

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class ProjectionService : Service() {
    companion object {
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_DATA = "resultData"
        private const val ACTION_START = "com.atlas.semaforo.START"
        private const val CHANNEL = "atlas_semaforo_projection"
        private const val NOTIFICATION_ID = 6109

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, ProjectionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, resultData)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProjectionService::class.java))
        }
    }

    private var projection: MediaProjection? = null
    private var callback: MediaProjection.Callback? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private var pipeline: OfferFramePipeline? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START || projection != null) return START_NOT_STICKY

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentTitle("Atlas Semáforo activo")
            .setContentText("Análisis local e informativo")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val ht = HandlerThread("AtlasCapture").also { it.start() }
        thread = ht
        val handler = Handler(ht.looper)

        val overlay = SemaforoOverlay(this)
        pipeline = OfferFramePipeline(overlay)

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = manager.getMediaProjection(resultCode, data)
        projection = p

        val cb = object : MediaProjection.Callback() {
            override fun onStop() = stopSelf()
        }
        callback = cb
        p.registerCallback(cb, handler)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = if (Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            android.graphics.Rect().also {
                val point = android.graphics.Point()
                wm.defaultDisplay.getRealSize(point)
                it.set(0, 0, point.x, point.y)
            }
        }

        val width = bounds.width()
        val height = bounds.height()
        val density = resources.displayMetrics.densityDpi
        val ir = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader = ir

        ir.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            try { pipeline?.onFrame(image) } finally { image.close() }
        }, handler)

        display = p.createVirtualDisplay(
            "AtlasSemaforoCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            ir.surface, null, handler
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        pipeline?.close()
        pipeline = null

        display?.release()
        display = null

        reader?.close()
        reader = null

        val p = projection
        val cb = callback
        if (p != null && cb != null) {
            try { p.unregisterCallback(cb) } catch (_: Throwable) {}
        }
        callback = null
        try { p?.stop() } catch (_: Throwable) {}
        projection = null

        thread?.quitSafely()
        thread = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL, "Atlas Semáforo", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
