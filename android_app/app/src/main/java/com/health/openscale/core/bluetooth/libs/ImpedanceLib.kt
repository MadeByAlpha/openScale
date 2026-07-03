package com.health.openscale.core.bluetooth.libs

import com.health.openscale.core.bluetooth.data.ScaleUser


abstract class ImpedanceLib private constructor(
    protected val isMale: Boolean,
    protected val age: Int,
    protected val weightKg: Float,
    protected val heightCm: Float,
    protected val impedance: Float,
) {

    protected constructor(user: ScaleUser, weightKg: Float, impedance: Float) :
            this(user.gender.isMale(), user.age, weightKg, user.bodyHeight, impedance)

    protected val male: Float
        inline get() = if (isMale) 1f else 0f

    abstract val musclePercent: Float
    abstract val visceralFatPercent: Float
    abstract val bodyFatPercent: Float
    abstract val waterPercent: Float
    abstract val proteinPercent: Float
    abstract val boneMassKg: Float

    /**
     * Lean Body Mass in Kg
     */
    abstract val lbmKg: Float

    /**
     * Body Cell Mass in Kg
     */
    abstract val bcmKg: Float

    /**
     * Basal Metabolic Rate in Kcal
     */
    abstract val bmrKcal: Float

    protected val Float.percent: Float
        inline get() = this * 100f / weightKg

}
