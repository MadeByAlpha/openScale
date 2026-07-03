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
import org.junit.Test

/**
 * Unit tests for [TrisaBodyAnalyzeLib].
 *
 * - Regression fixtures use outputs computed from the current formulas
 *   to guard against accidental changes.
 * - Behavioral tests verify key monotonicity/branch properties.
 */
class TrisaBodyAnalyzeLibTest {

    private companion object {

        @JvmStatic
        private val SUPPORTS = Supports(
            builder = ::TrisaBodyAnalyzeLib,
            waterPercent = true,
            bodyFatPercent = true,
            musclePercent = true,
            boneMassKg = true,
            extras = mapOf("bmi" to { bmi })
        )

        // --- Snapshots (pre-recorded from current Java implementation) -------
        // <editor-fold defaultstate="collapsed" desc="private val FIXTURES = mapOf(...)">
        @JvmStatic
        private val FIXTURES = mapOf(
            "male_30y_180cm_80kg_imp500" to Snapshot(
                age = 30,
                heightCm = 180f,
                isMale = true,
                weightKg = 80f,
                impedanceOhms = 500f,
                bmi = 24.691359f,
                waterPercent = 57.031845f,
                bodyFatPercent = 23.186619f,
                musclePercent = 40.767307f,
                boneMassKg = 4.254889f
            ),
            "female_28y_165cm_60kg_imp520" to Snapshot(
                age = 28,
                heightCm = 165f,
                isMale = false,
                weightKg = 60f,
                impedanceOhms = 520f,
                bmi = 22.038567f,
                waterPercent = 51.246567f,
                bodyFatPercent = 27.63467f,
                musclePercent = 32.776436f,
                boneMassKg = 4.575968f
            ),
            "male_45y_175cm_95kg_imp430" to Snapshot(
                age = 45,
                heightCm = 175f,
                isMale = true,
                weightKg = 95f,
                impedanceOhms = 430f,
                bmi = 31.020409f,
                waterPercent = 51.385693f,
                bodyFatPercent = 34.484245f,
                musclePercent = 30.524948f,
                boneMassKg = 3.1716952f
            ),
            "female_55y_160cm_50kg_imp600" to Snapshot(
                age = 55,
                heightCm = 160f,
                isMale = false,
                weightKg = 50f,
                impedanceOhms = 600f,
                bmi = 19.53125f,
                waterPercent = 55.407524f,
                bodyFatPercent = 26.659752f,
                musclePercent = 27.356312f,
                boneMassKg = 3.8092093f
            ),
            "male_20y_190cm_65kg_imp480" to Snapshot(
                age = 20,
                heightCm = 190f,
                isMale = true,
                weightKg = 65f,
                impedanceOhms = 480f,
                bmi = 18.00554f,
                waterPercent = 64.203964f,
                bodyFatPercent = 10.668964f,
                musclePercent = 49.972504f,
                boneMassKg = 5.2273664f
            ),
            "female_22y_155cm_55kg_imp510" to Snapshot(
                age = 22,
                heightCm = 155f,
                isMale = false,
                weightKg = 55f,
                impedanceOhms = 510f,
                bmi = 22.89282f,
                waterPercent = 49.936302f,
                bodyFatPercent = 28.405312f,
                musclePercent = 33.747982f,
                boneMassKg = 4.713689f
            ),
            "male_35y_175cm_85kg_imp200" to Snapshot(
                age = 35,
                heightCm = 175f,
                isMale = true,
                weightKg = 85f,
                impedanceOhms = 200f,
                bmi = 27.755102f,
                waterPercent = 56.290474f,
                bodyFatPercent = 25.228241f,
                musclePercent = 38.142612f,
                boneMassKg = 3.9760387f
            ),
            "female_40y_170cm_70kg_imp800" to Snapshot(
                age = 40,
                heightCm = 170f,
                isMale = false,
                weightKg = 70f,
                impedanceOhms = 800f,
                bmi = 24.221453f,
                waterPercent = 47.909973f,
                bodyFatPercent = 35.216103f,
                musclePercent = 27.238312f,
                boneMassKg = 3.7960525f
            )
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

    // --- Simple BMI checks ---------------------------------------------------

    @Test
    fun bmi_isComputedCorrectly_forTypicalMale() {
        val bmi = TrisaBodyAnalyzeLib(user(30, 180f, true), 80f, 1f).bmi
        assertThat(bmi).isWithin(EPS).of(24.691358.toFloat())
    }

    @Test
    fun bmi_monotonicity_weightUp_heightSame_increases() {
        val lib = InstanceBuilder(28, 165f, false, null, ::TrisaBodyAnalyzeLib)
        val bmiLow = lib(60f, 1f).bmi
        val bmiHigh = lib(65f, 1f).bmi

        assertThat(bmiHigh).isGreaterThan(bmiLow)
    }

    @Test
    fun bmi_monotonicity_heightUp_weightSame_decreases() {
        fun bmi(heightCm: Float) =
            TrisaBodyAnalyzeLib(user(35, heightCm, true), 80f, 1f).bmi

        assertThat(bmi(185f)).isLessThan(bmi(170f))
    }

    // --- Behavioral properties -----------------------------------------------

    @Test
    fun `impedance effects have expected directions (male)`() {
        val lib = InstanceBuilder(30, 180f, true, null, ::TrisaBodyAnalyzeLib)
        val impLow = lib(70f, 300f)
        val impHigh = lib(70f, 700f)

        assertThat(impHigh.waterPercent).isLessThan(impLow.waterPercent)
        assertThat(impHigh.musclePercent).isLessThan(impLow.musclePercent)
        assertThat(impHigh.boneMassKg).isLessThan(impLow.boneMassKg)
        assertThat(impHigh.bodyFatPercent).isGreaterThan(impLow.bodyFatPercent)
    }

    @Test
    fun `impedance effects have expected directions (female)`() {
        val lib = InstanceBuilder(30, 165f, false, null, ::TrisaBodyAnalyzeLib)
        val impLow = lib(70f, 300f)
        val impHigh = lib(70f, 700f)

        assertThat(impHigh.waterPercent).isLessThan(impLow.waterPercent)
        assertThat(impHigh.musclePercent).isLessThan(impLow.musclePercent)
        assertThat(impHigh.boneMassKg).isLessThan(impLow.boneMassKg)
        assertThat(impHigh.bodyFatPercent).isGreaterThan(impLow.bodyFatPercent)
    }

    @Test
    fun sex_flag_changes_branch_outputs() {
        fun lib(isMale: Boolean) =
            TrisaBodyAnalyzeLib(user(30, 175f, isMale), 70f, 500f)

        val male = lib(true)
        val female = lib(false)

        assertThat(male.waterPercent).isNotEqualTo(female.waterPercent)
        assertThat(male.musclePercent).isNotEqualTo(female.musclePercent)
        assertThat(male.boneMassKg).isNotEqualTo(female.boneMassKg)
        assertThat(male.bodyFatPercent).isNotEqualTo(female.bodyFatPercent)
    }

}
