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
import com.health.openscale.core.data.ActivityLevel


@Suppress("detekt:MagicNumber")
open class OneByoneLib(user: ScaleUser, weightKg: Float, impedance: Float) : ImpedanceLib(user, weightKg, impedance) {

    private val peopleType: Int = when (user.activityLevel) {
        // Matches legacy mapping:
        // SEDENTARY/MILD -> 0, MODERATE -> 1, HEAVY/EXTREME -> 2
        ActivityLevel.SEDENTARY, ActivityLevel.MILD -> 0
        ActivityLevel.MODERATE -> 1
        ActivityLevel.HEAVY, ActivityLevel.EXTREME -> 2
    }

    override val musclePercent: Float =
        ((heightCm * heightCm / impedance * 0.401f) + (male * 3.825f) - (age * 0.071f) + 5.102f).percent

    @Suppress("detekt:MaxLineLength")
    override val visceralFatPercent: Float by lazy {
        val visceralFat: Float

        if (isMale) {
            if (heightCm < ((1.6f * weightKg) + 63.0f)) {
                visceralFat =
                    (((weightKg * 305.0f) / (0.0826f * heightCm * heightCm - (0.4f * heightCm) + 48.0f)) - 2.9f) + (age.toFloat() * 0.15f)

                if (peopleType == 0) {
                    return@lazy visceralFat
                } else {
                    return@lazy subVisceralFat_A(visceralFat, peopleType)
                }
            } else {
                visceralFat =
                    ((age.toFloat() * 0.15f) + ((weightKg * (-0.0015f * heightCm + 0.765f)) - heightCm * 0.143f)) - 5.0f

                if (peopleType == 0) {
                    return@lazy visceralFat
                } else {
                    return@lazy subVisceralFat_A(visceralFat, peopleType)
                }
            }
        } else {
            if (((0.5f * heightCm) - 13.0f) > weightKg) {
                visceralFat =
                    ((age.toFloat() * 0.07f) + ((weightKg * (-0.0024f * heightCm + 0.691f)) - (heightCm * 0.027f))) - 10.5f

                if (peopleType != 0) {
                    return@lazy subVisceralFat_A(visceralFat, peopleType)
                } else {
                    return@lazy visceralFat
                }
            } else {
                visceralFat =
                    (weightKg * 500.0f) / (((1.45f * heightCm) + 0.1158f * heightCm * heightCm) - 120.0f) - 6.0f + (age.toFloat() * 0.07f)

                if (peopleType == 0) {
                    return@lazy visceralFat
                } else {
                    return@lazy subVisceralFat_A(visceralFat, peopleType)
                }
            }
        }
    }

    override val bodyFatPercent: Float by lazy {
        var bodyFat: Float =
            12.226f + (9.058E-4f * heightCm * heightCm) - (0.0068f * impedance) - (0.0542f * age)
        bodyFat += 0.32f * heightCm

        bodyFat -= if (isMale) 0.8f else if (age >= 50) 7.25f else 9.25f
        bodyFat *= when (peopleType) {
            0 -> 1f
            1 -> 1.0427f
            else -> 1.0958f
        }

        if (isMale) {
            if (61.0f > weightKg) bodyFat *= 0.98f
        } else {
            if (50.0f > weightKg) bodyFat *= 1.02f
            if (weightKg > 60.0f) bodyFat *= 0.96f
            if (heightCm > 160.0f) bodyFat *= 1.03f
        }

        (100f - bodyFat.percent).coerceIn(1f, 45f)
    }

    override val waterPercent: Float =
        ((100f - bodyFatPercent) * 0.7f).let { it * (if (it < 50f) 1.02f else 0.98f) }

    override val proteinPercent: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    override val boneMassKg: Float by lazy {
        var boneMass: Float =
            12.226f + (9.058E-4f * heightCm * heightCm) - (0.0068f * impedance) - (0.0542f * age)
        boneMass += 0.32f * weightKg

        boneMass -= if (isMale) 3.49305f else 4.76325f
        boneMass *= when (peopleType) {
            0 -> 1.0f
            1 -> 1.0427f
            else -> 1.0958f
            // else -> 0f
        }

        boneMass += if (boneMass <= 2.2f) -0.1f else 0.1f
        boneMass *= 0.05158f

        boneMass.coerceIn(0.5f, 8.0f)
    }

    override val lbmKg: Float = weightKg - (bodyFatPercent * weightKg / 100.0f)
    override val bcmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bmrKcal: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    private companion object {
        @JvmStatic
        @JvmSynthetic
        private fun subVisceralFat_A(visceralFat: Float, peopleType: Int): Float {
            var visceralFat = visceralFat
            if (peopleType != 0) {
                if (10.0f <= visceralFat) {
                    return subVisceralFat_B(visceralFat)
                } else {
                    visceralFat -= 4.0f
                    return visceralFat
                }
            } else {
                if (10.0f > visceralFat) {
                    visceralFat -= 2.0f
                    return visceralFat
                } else {
                    return subVisceralFat_B(visceralFat)
                }
            }
        }

        @JvmStatic
        @JvmSynthetic
        private fun subVisceralFat_B(visceralFat: Float): Float {
            var visceralFat = visceralFat
            if (visceralFat < 10.0f) {
                visceralFat *= 0.85f
                return visceralFat
            } else {
                if (20.0f < visceralFat) {
                    visceralFat *= 0.85f
                    return visceralFat
                } else {
                    visceralFat *= 0.8f
                    return visceralFat
                }
            }
        }
    }
}
