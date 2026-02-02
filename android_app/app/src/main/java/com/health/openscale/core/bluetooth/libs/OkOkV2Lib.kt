/**
 * Based on https://github.com/AlphaKR93/study/blob/personal/reverse-engineering-okok-v2
 */
package com.health.openscale.core.bluetooth.libs

import kotlin.math.max
import kotlin.math.min

class OkOkV2Lib(
    isMale: Boolean,
    age: Int,
    height: Float,  // cm
) : MonoSensorAnalyzeLib(isMale, age + 1, height) {

    override fun getLBM(weight: Float, impedance: Float) =
        weight - getFatMass(weight, impedance)

    override fun getMuscle(weight: Float, impedance: Float): Float {
        val smm: Float = min(
            max(
                (height * 0.2573f) + (weight * 0.1745f) - (age * 0.0161f) + ((if (isMale) 1f else 0f) * 2.4269f) - (impedance * 0.017f) - 20.2165f,
                20f
            ),
            70f
        )
        return (smm / weight) * 100f
    }

    private fun getMuscleMass(weight: Float, impedance: Float): Float {
        val bodyFat = getBodyFat(weight, impedance)
        if (bodyFat > 45f) {
            return (weight - (weight * 0.05f)) - 4f
        } else if (bodyFat < 5f) {
            return (weight - (weight * 0.05f)) - 1f
        }

        val c: Float
        val f: Float
        if (isMale) {
            c = 15.7665f
            f = (height * 0.2867f) + (weight * 0.3894f) - (age * 0.0408f) - (impedance * 0.01235f)
        } else {
            c = 16.4556f
            f = (height * 0.3186f) + (weight * 0.1934f) - (age * 0.0206f) - (impedance * 0.0132f)
        }

        val raw = f - c
        return min(max(raw, 20f), 70f)
    }

    override fun getWater(weight: Float, impedance: Float): Float {
        if (age <= 17) return 0f

        val f: Float
        val c: Float
        if (isMale) {
            c = 0.097f
            f = (height * 0.0939f) + (weight * 0.3758f) - (age * 0.0032f) - (impedance * 0.006925f)
        } else {
            c = 0.5175f
            f = (height * 0.0877f) + (weight * 0.2973f) - (age * 0.0128f) - (impedance * 0.00603f)
        }

        val water = (f + c) / weight * 100f
        return min(max(water, 20f), 85f)
    }

    fun getWaterMass(weight: Float, impedance: Float): Float {
        if (age <= 17) return 0f
        return (getWater(weight, impedance) / 100f) * weight
    }

    override fun getBoneMass(weight: Float, impedance: Float): Float {
        val fm = weight - getFatMass(weight, impedance) - getMuscleMass(weight, impedance)
        return min(max(fm, 1f), 4f)
    }

    override fun getVisceralFat(weight: Float, impedance: Float): Float {
        if (age <= 17) return 0f

        val c: Float
        val f: Float
        if (isMale) {
            c = 13.9871f
            f = (height * -0.2675f) + (weight * 0.42f) + (age * 0.1462f) + (impedance * 0.0123f)
        } else {
            c = 12.3445f
            f = (height * -0.1651f) + (weight * 0.2628f) + (age * 0.0649f) + (impedance * 0.0024f)
        }

        return min(max(f + c, 1f), 60f)
    }

    private fun getFatMass(weight: Float, impedance: Float): Float {
        val f: Float
        val c: Float

        if (isMale) {
            c = 22.554f
            f = (height * -0.3315f) + (weight * 0.6216f) + (age * 0.0183f) + (impedance * 0.0085f)
        } else {
            c = 22.7193f
            f = (height * -0.3332f) + (weight * 0.7509f) + (age * 0.0196f) + (impedance * 0.0072f)
        }

        return f + c
    }

    override fun getBodyFat(weight: Float, impedance: Float): Float =
        min(max(getFatMass(weight, impedance) / weight * 100f, 5f), 45f)

    fun getObesity(weight: Float): Float {
        val ideal = getIdealWeight()
        if (ideal == 0f) return 0f
        return (weight - ideal) / ideal * 100f
    }

    fun getBMR(weight: Float): Float {
        return if (isMale) {
            66f + (weight * 13.7f) + (height * 5f) - (age * 6.8f)
        } else {
            655f + (weight * 9.6f) + (height * 1.8f) - (age * 4.7f)
        }
    }

    fun getBodyAge(weight: Float, impedance: Float): Int {
        if (age <= 17) return 0

        val c: Float
        val f: Float
        if (isMale) {
            c = 54.2267f
            f = (height * -0.7471f) + (weight * 0.9161f) + (age * 0.4184f) + (impedance * 0.0517f)
        } else {
            c = 83.2548f
            f = (height * -1.1165f) + (weight * 1.5784f) + (age * 0.4615f) + (impedance * 0.0415f)
        }

        val bodyAge = (f + c).toInt()
        return min(max(bodyAge, 18), 80)
    }

    override fun getProtein(weight: Float, impedance: Float): Float {
        if (age <= 17) return 0f
        return ((getMuscleMass(weight, impedance) - getWaterMass(weight, impedance)) / weight * 100f)
    }

    fun getIdealWeight(): Float {
        return if (isMale) {
            0.7f * (height - 80f)
        } else {
            0.6f * (height - 70f)
        }
    }

    // Copied from original implementation
    // I don't know what the f**k is this formula supposed to calculate
    fun getScore(weight: Float, impedance: Float): Float {
        if (age <= 17) return 0f

        val bodyFat = getBodyFat(weight, impedance)
        val muscleMass = getMuscleMass(weight, impedance)
        val viscera = getVisceralFat(weight, impedance)

        val f6: Float
        val f7: Float
        val f8: Float
        val f9: Float
        var f10 = 0.0f
        var f11 = 100.0f
        val f12 = (weight / (height * height)) * 100.0f * 100.0f
        var f13 = (((f12 * f12) * -5.686f) + (f12 * 241.7f)) - 2470.0f
        var f14 = 55.0f
        if (f13 < 55.0f) {
            f13 = 55.0f
        }
        if (f13 > 96.0f) {
            f13 = 96.0f
        }
        val f15 = bodyFat + (age * 0.03f)
        if (isMale) {
            val f16 = f15 * f15
            val f17 = f16 * f15
            f7 =
                ((((f17 * f15) * 1.085E-4f) - (f17 * 0.003181f)) - (f16 * 0.2952f)) + (f15 * 10.85f) + 0.4248f
            f6 = 0.77f
        } else {
            val f18 = f15 * f15
            val f19 = f18 * f15
            f7 =
                (((((f19 * f15) * 2.469E-4f) - (f19 * 0.02788f)) + (f18 * 0.9597f)) - (f15 * 10.02f)) + 80.42f
            f6 = 0.735f
        }
        val f20 = weight * f6
        if ((f7.toDouble()) >= 55.0) {
            f14 = f7
        }
        val f21 = (muscleMass + 90.0f) - f20
        if (f21 <= 100.0f) {
            f11 = f21
        }
        var f22 = -50.0f
        if ((viscera.toDouble()) >= 15.0) {
            f8 = -50.0f
        } else {
            val f23 = viscera * viscera
            val f24 = f23 * viscera
            val f25 = f24 * viscera
            f8 =
                (((((f25 * viscera) * 0.007212f) - (f25 * 0.2825f)) + (f24 * 3.912f)) - (f23 * 22.27f)) + (30.38f * viscera) + 89.35f
        }
        if (f8 >= -50.0f) {
            f22 = f8
        }
        if (viscera == 0.0f) {
            f9 = 0.48f
        } else {
            f9 = 0.4f
            f10 = 0.08f
        }
        var i2 = ((f9 * f13) + (0.4f * f14) + (0.1f * f11) + (f10 * f22)).toInt()
        if (i2 < 45) {
            i2 = 45
        }
        if (i2 > 100) {
            i2 = 100
        }
        return i2.toFloat()
    }

    fun getBodyFatObesity(weight: Float): Float {
        val ideal = getIdealWeight()
        return (weight - ideal) / ideal * 100f
    }

    fun getProteinObesity(weight: Float, impedance: Float): Float {
        return 100f - getBodyFat(weight, impedance) - getWater(weight, impedance) - ((if (isMale) 3f else 2.5f) / weight * 100f)
    }

}
