package com.atlas.semaforo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ProjectionService.start(this, result.resultCode, data)
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val status = TextView(this).apply {
            text = "Atlas Semáforo Lite v0.16\nSolo informa. No acepta ni rechaza viajes."
            textSize = 18f
        }

        val overlayButton = Button(this).apply {
            text = "1. Autorizar ventana flotante"
            setOnClickListener { requestOverlay() }
        }

        val startButton = Button(this).apply {
            text = "2. Iniciar semáforo"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    requestOverlay()
                    return@setOnClickListener
                }
                requestNotificationPermissionIfNeeded()
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                captureLauncher.launch(manager.createScreenCaptureIntent())
            }
        }

        val stopButton = Button(this).apply {
            text = "Detener semáforo"
            setOnClickListener { ProjectionService.stop(this@MainActivity) }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            addView(status)
            addView(overlayButton)
            addView(startButton)
            addView(stopButton)
        })
    }

    private fun requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
