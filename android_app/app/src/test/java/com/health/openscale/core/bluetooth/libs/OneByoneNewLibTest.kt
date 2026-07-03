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

import com.google.common.truth.Truth.assertThat
import com.health.openscale.core.bluetooth.libs.utils.EPS
import com.health.openscale.core.bluetooth.libs.utils.InstanceBuilder
import com.health.openscale.core.bluetooth.libs.utils.Snapshot
import com.health.openscale.core.bluetooth.libs.utils.Supports
import com.health.openscale.core.bluetooth.libs.utils.user
import com.health.openscale.core.data.ActivityLevel
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for [OneByoneNewLib].
 *
 * - Snapshot tests (FIXTURES) sichern, dass sich die Outputs nach Refactor/Port nicht verändern.
 * - Property-Tests prüfen Monotonie, Clamping, Finite-Werte etc.
 */
class OneByoneNewLibTest {

    private companion object {

        @JvmStatic
        private val SUPPORTS = Supports(
            builder = ::OneByoneNewLib,
            lbmKg = true,
            bodyFatPercent = true,
            boneMassKg = true,
            musclePercent = true,
            visceralFatPercent = true,
            waterPercent = true,
            proteinPercent = true,
            extras = mapOf(
                "bmmr" to { bmmr },
                "bmmrCoeff" to { bmmrCoeff },
                "muscleMassKg" to { muscleMassKg },
            )
        )

        // --- Snapshots (pre-recorded from current Java implementation) -------
        // <editor-fold defaultstate="collapsed" desc="private val FIXTURES = mapOf(...)">
        @JvmStatic
        private val FIXTURES = mapOf(
            "male_mid" to Snapshot(
                age = 30,
                heightCm = 180f,
                isMale = true,
                weightKg = 80f,
                impedanceOhms = 500f,
                bmi = 24.691359f,
                musclePercent = 40.566765f,
                bodyFatPercent = 23.315102f,
                visceralFatPercent = 10.79977f,
                waterPercent = 52.60584f,
                proteinPercent = 15.963814f,
                boneMassKg = 3.1254208f,
                lbmKg = 62.14792f,
                extras = mapOf(
                    "bmmr" to 1000.0f,
                    "bmmrCoeff" to 21.0f,
                    "muscleMassKg" to 58.2225f,
                )
            ),
            "female_mid" to Snapshot(
                age = 28,
                heightCm = 165f,
                isMale = false,
                weightKg = 60f,
                impedanceOhms = 520f,
                bmi = 22.038567f,
                musclePercent = 36.45647f,
                bodyFatPercent = 28.27285f,
                visceralFatPercent = 4.704997f,
                waterPercent = 49.204823f,
                proteinPercent = 14.44164f,
                boneMassKg = 2.486581f,
                lbmKg = 51.032806f,
                extras = mapOf(
                    "bmmr" to 1000.0f,
                    "bmmrCoeff" to 22.0f,
                    "muscleMassKg" to 40.549713f
                )
            ),
            "imp_low" to Snapshot(
                age = 25,
                heightCm = 178f,
                isMale = true,
                weightKg = 72f,
                impedanceOhms = 80f,
                bmi = 22.724403f,
                musclePercent = 45.008743f,
                bodyFatPercent = 14.907819f,
                visceralFatPercent = 9.316022f,
                waterPercent = 58.373234f,
                proteinPercent = 17.714064f,
                boneMassKg = 3.1212144f,
                lbmKg = 62.06637f,
                extras = mapOf(
                    "bmmr" to 1000.0f,
                    "bmmrCoeff" to 23.0f,
                    "muscleMassKg" to 58.145157f
                )
            ),
            "imp_mid" to Snapshot(
                age = 35,
                heightCm = 170f,
                isMale = false,
                weightKg = 68f,
                impedanceOhms = 300f,
                bmi = 23.529411f,
                musclePercent = 36.679123f,
                bodyFatPercent = 31.690466f,
                visceralFatPercent = 6.603998f,
                waterPercent = 48.773006f,
                proteinPercent = 11.583979f,
                boneMassKg = 2.754478f,
                lbmKg = 56.22662f,
                extras = mapOf(
                    "bmmr" to 1000.0f,
                    "bmmrCoeff" to 20.0f,
                    "muscleMassKg" to 43.696007f
                )
            ),
            "imp_high" to Snapshot(
                age = 45,
                heightCm = 182f,
                isMale = true,
                weightKg = 90f,
                impedanceOhms = 1300f,
                bmi = 27.170633f,
                musclePercent = 36.065098f,
                bodyFatPercent = 34.49919f,
                visceralFatPercent = 13.974511f,
                waterPercent = 46.76758f,
                proteinPercent = 11.656517f,
                boneMassKg = 3.0017734f,
                lbmKg = 59.750725f,
                extras = mapOf(
                    "bmmr" to 1000.0f,
                    "bmmrCoeff" to 21.0f,
                    "muscleMassKg" to 55.948956f
                )
            ),
        )
        // </editor-fold>

    }

    // --- Generic / property-based tests --------------------------------------

    @Test
    fun `snapshots match expected outputs`() {
        SUPPORTS.testAll(FIXTURES)
    }

    @Test
    fun `outputs are finite for typical inputs`() {
        SUPPORTS.assertOutputs(30, 180f, true, 80f, 500f)
    }

