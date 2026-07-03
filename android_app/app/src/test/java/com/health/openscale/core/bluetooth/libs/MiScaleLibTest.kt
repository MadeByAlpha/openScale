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
import com.health.openscale.core.bluetooth.libs.utils.Snapshot
import com.health.openscale.core.bluetooth.libs.utils.Supports
import com.health.openscale.core.bluetooth.libs.utils.user
import org.junit.Test

/**
 * Unit tests for [MiScaleLib] (current implementation).
 *
 * - Three regression fixtures use the exact outputs printed from the current code
 *   to guard against accidental changes.
 * - Behavioral tests verify important branches without brittle hard-coded numbers.
 */
class MiScaleLibTest {

    private companion object {

        @JvmStatic
        private val SUPPORTS = Supports(
            builder = ::MiScaleLib,
            lbmKg = true,
            musclePercent = true,
            waterPercent = true,
            boneMassKg = true,
            visceralFatPercent = true,
            bodyFatPercent = true,
        )

        // --- Snapshots (pre-recorded from current Java implementation) -------
        // <editor-fold defaultstate="collapsed" desc="private val FIXTURES = mapOf(...)">
        @JvmStatic
        private val FIXTURES = mapOf(
            "male_30y_180cm_80kg_imp500" to Snapshot(
                age = 30, heightCm = 180f, isMale = true, weightKg = 80f, impedanceOhms = 500f,
                // bmi = 24.691359f,
                bodyFatPercent = 23.315107f,
                boneMassKg = 3.1254203f,
                lbmKg = 58.222496f,
                musclePercent = 40.977253f,
                waterPercent = 52.605835f,
                visceralFatPercent = 13.359997f,
            ),
            "female_28y_165cm_60kg_imp520" to Snapshot(
                age = 28, heightCm = 165f, isMale = false, weightKg = 60f, impedanceOhms = 520f,
                // bmi = 22.038567f,
                bodyFatPercent = 30.361998f,
                boneMassKg = 2.4865808f,
                lbmKg = 39.29622f,
                musclePercent = 40.181103f,
                waterPercent = 49.72153f,
                visceralFatPercent = -36.555004f,
            ),
            "male_45y_175cm_95kg_imp430" to Snapshot(
                age = 45, heightCm = 175f, isMale = true, weightKg = 95f, impedanceOhms = 430f,
                // bmi = 31.020409f,
                bodyFatPercent = 32.41778f,
                boneMassKg = 3.2726917f,
                lbmKg = 60.93042f,
                musclePercent = 36.096416f,
                waterPercent = 48.2537f,
                visceralFatPercent = 24.462498f,
            )
        )
        // </editor-fold>

    }

    // --- Generic tests -------------------------------------------------------

    @Test
    fun `snapshots match expected outputs`() {
        SUPPORTS.testAll(FIXTURES)
    }

    @Test
    fun `outputs are finite for typical inputs`() {
        SUPPORTS.assertOutputs(30, 180f, true, 80f, 500f)
    }

    // --- Simple BMI checks ---------------------------------------------------

//    @Test
//    fun bmi_isComputedCorrectly_forTypicalMale() {
//        // Given
//        val lib = MiScaleLib(/* sex=male */ 1, /* age */ 30, /* height cm */ 180f)
//        val weight = 80f
//
//        // When
//        val bmi = lib.getBMI(weight)
//
//        // Then: BMI = weight / (height_m^2) = 80 / (1.8 * 1.8) = 24.691...
//        assertThat(bmi).isWithin(EPS).of(24.691358f)
//    }
//
//    @Test
//    fun bmi_monotonicity_weightUp_heightSame_increases() {
//        val lib = MiScaleLib(0, 28, 165f)
//        val bmi1 = lib.getBMI(60f)
//        val bmi2 = lib.getBMI(65f)
//        assertThat(bmi2).isGreaterThan(bmi1)
//    }
//
//    @Test
//    fun bmi_monotonicity_heightUp_weightSame_decreases() {
//        val libShort = MiScaleLib(1, 35, 170f)
//        val libTall  = MiScaleLib(1, 35, 185f)
//        val weight = 80f
//        assertThat(libTall.getBMI(weight)).isLessThan(libShort.getBMI(weight))
//    }

    // --- Special paths & edge behavior ---------------------------------------

    @Test
    fun muscle_fallback_whenImpedanceZero_usesLbmRatio_andIsClamped() {
        // Female, impedance=0 triggers fallback path (LBM * 0.46) → % of weight → clamp 10..60
        val lib = MiScaleLib(user(52, 160f, false), 48f, 0f)

        // Compute expected via the same path the code uses (behavioral property, not magic number)
        val lbm = lib.lbmKg
        val expectedPct = (lbm * 0.46f) / 48f * 100f
        val expectedClamped = expectedPct.coerceIn(10f, 60f)

        val actual = lib.musclePercent
        assertThat(actual).isWithin(EPS).of(expectedClamped)
        assertThat(actual).isAtLeast(10f)
        assertThat(actual).isAtMost(60f)
    }

    @Test
    fun muscle_percentage_isClampedAt60_whenExtremelyHigh() {
        // Construct params that blow up SMM/weight; expect clamp to 60%
        val lib = MiScaleLib(user(20, 190f, true), 40f, 50f)
        assertThat(lib.musclePercent).isWithin(EPS).of(60f)
    }

    @Test
    fun water_derivesFromBodyFat_andUsesCoeffBranch() {
        // Check: water = ((100 - BF) * 0.7) * coeff, coeff = 1.02 if <50 else 0.98
        val lib = MiScaleLib(user(50, 150f, false), 100f, 700f)

        val bf = lib.bodyFatPercent
        val raw = (100f - bf) * 0.7f
        val coeff = if (raw < 50f) 1.02f else 0.98f
        val expected = raw * coeff

        val water = lib.waterPercent
        assertThat(water).isWithin(EPS).of(expected)
        if (raw < 50f) {
            assertThat(water).isLessThan(50f)
        } else {
            assertThat(water).isGreaterThan(50f)
        }
    }
}
