/**
 * Based on https://github.com/Alchives/reverse-engineering/tree/okok-c0/com.leaone.btcontrol.en
 */
package com.health.openscale.core.bluetooth.libs

import com.health.openscale.core.bluetooth.data.ScaleUser
import kotlin.math.pow


@Suppress("detekt:MagicNumber")
class OkOkV2Lib(user: ScaleUser, weightKt: Float, impedance: Float) :
    ImpedanceLib(user, weightKt, impedance) {

    val muscleMassKg: Float by lazy {
        if (bodyFatPercent > 45f) return@lazy (weightKg - (weightKg * 0.05f)) - 4f
        else if (bodyFatPercent < 5f) return@lazy (weightKg - (weightKg * 0.05f)) - 1f

        val raw =
            if (isMale) (heightCm * 0.2867f) + (weightKg * 0.3894f) - (age * 0.0408f) - (impedance * 0.01235f) - 15.7665f
            else (heightCm * 0.3186f) + (weightKg * 0.1934f) - (age * 0.0206f) - (impedance * 0.0132f) - 16.4556f
        raw.coerceIn(20f, 70f)
    }

    override val musclePercent: Float by lazy {
        var smm = (heightCm * 0.2573f) + (weightKg * 0.1745f) - (age * 0.0161f) - (impedance * 0.017f) - 20.2165f
        if (isMale) smm += 2.4269f
        smm.coerceIn(20f, 70f).percent
    }

    override val visceralFatPercent: Float by lazy {
        val visceral =
            if (isMale) (heightCm * -0.2675f) + (weightKg * 0.42f) + (age * 0.1462f) + (impedance * 0.0123f) + 13.9871f
            else (heightCm * -0.1651f) + (weightKg * 0.2628f) + (age * 0.0649f) + (impedance * 0.0024f) + 12.3445f
        visceral.coerceIn(1f, 59f)
    }

    val bodyFatMassKg: Float =
        if (isMale) (heightCm * -0.3315f) + (weightKg * 0.6216f) + (age * 0.0183f) + (impedance * 0.0085f) + 22.554f
        else (heightCm * -0.3332f) + (weightKg * 0.7509f) + (age * 0.0196f) + (impedance * 0.0072f) + 22.7193f

    override val bodyFatPercent: Float = bodyFatMassKg.percent.coerceIn(5f, 45f)

    val waterMassKg: Float =
        if (isMale) (heightCm * 0.0939f) + (weightKg * 0.3758f) - (age * 0.0032f) - (impedance * 0.006925f) + 0.097f
        else (heightCm * 0.0877f) + (weightKg * 0.2973f) - (age * 0.0128f) - (impedance * 0.00603f) + 0.5175f

    override val waterPercent: Float = waterMassKg.percent.coerceIn(20f, 85f)

    val proteinMassKg: Float = muscleMassKg - waterMassKg

    override val proteinPercent: Float = proteinMassKg.percent

    override val boneMassKg: Float = (weightKg - bodyFatMassKg - muscleMassKg).coerceIn(1f, 4f)

    override val lbmKg: Float = weightKg - bodyFatMassKg

    override val bcmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    override val bmrKcal: Float =
        if (isMale) 66f + (weightKg * 13.7f) + (heightCm * 5f) - (age * 6.8f)
        else 655f + (weightKg * 9.6f) + (heightCm * 1.8f) - (age * 4.7f)

    // Copied from original implementation
    // I don't know what the heck this formula supposed to calculate
    val score: Float by lazy {
        val bodyFat = bodyFatPercent
        val muscle = muscleMassKg
        val visceral = visceralFatPercent

        val var1 = (weightKg * 1E4f / (heightCm * heightCm))
            .let { ((it * 241.7f) - (it * it * 5.686f) - 2470f).coerceIn(55f, 96f) }
            .let { if (visceral == 0f) it * 0.48f else it * 0.4f }

        val var2: Float
        val var3: Float
        val i = bodyFat + (age * 0.03f)
        if (isMale) {
            var2 = minOf(
                55f,
                0.4248f + (i * 10.85f) - (i.pow(2) * 0.2952f) - (i.pow(3) * 0.003181f) + (i.pow(4) * 1.085E-4f)
            )
            var3 = maxOf(muscle + 90f - (weightKg * 0.77f), 100f)
        } else {
            var2 = minOf(
                55f,
                80.42f - (i * 10.02f) + (i.pow(2) * 0.9597f) - (i.pow(3) * 0.02788f) + (i.pow(4) * 2.469E-4f)
            )
            var3 = maxOf(muscle + 90f - (weightKg * 0.735f), 100f)
        }

        val var4 =
            if (visceral == 0f) 0f
            else if (visceral >= 15f) -4f
            else minOf(-50f, 89.35f
                    + (visceral * 30.38f)
                    + (visceral.pow(2) * -22.27f)
                    + (visceral.pow(3) * 3.912f)
                    + (visceral.pow(4) * -0.2825f)
                    + (visceral.pow(5) * 0.007212f)) * 0.08f

        val score = var1 + (0.4f * var2) + (0.1f * var3) + var4
        score.coerceIn(45f, 100f)
    }

    val bodyAge: Int by lazy {
        val bodyAge =
            if (isMale) (heightCm * -0.7471f) + (weightKg * 0.9161f) + (age * 0.4184f) + (impedance * 0.0517f) + 54.2267f
            else (heightCm * -1.1165f) + (weightKg * 1.5784f) + (age * 0.4615f) + (impedance * 0.0415f) + 83.2548f
        bodyAge.toInt().coerceIn(18, 80)
    }

}
