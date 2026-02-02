package com.health.openscale.core.bluetooth.libs


abstract class MonoSensorAnalyzeLib internal constructor(
    protected val isMale: Boolean,
    protected val age: Int,
    protected val height: Float,
) {

    fun getBMI(weight: Float): Float {
        // weight [kg], height [cm]
        // BMI = kg / (m^2)
        return weight / (((height * height) / 100.0f) / 100.0f)
    }

    abstract fun getBodyFat(weight: Float, impedance: Float): Float

    abstract fun getWater(weight: Float, impedance: Float): Float

    abstract fun getMuscle(weight: Float, impedance: Float): Float

    abstract fun getVisceralFat(weight: Float, impedance: Float): Float

    abstract fun getProtein(weight: Float, impedance: Float): Float

    abstract fun getBoneMass(weight: Float, impedance: Float): Float

    abstract fun getLBM(weight: Float, impedance: Float): Float

}
