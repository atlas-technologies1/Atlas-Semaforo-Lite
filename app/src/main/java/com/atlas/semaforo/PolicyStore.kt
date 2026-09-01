package com.atlas.semaforo

import android.content.Context

class PolicyStore(context: Context) {
    private val prefs = context.getSharedPreferences("atlas_semaforo_policy", Context.MODE_PRIVATE)

    fun load(): SemaforoPolicy = SemaforoPolicy(
        excellentCopPerKm = prefs.getString("excellent_cop_km", null)?.toDoubleOrNull() ?: 2200.0,
        minimumCopPerKm = prefs.getString("minimum_cop_km", null)?.toDoubleOrNull() ?: 1500.0,
        hardFloorCopPerKm = prefs.getString("floor_cop_km", null)?.toDoubleOrNull() ?: 1200.0,
        excellentCopPerHour = prefs.getString("excellent_cop_hour", null)?.toDoubleOrNull() ?: 35000.0,
        minimumCopPerHour = prefs.getString("minimum_cop_hour", null)?.toDoubleOrNull() ?: 25000.0,
        hardFloorCopPerHour = prefs.getString("floor_cop_hour", null)?.toDoubleOrNull() ?: 22000.0,
        ratingEnabled = prefs.getBoolean("rating_enabled", true),
        excellentRating = prefs.getString("excellent_rating", null)?.toDoubleOrNull() ?: 4.85,
        minimumRating = prefs.getString("minimum_rating", null)?.toDoubleOrNull() ?: 4.60
    ).let { if (it.isValid()) it else SemaforoPolicy() }

    fun save(policy: SemaforoPolicy): Boolean {
        if (!policy.isValid()) return false
        prefs.edit()
            .putString("excellent_cop_km", policy.excellentCopPerKm.toString())
            .putString("minimum_cop_km", policy.minimumCopPerKm.toString())
            .putString("floor_cop_km", policy.hardFloorCopPerKm.toString())
            .putString("excellent_cop_hour", policy.excellentCopPerHour.toString())
            .putString("minimum_cop_hour", policy.minimumCopPerHour.toString())
            .putString("floor_cop_hour", policy.hardFloorCopPerHour.toString())
            .putBoolean("rating_enabled", policy.ratingEnabled)
            .putString("excellent_rating", policy.excellentRating.toString())
            .putString("minimum_rating", policy.minimumRating.toString())
            .apply()
        return true
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}
