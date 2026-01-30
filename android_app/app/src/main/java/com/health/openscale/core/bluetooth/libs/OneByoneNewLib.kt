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

// This class is similar to OneByoneLib, but the way measures are computer are slightly different
class OneByoneNewLib(
    isMale: Boolean,
    age: Int,
    height: Float,
) : MonoSensorAnalyzeLib(isMale, age, height) {

    override fun getLBM(weight: Float, impedance: Float): Float {
        var lbmCoeff = height / 100 * height / 100 * 9.058f
        lbmCoeff += 12.226f
        lbmCoeff += (weight * 0.32f)
        lbmCoeff -= (impedance * 0.0068f)
        lbmCoeff -= (age * 0.0542f)
        return lbmCoeff
    }

    fun getBMMRCoeff(weight: Float): Float {
        var bmmrCoeff = 20
        if (isMale) {
            bmmrCoeff = 21
            if (age < 0xd) {
                bmmrCoeff = 36
            } else if (age < 0x10) {
                bmmrCoeff = 30
            } else if (age < 0x12) {
                bmmrCoeff = 26
            } else if (age < 0x1e) {
                bmmrCoeff = 23
            } else if (age >= 0x32) {
                bmmrCoeff = 20
            }
        } else {
            if (age < 0xd) {
                bmmrCoeff = 34
            } else if (age < 0x10) {
                bmmrCoeff = 29
            } else if (age < 0x12) {
                bmmrCoeff = 24
            } else if (age < 0x1e) {
                bmmrCoeff = 22
            } else if (age >= 0x32) {
                bmmrCoeff = 19
            }
        }
        return bmmrCoeff.toFloat()
    }

    fun getBMMR(weight: Float): Float {
        var bmmr: Float
        if (isMale) {
            bmmr = (weight * 14.916f + 877.8f) - height * 0.726f
            bmmr -= (age * 8.976f)
        } else {
            bmmr = (weight * 10.2036f + 864.6f) - height * 0.39336f
            bmmr -= (age * 6.204f)
        }

        return getBounded(bmmr, 500f, 1000f)
    }

    override fun getBodyFat(weight: Float, impedance: Float): Float {
        var bodyFat = getLBM(weight, impedance)

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
            if (weight < 50) {
                bodyFat *= 1.02f
            } else if (weight > 60) {
                bodyFat *= 0.96f
            }

            if (height > 160) {
                bodyFat *= 1.03f
            }
        } else {
            if (weight < 61) {
                bodyFat *= 0.98f
            }
        }

        return 100 * (1 - bodyFat / weight)
    }

    override fun getBoneMass(weight: Float, impedance: Float): Float {
        val lbmCoeff = getLBM(weight, impedance)

        var boneMassConst: Float
        if (isMale) {
            boneMassConst = 0.18016894f
        } else {
            boneMassConst = 0.245691014.toFloat()
        }

        boneMassConst = lbmCoeff * 0.05158f - boneMassConst
        val boneMass: Float
        if (boneMassConst <= 2.2) {
            boneMass = boneMassConst - 0.1f
        } else {
            boneMass = boneMassConst + 0.1f
        }

        return getBounded(boneMass, 0.5f, 8f)
    }

    fun getMuscleMass(weight: Float, impedance: Float): Float {
        var muscleMass = weight - getBodyFat(weight, impedance) * 0.01f * weight
        muscleMass -= getBoneMass(weight, impedance)
        return getBounded(muscleMass, 10f, 120f)
    }

    override fun getMuscle(weight: Float, impedance: Float): Float {
        var skeletonMuscleMass = getWater(weight, impedance)
        skeletonMuscleMass *= weight
        skeletonMuscleMass *= 0.8422f * 0.01f
        skeletonMuscleMass -= 2.9903f
        skeletonMuscleMass /= weight
        return skeletonMuscleMass * 100
    }

    override fun getVisceralFat(weight: Float, impedance: Float): Float {
        val visceralFat: Float
        if (isMale) {
            if (height < weight * 1.6 + 63.0) {
                visceralFat =
                    age * 0.15f + ((weight * 305.0f) / ((height * 0.0826f * height - height * 0.4f) + 48.0f) - 2.9f)
            } else {
                visceralFat =
                    age * 0.15f + (weight * (height * -0.0015f + 0.765f) - height * 0.143f) - 5.0f
            }
        } else {
            if (weight <= height * 0.5 - 13.0) {
                visceralFat =
                    age * 0.07f + (weight * (height * -0.0024f + 0.691f) - height * 0.027f) - 10.5f
            } else {
                visceralFat =
                    age * 0.07f + ((weight * 500.0f) / ((height * 1.45f + height * 0.1158f * height) - 120.0f) - 6.0f)
            }
        }

        return getBounded(visceralFat, 1f, 50f)
    }

    override fun getWater(weight: Float, impedance: Float): Float {
        var waterPercentage = (100 - getBodyFat(weight, impedance)) * 0.7f
        if (waterPercentage > 50) {
            waterPercentage *= 0.98f
        } else {
            waterPercentage *= 1.02f
        }

        return getBounded(waterPercentage, 35f, 75f)
    }

    fun getProtein(weight: Float, impedance: Float): Float {
        return (((100.0f - getBodyFat(weight, impedance))
                - getWater(weight, impedance) * 1.08f
                )
                - (getBoneMass(weight, impedance) / weight) * 100.0f)
    }


    private fun getBounded(value: Float, lowerBound: Float, upperBound: Float): Float {
        if (value < lowerBound) {
            return lowerBound
        } else if (value > upperBound) {
            return upperBound
        }
        return value
    }
}
