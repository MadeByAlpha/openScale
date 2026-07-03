/*
 * openScale
 * Copyright (C) 2025 olie.xdev <olie.xdeveloper@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.health.openscale.core.bluetooth.libs

import com.health.openscale.core.bluetooth.data.ScaleUser
import org.jetbrains.annotations.VisibleForTesting
import kotlin.math.floor


// Based on https://github.com/ronnnnnnnnnnnnn/etekcity_esf551_ble
@Suppress("detekt:MagicNumber")
class EtekcityLib(user: ScaleUser, weight: Float, impedance: Float) : ImpedanceLib(user, weight, impedance) {
    @VisibleForTesting
    internal val bmi: Float = weight * 1E4f / (heightCm * heightCm)

    override val bodyFatPercent: Float by lazy {
        val ageFactor = if (isMale) 0.103f else 0.097f
        val bmiFactor = if (isMale) 1.524f else 1.545f
        val constant = if (isMale) 22.0f else 12.7f
        val raw = floor((ageFactor * age + bmiFactor * bmi - 500f / impedance - constant) * 10f) / 10f
        raw.coerceIn(5f, 75f)
    }

    val fatFreeWeight: Float = weight * (1f - bodyFatPercent / 100f)

    override val visceralFatPercent: Float by lazy {
        val bmiFactor = if (isMale) 0.8666f else 0.8895f
        val bfpFactor = if (isMale) 0.0082f else 0.0943f
        val fatFactor = if (isMale) 0.026f else -0.0534f
        val constant = if (isMale) 14.2692f else 16.215f
        (bmiFactor * bmi + bfpFactor * bodyFatPercent + fatFactor * (weight - fatFreeWeight) - constant)
            .coerceIn(1f, 30f)
    }

    override val waterPercent: Float by lazy {
        val ff1Factor = if (isMale) 0.05f else 0.06f
        val ff2Factor = if (isMale) 0.76f else 0.73f
        val ff1 = maxOf(1f, ff1Factor * fatFreeWeight)
        (ff2Factor * (fatFreeWeight - ff1).percent).coerceIn(10f, 80f)
    }
    override val proteinPercent: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    val basalMetabolicRate: Float = (fatFreeWeight * 21.6f + 370f).coerceIn(900f, 2500f)

    override val musclePercent: Float by lazy {
        val ff1Factor = if (isMale) 0.05f else 0.06f
        val ff2Factor = if (isMale) 0.68f else 0.62f
        val ff1 = maxOf(1f, ff1Factor * fatFreeWeight)
        ff2Factor * (fatFreeWeight - ff1).percent
    }

    override val boneMassKg: Float by lazy {
        val ff1Factor = if (isMale) 0.05f else 0.06f
        maxOf(1f, ff1Factor * fatFreeWeight)
    }
    override val lbmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bcmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bmrKcal: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    val subcutaneousFat: Float by lazy {
        val bfpFactor = if (isMale) 0.965f else 0.983f
        val vfvFactor = if (isMale) 0.22f else 0.303f
        bfpFactor * bodyFatPercent - vfvFactor * visceralFatPercent
    }

    val muscleMass: Float by lazy {
        weight - boneMassKg - 0.01f * bodyFatPercent * weight
    }

    val proteinPercentage: Float by lazy {
        val bfpFactor = if (isMale) 1.0f else 1.05f
        maxOf(5f, 100 - bfpFactor * bodyFatPercent - boneMassKg.percent - waterPercent)
    }

    val weightScore: Int by lazy {
        val heightFactor = if (isMale) 100 else 137
        val constant = if (isMale) 80 else 110
        val factor = if (isMale) 0.7 else 0.45
        val res = factor * (heightFactor * heightCm * 100f - constant)

        if (res <= weight) {
            if (1.3 * res < weight) {
                return@lazy 50
            }
            return@lazy (100 - 50 * (weight - res) / (0.3 * res)).toInt()
        }
        if (res * 0.7 < weight) {
            return@lazy (100 - 50 * (res - weight) / (0.3 * res)).toInt()
        }
        for (x in 0..<6) {
            if (res * x / 10 > weight) {
                return@lazy x * 10
            }
        }
        0
    }

    val fatScore: Int by lazy {
        val constant = if (isMale) 16 else 26
        if (constant < bodyFatPercent) {
            if (bodyFatPercent >= 45) {
                50
            } else {
                (100 - 50 * (bodyFatPercent - constant) / (45 - constant)).toInt()
            }
        } else {
            (100 - 50 * (constant - bodyFatPercent) / (constant - 5)).toInt()
        }
    }

    val bmiScore: Int = when {
        bmi >= 35 -> 50
        bmi >= 22 -> (100 - 3.85 * (bmi - 22)).toInt()
        bmi >= 15 -> (100 - 3.85 * (22 - bmi)).toInt()
        bmi >= 10 -> 40
        bmi >= 5 -> 30
        else -> 20
    }

    val healthScore: Int = (weightScore + fatScore + bmiScore) / 3

    val metabolicAge: Int by lazy {
        val ageAdjustmentFactor = when {
            healthScore < 50 -> 0
            healthScore < 60 -> 1
            healthScore < 65 -> 2
            healthScore < 68 -> 3
            healthScore < 70 -> 4
            healthScore < 73 -> 5
            healthScore < 75 -> 6
            healthScore < 80 -> 7
            healthScore < 85 -> 8
            healthScore < 88 -> 9
            healthScore < 90 -> 10
            healthScore < 93 -> 11
            healthScore < 95 -> 12
            healthScore < 97 -> 13
            healthScore < 98 -> 14
            healthScore < 99 -> 15
            else -> 16
        }
        maxOf(18, age + 8 - ageAdjustmentFactor)
    }
}
