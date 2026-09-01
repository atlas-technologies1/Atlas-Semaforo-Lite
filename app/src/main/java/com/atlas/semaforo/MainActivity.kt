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
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var minimumKm: EditText
    private lateinit var excellentKm: EditText
    private lateinit var floorKm: EditText
    private lateinit var minimumHour: EditText
    private lateinit var excellentHour: EditText
    private lateinit var floorHour: EditText
    private lateinit var ratingEnabled: CheckBox
    private lateinit var minimumRating: EditText
    private lateinit var excellentRating: EditText

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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "Atlas Semáforo Lite v${BuildConfig.VERSION_NAME}\nSolo informa. No acepta ni rechaza viajes."
            textSize = 18f
        })

        root.addView(Button(this).apply {
            text = "1. Autorizar ventana flotante"
            setOnClickListener { requestOverlay() }
        })

        root.addView(Button(this).apply {
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
        })

        root.addView(Button(this).apply {
            text = "Detener semáforo"
            setOnClickListener { ProjectionService.stop(this@MainActivity) }
        })

        root.addView(TextView(this).apply {
            text = "\nParámetros personales"
            textSize = 20f
        })

        floorKm = numericField("COP/km piso duro")
        minimumKm = numericField("COP/km mínimo")
        excellentKm = numericField("COP/km excelente")
        floorHour = numericField("COP/h piso duro")
        minimumHour = numericField("COP/h mínimo")
        excellentHour = numericField("COP/h excelente")
        minimumRating = numericField("Rating mínimo")
        excellentRating = numericField("Rating excelente")
        ratingEnabled = CheckBox(this).apply { text = "Usar rating para limitar el semáforo" }

        root.addView(floorKm)
        root.addView(minimumKm)
        root.addView(excellentKm)
        root.addView(floorHour)
        root.addView(minimumHour)
        root.addView(excellentHour)
        root.addView(ratingEnabled)
        root.addView(minimumRating)
        root.addView(excellentRating)

        root.addView(Button(this).apply {
            text = "Guardar parámetros"
            setOnClickListener { savePolicy() }
        })

        root.addView(Button(this).apply {
            text = "Restaurar recomendados"
            setOnClickListener {
                PolicyStore(this@MainActivity).reset()
                loadPolicy()
                Toast.makeText(this@MainActivity, "Valores recomendados restaurados", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
        loadPolicy()
    }

    private fun numericField(label: String): EditText = EditText(this).apply {
        hint = label
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        textSize = 16f
    }

    private fun loadPolicy() {
        val policy = PolicyStore(this).load()
        floorKm.setText(policy.hardFloorCopPerKm.toCleanString())
        minimumKm.setText(policy.minimumCopPerKm.toCleanString())
        excellentKm.setText(policy.excellentCopPerKm.toCleanString())
        floorHour.setText(policy.hardFloorCopPerHour.toCleanString())
        minimumHour.setText(policy.minimumCopPerHour.toCleanString())
        excellentHour.setText(policy.excellentCopPerHour.toCleanString())
        ratingEnabled.isChecked = policy.ratingEnabled
        minimumRating.setText(policy.minimumRating.toString())
        excellentRating.setText(policy.excellentRating.toString())
    }

    private fun savePolicy() {
        val policy = SemaforoPolicy(
            excellentCopPerKm = excellentKm.text.toString().toDoubleOrNull() ?: -1.0,
            minimumCopPerKm = minimumKm.text.toString().toDoubleOrNull() ?: -1.0,
            hardFloorCopPerKm = floorKm.text.toString().toDoubleOrNull() ?: -1.0,
            excellentCopPerHour = excellentHour.text.toString().toDoubleOrNull() ?: -1.0,
            minimumCopPerHour = minimumHour.text.toString().toDoubleOrNull() ?: -1.0,
            hardFloorCopPerHour = floorHour.text.toString().toDoubleOrNull() ?: -1.0,
            ratingEnabled = ratingEnabled.isChecked,
            excellentRating = excellentRating.text.toString().toDoubleOrNull() ?: -1.0,
            minimumRating = minimumRating.text.toString().toDoubleOrNull() ?: -1.0
        )

        if (!PolicyStore(this).save(policy)) {
            Toast.makeText(this, "Revisa los valores: debe cumplirse piso ≤ mínimo ≤ excelente", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Parámetros guardados. Reinicia el semáforo para aplicarlos.", Toast.LENGTH_LONG).show()
    }

    private fun Double.toCleanString(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

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
