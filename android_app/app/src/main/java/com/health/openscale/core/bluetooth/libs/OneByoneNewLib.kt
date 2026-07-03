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


// This class is similar to OneByoneLib, but the way measures are computer are slightly different
@Suppress("detekt:MagicNumber")
class OneByoneNewLib(
    user: ScaleUser,
    weightKg: Float,
    impedance: Float
) : OneByoneLib(user, weightKg, impedance) {
    override val lbmKg: Float by lazy {
        var lbmCoeff = heightCm * heightCm * 9.058E-4f
        lbmCoeff += 12.226f
        lbmCoeff += (weightKg * 0.32f)
        lbmCoeff -= (impedance * 0.0068f)
        lbmCoeff -= (age * 0.0542f)
        lbmCoeff
    }
    override val bcmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bmrKcal: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    val bmmrCoeff: Float = (if (isMale) when {
        age < 13 -> 36
        age < 16 -> 30
        age < 18 -> 26
        age < 30 -> 23
        age < 50 -> 21
        else -> 20
    } else when {
        age < 13 -> 34
        age < 16 -> 29
        age < 18 -> 24
        age < 30 -> 22
        age < 50 -> 20
        else -> 19
    }).toFloat()

    val bmmr: Float by lazy {
        var bmmr: Float
        if (isMale) {
            bmmr = (weightKg * 14.916f + 877.8f) - heightCm * 0.726f
            bmmr -= (age * 8.976f)
        } else {
            bmmr = (weightKg * 10.2036f + 864.6f) - heightCm * 0.39336f
            bmmr -= (age * 6.204f)
        }

        bmmr.coerceIn(500f, 1000f)
    }

    override val bodyFatPercent: Float by lazy {
        var bodyFat = lbmKg

        val bodyFatConst: Float
        if (!isMale) {
            if (age < 0x32) {
                bodyFatConst = 9.25f
            } else {
                bodyFatConst = 7.25f
            }
        } else {
            bodyFatConst = 0.8f
        }

        bodyFat -= bodyFatConst

        if (!isMale) {
            if (weightKg < 50) {
                bodyFat *= 1.02.toFloat()
            } else if (weightKg > 60) {
                bodyFat *= 0.96.toFloat()
            }

            if (heightCm > 160) {
                bodyFat *= 1.03.toFloat()
            }
        } else {
            if (weightKg < 61) {
                bodyFat *= 0.98.toFloat()
            }
        }

        100 * (1 - bodyFat / weightKg)
    }

    override val boneMassKg: Float by lazy {
        var boneMassConst: Float = if (isMale) {
            0.18016894f
        } else {
            0.245691014f
        }

        boneMassConst = lbmKg * 0.05158f - boneMassConst
        val boneMass: Float = if (boneMassConst <= 2.2) {
            boneMassConst - 0.1f
        } else {
            boneMassConst + 0.1f
        }

        boneMass.coerceIn(0.5f, 8f)
    }

    val muscleMassKg: Float = (weightKg - bodyFatPercent * 0.01f * weightKg - boneMassKg).coerceIn(10f, 120f)

    override val musclePercent: Float by lazy {
        var skeletonMuscleMass = waterPercent * weightKg
        skeletonMuscleMass *= 0.8422f * 0.01f
        skeletonMuscleMass -= 2.9903f
        skeletonMuscleMass.percent
    }

    override val visceralFatPercent: Float by lazy {
        val visceralFat: Float
        if (isMale) {
            if (heightCm < weightKg * 1.6 + 63.0) {
                visceralFat =
                    age * 0.15f + ((weightKg * 305.0f) / ((heightCm * 0.0826f * heightCm - heightCm * 0.4f) + 48.0f) - 2.9f)
            } else {
                visceralFat =
                    age * 0.15f + (weightKg * (heightCm * -0.0015f + 0.765f) - heightCm * 0.143f) - 5.0f
            }
        } else {
            if (weightKg <= heightCm * 0.5 - 13.0) {
                visceralFat =
                    age * 0.07f + (weightKg * (heightCm * -0.0024f + 0.691f) - heightCm * 0.027f) - 10.5f
            } else {
                visceralFat =
                    age * 0.07f + ((weightKg * 500.0f) / ((heightCm * 1.45f + heightCm * 0.1158f * heightCm) - 120.0f) - 6.0f)
            }
        }

        visceralFat.coerceIn(1f, 50f)
    }

    override val waterPercent: Float by lazy {
        var waterPercentage = (100 - bodyFatPercent) * 0.7f
        if (waterPercentage > 50) {
            waterPercentage *= 0.98f
        } else {
            waterPercentage *= 1.02f
        }

        waterPercentage.coerceIn(35f, 75f)
    }

    override val proteinPercent: Float =
        ((100.0f - bodyFatPercent) - waterPercent * 1.08f) - boneMassKg.percent
}
