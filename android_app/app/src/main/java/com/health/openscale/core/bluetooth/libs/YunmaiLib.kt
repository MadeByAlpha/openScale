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
import kotlin.math.sqrt


@Suppress("detekt:MagicNumber")
class YunmaiLib(
    user: ScaleUser,
    weightKg: Float,
    impedance: Float,
    embeddedFatPercent: Float? = null
) : ImpedanceLib(user, weightKg, impedance) {
    private val isFitnessBodyType: Boolean = user.activityLevel.isYunmaiActive()

    override val bodyFatPercent: Float by lazy {
        if (embeddedFatPercent != null) return@lazy embeddedFatPercent

        // for < 0x1e version devices
        var fat: Float

        var r = (impedance - 100.0f) / 100.0f
        val h = heightCm / 100.0f

        if (r >= 1) {
            r = sqrt(r.toDouble()).toFloat()
        }

        fat = (weightKg * 1.5f / h / h) + (age * 0.08f)
        if (isMale) {
            fat -= 10.8f
        }

        fat = (fat - 7.4f) + r

        if (fat in 5.0f..75.0f) fat else 0f
    }

    override val waterPercent: Float = ((100.0f - bodyFatPercent) * 0.726f * 100.0f + 0.5f) / 100.0f
    override val proteinPercent: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    override val musclePercent: Float by lazy {
        val muscle: Float = (100f - bodyFatPercent) * (if (isFitnessBodyType) 0.7f else 0.67f)
        ((muscle * 100.0f) + 0.5f) / 100.0f
    }

    val skeletalMuscle: Float by lazy {
        val muscle: Float = (100f - bodyFatPercent) * (if (isFitnessBodyType) 0.6f else 0.53f)
        ((muscle * 100.0f) + 0.5f) / 100.0f
    }

    override val boneMassKg: Float by lazy {
        var boneMass: Float

        val h = heightCm - 170.0f

        if (isMale) {
            boneMass = ((weightKg * (musclePercent / 100.0f) * 4.0f) / 7.0f * 0.22f * 0.6f) + (h / 100.0f)
        } else {
            boneMass = ((weightKg * (musclePercent / 100.0f) * 4.0f) / 7.0f * 0.34f * 0.45f) + (h / 100.0f)
        }

        ((boneMass * 10.0f) + 0.5f) / 10.0f
    }

    override val lbmKg: Float = weightKg * (100.0f - bodyFatPercent) / 100.0f
    override val bcmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bmrKcal: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    override val visceralFatPercent: Float by lazy {
        var f = bodyFatPercent
        val a = if (age < 18) 18 else age

        val vf: Float
        if (!isFitnessBodyType) {
            if (isMale) {
                if (a < 40) {
                    f -= 21.0f
                } else if (a < 60) {
                    f -= 22.0f
                } else {
                    f -= 24.0f
                }
            } else {
                if (a < 40) {
                    f -= 34.0f
                } else if (a < 60) {
                    f -= 35.0f
                } else {
                    f -= 36.0f
                }
            }

            var d = if (isMale) 1.4f else 1.8f
            if (f > 0.0f) {
                d = 1.1f
            }

            vf = (f / d) + 9.5f
            if (vf < 1.0f) {
                return@lazy 1.0f
            }
            if (vf > 30.0f) {
                return@lazy 30.0f
            }
            return@lazy vf
        } else {
            if (bodyFatPercent > 15.0f) {
                vf = (bodyFatPercent - 15.0f) / 1.1f + 12.0f
            } else {
                vf = -1 * (15.0f - bodyFatPercent) / 1.4f + 12.0f
            }
            if (vf < 1.0f) {
                return@lazy 1.0f
            }
            if (vf > 9.0f) {
                return@lazy 9.0f
            }
            return@lazy vf
        }
    }

    companion object {
        @JvmStatic
        fun ActivityLevel.isYunmaiActive(): Boolean = when (this) {
            ActivityLevel.HEAVY, ActivityLevel.EXTREME -> return true
            else -> return false
        }
    }
}
