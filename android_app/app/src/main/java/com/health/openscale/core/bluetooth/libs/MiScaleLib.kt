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
/**
 * based on https://github.com/prototux/MIBCS-reverse-engineering by prototux
 */
package com.health.openscale.core.bluetooth.libs

import com.health.openscale.core.bluetooth.data.ScaleUser


@Suppress("detekt:MagicNumber")
class MiScaleLib(user: ScaleUser, weight: Float, impedance: Float) : ImpedanceLib(user, weight, impedance) {

    private val lbmCoefficient: Float by lazy {
        var lbm = (heightCm * 9.058f / 100.0f) * (heightCm / 100.0f)
        lbm += weightKg * 0.32f + 12.226f
        lbm -= impedance * 0.0068f
        lbm -= age * 0.0542f
        lbm
    }

    override val lbmKg: Float by lazy {
        var leanBodyMass = weightKg - (bodyFatPercent * weightKg * 0.01f) - boneMassKg

        if (!isMale && leanBodyMass >= 84.0f) {
            leanBodyMass = 120.0f
        } else if (isMale && leanBodyMass >= 93.5f) {
            leanBodyMass = 120.0f
        }

        leanBodyMass
    }

    /**
     * Skeletal Muscle Mass (%) derived from Janssen et al. BIA equation (kg) -> percent of body weight.
     * If impedance is non-positive, falls back to LBM * ratio.
     */
    override val musclePercent: Float by lazy {
        if (weightKg <= 0f) return@lazy 0f

        val smmKg: Float =
            // Janssen et al.: SMM(kg) = 0.401*(H^2/R) + 3.825*sex - 0.071*age + 5.102
            if (impedance > 0f) 0.401f * ((heightCm * heightCm) / impedance) + (3.825f * male) - (0.071f * age) + 5.102f
            // Fallback: approximate as fraction of LBM
            else lbmKg * (if (isMale) 0.52f else 0.46f)

        val percent = (smmKg / weightKg) * 100f
        percent.coerceIn(10f, 60f)
    }

    override val waterPercent: Float by lazy {
        val water = (100.0f - bodyFatPercent) * 0.7f
        val coeff = if (water < 50f) 1.02f else 0.98f
        coeff * water
    }

    override val boneMassKg: Float by lazy {
        val base = if (!isMale) 0.245691014f else 0.18016894f
        var boneMass = (base - (lbmCoefficient * 0.05158f)) * -1.0f

        boneMass = if (boneMass > 2.2f) boneMass + 0.1f else boneMass - 0.1f

        if (!isMale && boneMass > 5.1f) {
            boneMass = 8.0f
        } else if (isMale && boneMass > 5.2f) {
            boneMass = 8.0f
        }

        boneMass
    }

    override val visceralFatPercent: Float by lazy {
        val visceralFat: Float
        if (!isMale) {
            if (weightKg > (13.0f - (heightCm * 0.5f)) * -1.0f) {
                val subsubcalc = ((heightCm * 1.45f) + (heightCm * 0.1158f) * heightCm) - 120.0f
                val subcalc = weightKg * 500.0f / subsubcalc
                visceralFat = (subcalc - 6.0f) + (age * 0.07f)
            } else {
                val subcalc = 0.691f + (heightCm * -0.0024f) + (heightCm * -0.0024f)
                visceralFat = (((heightCm * 0.027f) - (subcalc * weightKg)) * -1.0f) + (age * 0.07f) - age
            }
        } else {
            if (heightCm < weightKg * 1.6f) {
                val subcalc = ((heightCm * 0.4f) - (heightCm * (heightCm * 0.0826f))) * -1.0f
                visceralFat = ((weightKg * 305.0f) / (subcalc + 48.0f)) - 2.9f + (age * 0.15f)
            } else {
                val subcalc = 0.765f + heightCm * -0.0015f
                visceralFat = (((heightCm * 0.143f) - (weightKg * subcalc)) * -1.0f) + (age * 0.15f) - 5.0f
            }
        }
        visceralFat
    }

    override val bodyFatPercent: Float by lazy {
        val lbmSub = if (isMale) 0.8f else if (age <= 49) 9.25f else 7.25f

        var coeff = 1.0f
        if (isMale && weightKg < 61.0f) {
            coeff = 0.98f
        } else if (!isMale && weightKg > 60.0f) {
            coeff = 0.96f
            if (heightCm > 160.0f) {
                coeff *= 1.03f
            }
        } else if (!isMale && weightKg < 50.0f) {
            coeff = 1.02f
            if (heightCm > 160.0f) {
                coeff *= 1.03f
            }
        }

        var bodyFat = (1.0f - (((lbmCoefficient - lbmSub) * coeff) / weightKg)) * 100.0f
        if (bodyFat > 63.0f) {
            bodyFat = 75.0f
        }
        bodyFat
    }

    override val proteinPercent: Float get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bcmKg: Float get() = throw UnsupportedOperationException("Unsupported on this scale")
    override val bmrKcal: Float get() = throw UnsupportedOperationException("Unsupported on this scale")
}