    // --- Helper to (re)generate snapshot values if formulas change -----------

    @Test
    fun `print current outputs for fixtures`() {
        // Re-run if you intentionally modify formulas; then paste outputs into FIXTURES above.
        SUPPORTS.dump("male_mid", 30, 180f, 80f, true, 500f, ActivityLevel.SEDENTARY)
        SUPPORTS.dump("female_mid", 28, 165f, 60f, false, 520f, ActivityLevel.MODERATE)
        SUPPORTS.dump("imp_low", 25, 178f, 72f, true, 80f, ActivityLevel.SEDENTARY)
        SUPPORTS.dump("imp_mid", 35, 170f, 68f, false, 300f, ActivityLevel.EXTREME)
        SUPPORTS.dump("imp_high", 45, 182f, 90f, true, 1300f, ActivityLevel.MODERATE)
    }

    // --- Behavior / Property tests -------------------------------------------

//    @Test
//    fun bmi_is_bounded_and_monotonic_with_weight() {
//        val lib = OneByoneNewLib(1, 30, 180f, 0)
//        val b1 = lib.getBMI(60f)
//        val b2 = lib.getBMI(80f)
//        val b3 = lib.getBMI(100f)
//
//        listOf(b1, b2, b3).forEach {
//            assertThat(it).isAtLeast(10f - EPS)
//            assertThat(it).isAtMost(90f + EPS)
//        }
//        assertThat(b2).isGreaterThan(b1)
//        assertThat(b3).isGreaterThan(b2)
//    }

    @Test
    fun lbm_varies_with_impedance_and_weight() {
        val lib = InstanceBuilder(28, 165f, false, builder = ::OneByoneNewLib)

        val lbmHighImp = lib(60f, 1200f).lbmKg
        val lbmLowImp  = lib(60f, 200f).lbmKg
        assertThat(lbmLowImp).isGreaterThan(lbmHighImp)

        val lbmHeavier = lib(75f, 300f).lbmKg
        val lbmLighter = lib(55f, 300f).lbmKg
        assertThat(lbmHeavier).isGreaterThan(lbmLighter)
    }

    @Test
    fun bmmrCoeff_follows_age_bands_and_sex() {
        val male = listOf(36f, 30f, 26f, 23f, 20f)
        val female = listOf(34f, 29f, 24f, 22f, 19f)
        longArrayOf(10, 15, 17, 25, 50).forEachIndexed { i, age ->
            assertThat(OneByoneNewLib(user(age, 170f, true), 70f, 1f).bmmrCoeff).isWithin(EPS).of(male[i])
            assertThat(OneByoneNewLib(user(age, 170f, false), 70f, 1f).bmmrCoeff).isWithin(EPS).of(female[i])
        }
    }

    fun assertIsBounded(a: Float, b: Float, atLeast: Float, atMost: Float, minAbs: Float) {
        assertThat(a).isAtLeast(atLeast - EPS)
        assertThat(a).isAtMost(atMost + EPS)
        assertThat(b).isAtLeast(atLeast - EPS)
        assertThat(b).isAtMost(atMost + EPS)
        assertThat(abs(a - b)).isGreaterThan(minAbs)
    }

    @Test
    fun bmmr_is_bounded_and_differs_by_sex() {
        val male = OneByoneNewLib(user(30, 180f, true), 22f, 1f).bmmr        // ~806 (ungeclamped)
        val female = OneByoneNewLib(user(30, 180f, false), 19f, 1f).bmmr     // ~802 (ungeclamped)
        assertIsBounded(male, female, 500f, 1000f, 0.1f)
    }

    @Test
    fun water_switches_coeff_around_50_and_is_bounded() {
        val lib = InstanceBuilder(35, 175f, true, builder = ::OneByoneNewLib)
        val waterLow  = lib(80f, 1200f).waterPercent    // typ. <50
        val waterHigh = lib(80f, 200f).waterPercent     // typ. >50

        // lose heuristics; just ensure they are not identical and near the “switch” region across scenarios
        assertIsBounded(waterLow, waterHigh, 35f, 75f, 0.5f)
    }

    @Test
    fun boneMass_is_bounded_and_varies_with_impedance() {
        val lib = InstanceBuilder(30, 168f, false, builder = ::OneByoneNewLib)
        val boneHighImp = lib(70f, 1200f).boneMassKg
        val boneLowImp  = lib(70f, 200f).boneMassKg

        assertThat(boneLowImp).isGreaterThan(boneHighImp)
        assertIsBounded(boneLowImp, boneHighImp, 0.5f, 8.0f, 0f)
    }

    @Test
    fun muscleMass_is_bounded_and_correlates_with_impedance() {
        val lib = InstanceBuilder(28, 180f, true, builder = ::OneByoneNewLib)
        val mmHighImp = lib(82f, 1200f).muscleMassKg
        val mmLowImp  = lib(82f, 200f).muscleMassKg

        assertThat(mmLowImp).isGreaterThan(mmHighImp)
        assertIsBounded(mmLowImp, mmHighImp, 10f, 120f, 0f)
    }

    @Test
    fun skeletonMuscle_is_finite_and_reasonable_range() {
        val sm = OneByoneNewLib(user(33, 165f, false), 58f, 400f).musclePercent

        assertThat(sm).isGreaterThan(-20f)
        assertThat(sm).isLessThan(120f)
    }
}
