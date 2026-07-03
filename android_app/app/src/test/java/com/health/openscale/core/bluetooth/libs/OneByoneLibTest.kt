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
import kotlin.math.max
import kotlin.math.min

/**
 * Unit tests for [OneByoneLib].
 *
 * Strategy
 * 1) Snapshot tests with frozen numbers (from current Java impl) to guard a Kotlin port.
 * 2) Property tests (monotonicity, clamping, finite outputs, boundary behavior).
 *
 * NOTE: We do NOT re-implement formulas here. Snapshots are the source of truth.
 */
class OneByoneLibTest {

    private companion object {

        @JvmStatic
        private val SUPPORTS = Supports(
            builder = ::OneByoneLib,
            musclePercent = true,
            visceralFatPercent = true,
            bodyFatPercent = true,
            waterPercent = true,
            boneMassKg = true,
            lbmKg = true,
        )

        // --- Snapshots (pre-recorded from current Java implementation) -------
        // <editor-fold defaultstate="collapsed" desc="private val FIXTURES = mapOf(...)">
        private val FIXTURES = mapOf(
            "male_mid" to Snapshot(
                age = 30,
                heightCm = 180f,
                activityLevel = ActivityLevel.MILD,
                isMale = true,
                weightKg = 80f,
                impedanceOhms = 500f,
                bmi = 24.691359f,
                musclePercent = 40.97725f,
                bodyFatPercent = 23.315102f,
                visceralFatPercent = 10.79977f,
                waterPercent = 52.60584f,
                boneMassKg = 3.030576f,
                lbmKg = 61.34792f
            ),
            "female_mid" to Snapshot(
                age = 28,
                heightCm = 165f,
                activityLevel = ActivityLevel.MODERATE,
                isMale = false,
                weightKg = 60f,
                impedanceOhms = 520f,
                bmi = 22.038567f,
                musclePercent = 40.181107f,
                bodyFatPercent = 25.210106f,
                visceralFatPercent = 0.70499706f,
                waterPercent = 51.305866f,
                boneMassKg = 2.3883991f,
                lbmKg = 44.873936f
            ),
            "male_high" to Snapshot(
                age = 52,
                heightCm = 175f,
                activityLevel = ActivityLevel.EXTREME,
                isMale = true,
                weightKg = 95f,
                impedanceOhms = 430f,
                bmi = 31.020409f,
                musclePercent = 35.573257f,
                bodyFatPercent = 26.381027f,
                visceralFatPercent = 13.163806f,
                waterPercent = 50.502613f,
                boneMassKg = 3.1443515f,
                lbmKg = 69.93803f
            ),
            "imp_low" to Snapshot(
                age = 25,
                heightCm = 178f,
                activityLevel = ActivityLevel.SEDENTARY,
                isMale = true,
                weightKg = 72f,
                impedanceOhms = 80f,
                bmi = 22.724403f,
                musclePercent = 230.51118f,
                bodyFatPercent = 16.04116f,
                visceralFatPercent = 9.316022f,
                waterPercent = 57.595764f,
                boneMassKg = 3.0263696f,
                lbmKg = 60.450363f
            ),
            "imp_mid" to Snapshot(
                age = 35,
                heightCm = 170f,
                activityLevel = ActivityLevel.HEAVY,
                isMale = false,
                weightKg = 68f,
                impedanceOhms = 300f,
                bmi = 23.529411f,
                musclePercent = 60.656864f,
                bodyFatPercent = 25.14642f,
                visceralFatPercent = 2.6039982f,
                waterPercent = 51.349552f,
                boneMassKg = 2.650265f,
                lbmKg = 50.900436f
            ),
            "imp_high" to Snapshot(
                age = 45,
                heightCm = 182f,
                activityLevel = ActivityLevel.MODERATE,
                isMale = true,
                weightKg = 90f,
                impedanceOhms = 1300f,
                bmi = 27.170633f,
                musclePercent = 17.721643f,
                bodyFatPercent = 30.914497f,
                visceralFatPercent = 11.179609f,
                waterPercent = 49.32705f,
                boneMassKg = 2.901557f,
                lbmKg = 62.176952f
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
        SUPPORTS.dump("", 30, 180f, 80f, true, 500f, ActivityLevel.SEDENTARY)
        SUPPORTS.dump("", 28, 165f, 60f, false, 520f, ActivityLevel.MODERATE)
        SUPPORTS.dump("", 52, 175f, 95f, true, 430f, ActivityLevel.EXTREME)
        SUPPORTS.dump("", 25, 178f, 72f, true, 80f, ActivityLevel.SEDENTARY)
        SUPPORTS.dump("", 45, 182f, 90f, true, 1300f, ActivityLevel.MODERATE)
        SUPPORTS.dump("", 35, 170f, 68f, false, 300f, ActivityLevel.EXTREME)
    }

    // --- Behavior / Property tests -------------------------------------------

//    @Test
//    fun bmi_monotonicity_weightUp_increases_heightConstant() {
//        val lib = OneByoneLib(1, 30, 180f, 0)
//        val w1 = 70f
//        val w2 = 85f
//        assertThat(lib.getBMI(w2)).isGreaterThan(lib.getBMI(w1))
//    }
//
//    @Test
//    fun bmi_monotonicity_heightUp_decreases_weightConstant() {
//        val libShort = OneByoneLib(1, 30, 170f, 0)
//        val libTall  = OneByoneLib(1, 30, 190f, 0)
//        val w = 80f
//        assertThat(libTall.getBMI(w)).isLessThan(libShort.getBMI(w))
//    }

//    FIXME: getWater use bodyFat for calculation
//    @Test
//    fun water_switch_coeff_below_and_above_50() {
//        val lib = OneByoneLib(0, 40, 165f, 1)
//        val bfHigh = 35f // → (100-35)*0.7 = 45.5 < 50 → *1.02
//        val bfLow  = 20f // → (100-20)*0.7 = 56 > 50 → *0.98
//        val wHigh = lib.getWater(bfHigh)
//        val wLow  = lib.getWater(bfLow)
//        assertThat(wHigh).isLessThan(50f)
//        assertThat(wLow).isGreaterThan(50f)
//    }

    @Test
    fun boneMass_isReasonablyClamped_between_0_5_and_8_0() {
        val lib = InstanceBuilder(55, 170f, false, ActivityLevel.HEAVY, ::OneByoneLib)

        // Explore some extreme ranges
        val candidates = mapOf(
            40f to 1400f,
            150f to 200f,
            55f to 600f,
            95f to 300f,
        )
        candidates.forEach { (w, imp) ->
            val bone = lib(w, imp).boneMassKg
            assertThat(bone).isAtLeast(0.5f)
            assertThat(bone).isAtMost(8.0f)
        }
    }

    @Test
    fun muscle_reacts_to_impedance_reasonably() {
        val lib = InstanceBuilder(30, 180f, true, null, ::OneByoneLib)
        val weightKg = 80f

        val impHigh = 1300f
        val impMid  = 400f
        val impLow  = 80f

        val mHigh = lib(weightKg, impHigh).musclePercent
        val mMid  = lib(weightKg, impMid).musclePercent
        val mLow  = lib(weightKg, impLow).musclePercent

        // Lower impedance tends to increase SMM estimate (classic BIA behavior)
        assertThat(mLow).isGreaterThan(mMid)
        assertThat(mMid).isGreaterThan(mHigh)
    }

    @Test
    fun bodyFat_stays_within_reasonable_bounds() {
        val lib = InstanceBuilder(35, 180f, true, ActivityLevel.MODERATE, ::OneByoneLib)
        val weights = listOf(50f, 70f, 90f, 110f)
        val imps    = listOf(80f, 300f, 600f, 1200f)
        for (w in weights) for (imp in imps) {
            val bf = lib(w, imp).bodyFatPercent
            // Implementation clamps to [1, 45]; allow small epsilon
            assertThat(bf).isAtLeast(1f - EPS)
            assertThat(bf).isAtMost(45f + EPS)
        }
    }

    @Test
    fun peopleType_influences_outputs() {
        fun boneMassKg(activityLevel: ActivityLevel) =
            OneByoneLib(user(40, 175f, true, activityLevel), 85f, 450f).boneMassKg

        val boneBase = boneMassKg(ActivityLevel.MILD)
        val boneMid  = boneMassKg(ActivityLevel.MODERATE)
        val boneHigh = boneMassKg(ActivityLevel.HEAVY)

        // Different activity types should yield distinct (but not crazy) values.
        assertThat(abs(boneBase - boneMid)).isGreaterThan(0.0f)
        assertThat(abs(boneMid - boneHigh)).isGreaterThan(0.0f)

        // Guard against wild divergence
        val minV = min(boneBase, min(boneMid, boneHigh))
        val maxV = max(boneBase, max(boneMid, boneHigh))
        assertThat(maxV - minV).isLessThan(2.0f) // heuristic guard
    }

    @Test
    fun sex_flag_affects_outputs() {
        fun bodyFat(isMale: Boolean) =
            OneByoneLib(user(32, 178f, isMale), 75f, 420f).bodyFatPercent

        // Expect some difference between sexes
        assertThat(abs(bodyFat(true) - bodyFat(false))).isGreaterThan(0.1f)
    }
}
