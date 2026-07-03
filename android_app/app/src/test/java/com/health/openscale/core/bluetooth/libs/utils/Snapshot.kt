package com.health.openscale.core.bluetooth.libs.utils

import com.health.openscale.core.bluetooth.data.ScaleUser
import com.health.openscale.core.data.ActivityLevel


@ConsistentCopyVisibility
@JvmRecord
internal data class Snapshot private constructor(
    val user: ScaleUser,
    val impedanceOhms: Float,
    val weightKg: Float,
    val bmi: Float? = null,
    val musclePercent: Float? = null,
    val bodyFatPercent: Float? = null,
    val visceralFatPercent: Float? = null,
    val waterPercent: Float? = null,
    val proteinPercent: Float? = null,
    val boneMassKg: Float? = null,
    val lbmKg: Float? = null,
    val bcmKg: Float? = null,
    val bmrKcal: Float? = null,
    val extras: Map<String, Float>? = null,
) {

    constructor(
        age: Long,
        heightCm: Float,
        isMale: Boolean,
        weightKg: Float,
        impedanceOhms: Float,
        activityLevel: ActivityLevel? = null,
        bmi: Float? = null,
        musclePercent: Float? = null,
        bodyFatPercent: Float? = null,
        visceralFatPercent: Float? = null,
        waterPercent: Float? = null,
        proteinPercent: Float? = null,
        boneMassKg: Float? = null,
        lbmKg: Float? = null,
        bcmKg: Float? = null,
        bmrKcal: Float? = null,
        extras: Map<String, Float>? = null,
    ) : this(
        user(age, heightCm, isMale, activityLevel),
        impedanceOhms = impedanceOhms,
        weightKg = weightKg,
        bmi = bmi,
        musclePercent = musclePercent,
        bodyFatPercent = bodyFatPercent,
        visceralFatPercent = visceralFatPercent,
        waterPercent = waterPercent,
        proteinPercent = proteinPercent,
        boneMassKg = boneMassKg,
        lbmKg = lbmKg,
        bcmKg = bcmKg,
        bmrKcal = bmrKcal,
        extras = extras,
    )

}
