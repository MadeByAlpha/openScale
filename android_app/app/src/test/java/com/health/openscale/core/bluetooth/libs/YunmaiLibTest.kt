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
import com.health.openscale.core.bluetooth.libs.YunmaiLib.Companion.isYunmaiActive
import com.health.openscale.core.bluetooth.libs.utils.Snapshot
import com.health.openscale.core.bluetooth.libs.utils.Supports
import com.health.openscale.core.data.ActivityLevel
import org.junit.Test

class YunmaiLibTest {

    private companion object {

        @JvmStatic
        private val SUPPORTS = Supports(
            builder = ::YunmaiLib,
            bodyFatPercent = true,
            waterPercent = true,
            musclePercent = true,
            boneMassKg = true,
            lbmKg = true,
            visceralFatPercent = true,
            extras = mapOf("skeletalMuscle" to { skeletalMuscle })
        )

        // --- Snapshots (pre-recorded from current Java implementation) -------
        // <editor-fold defaultstate="collapsed" desc="private val FIXTURES = mapOf(...)">
        @Suppress("FloatingPointLiteralPrecision")
        @JvmStatic
        private val FIXTURES = mapOf(
            "male_mod_30y_180cm_80kg_res500_bf23" to Snapshot(
                age = 30,
                heightCm = 180f,
                activityLevel = ActivityLevel.MODERATE,
                isMale = true,
                weightKg = 80f,
                impedanceOhms = 500f,
                waterPercent = 55.907001f,
                bodyFatPercent = 23.237043f,
                musclePercent = 40.814999f,
                boneMassKg = 3.263390f,
                lbmKg = 61.599998f,
                visceralFatPercent = 11.318182f,
                extras = mapOf("skeletalMuscle" to 51.595001f, "bodyFatPercent" to 23f)
            ),
            "female_mild_28y_165cm_60kg_res520_bf28" to Snapshot(
                age = TODO(),
                heightCm = TODO(),
                activityLevel = ActivityLevel.MILD,
                isMale = false,
                weightKg = 60f,
                impedanceOhms = 520f,
                waterPercent = 52.276997f,
                bodyFatPercent = 29.947247f,
                musclePercent = 38.164993f,
                boneMassKg = 2.530795f,
                lbmKg = 43.200001f,
                visceralFatPercent = 6.166667f,
                extras = mapOf("skeletalMuscle" to 48.244999f, "bodyFatPercent" to 28f)
            ),
            "male_sedentary_55y_175cm_95kg_res430_bf32" to Snapshot(
                age = 55,
                heightCm = 175f,
                activityLevel = ActivityLevel.SEDENTARY,
                isMale = true,
                weightKg = 95f,
                impedanceOhms = 430f,
                waterPercent = 49.372997f,
                bodyFatPercent = 34.547203f,
                musclePercent = 36.044998f,
                boneMassKg = 3.365057f,
                lbmKg = 64.599998f,
                visceralFatPercent = 18.590908f,
                extras = mapOf("skeletalMuscle" to 45.564999f, "bodyFatPercent" to 32f)
            ),
            "female_sedentary_55y_160cm_50kg_res600_bf27" to Snapshot(
                age = 55,
                heightCm = 160f,
                activityLevel = ActivityLevel.SEDENTARY,
                isMale = false,
                weightKg = 50f,
                impedanceOhms = 600f,
                waterPercent = 53.003002f,
                bodyFatPercent = 28.532946f,
                musclePercent = 38.694996f,
                boneMassKg = 2.088284f,
                lbmKg = 36.500000f,
                visceralFatPercent = 5.055555f,
                extras = mapOf("skeletalMuscle" to 48.915001f, "bodyFatPercent" to 27f)
            ),
            "male_heavy_20y_190cm_72kg_res480_bf14" to Snapshot(
                age = 20,
                heightCm = 190f,
                activityLevel = ActivityLevel.HEAVY,
                isMale = true,
                weightKg = 72f,
                impedanceOhms = 480f,
                waterPercent = 62.441002f,
                bodyFatPercent = 15.266259f,
                musclePercent = 51.605000f,
                boneMassKg = 3.519648f,
                lbmKg = 61.919998f,
                visceralFatPercent = 9.000000f,
                extras = mapOf("skeletalMuscle" to 60.205002f, "bodyFatPercent" to 14f)
            ),
            "female_mod_22y_155cm_55kg_res510_bf29" to Snapshot(
                age = 22,
                heightCm = 155f,
                activityLevel = ActivityLevel.MODERATE,
                isMale = false,
                weightKg = 55f,
                impedanceOhms = 510f,
                waterPercent = 51.551003f,
                bodyFatPercent = 30.724077f,
                musclePercent = 37.634998f,
                boneMassKg = 2.187678f,
                lbmKg = 39.049999f,
                visceralFatPercent = 6.722222f,
                extras = mapOf("skeletalMuscle" to 47.575001f, "bodyFatPercent" to 29f)
            ),
            "male_mild_35y_175cm_85kg_res200_bf25" to Snapshot(
                age = 35,
                heightCm = 175f,
                activityLevel = ActivityLevel.MILD,
                isMale = true,
                weightKg = 85f,
                impedanceOhms = 200f,
                waterPercent = 54.455002f,
                bodyFatPercent = 27.232653f,
                musclePercent = 39.754993f,
                boneMassKg = 3.322063f,
                lbmKg = 63.750000f,
                visceralFatPercent = 13.136364f,
                extras = mapOf("skeletalMuscle" to 50.255001f, "bodyFatPercent" to 25f)
            ),
            "female_sedentary_40y_170cm_70kg_res800_bf36" to Snapshot(
                age = 40,
                heightCm = 170f,
                activityLevel = ActivityLevel.SEDENTARY,
                isMale = false,
                weightKg = 70f,
                impedanceOhms = 800f,
                waterPercent = 46.468998f,
                bodyFatPercent = 34.777931f,
                musclePercent = 33.924999f,
                boneMassKg = 2.674562f,
                lbmKg = 44.799999f,
                visceralFatPercent = 10.409091f,
                extras = mapOf("skeletalMuscle" to 42.884998f, "bodyFatPercent" to 36f)
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

    // --- Helper to (re)generate snapshot values if formulas change -----------

    @Test
    fun `print current outputs for fixtures`() {
        // Re-run if you intentionally modify formulas; then paste outputs into FIXTURES above.
        SUPPORTS.dump("", 30, 180f, 80f, true, 500f, ActivityLevel.MODERATE, mapOf("bodyFatPercent" to 23f))
        SUPPORTS.dump("", 28, 165f, 60f, false, 520f, ActivityLevel.MILD, mapOf("bodyFatPercent" to 28f))
        SUPPORTS.dump("", 55, 175f, 95f, true, 430f, ActivityLevel.SEDENTARY, mapOf("bodyFatPercent" to 32f))
        SUPPORTS.dump("", 55, 160f, 50f, false, 600f, ActivityLevel.SEDENTARY, mapOf("bodyFatPercent" to 27f))
        SUPPORTS.dump("", 20, 190f, 72f, true, 480f, ActivityLevel.HEAVY, mapOf("bodyFatPercent" to 14f))
        SUPPORTS.dump("", 22, 155f, 55f, false, 510f, ActivityLevel.MODERATE, mapOf("bodyFatPercent" to 29f))
        SUPPORTS.dump("", 35, 175f, 85f, true, 200f, ActivityLevel.MILD, mapOf("bodyFatPercent" to 25f))
        SUPPORTS.dump("", 40, 170f, 70f, false, 800f, ActivityLevel.SEDENTARY, mapOf("bodyFatPercent" to 36f))
    }

    // --- Behavior (kept from earlier) ---------------------------------------

    @Test
    fun toYunmaiActivityLevel_mapsCorrectly() {
        assertThat(ActivityLevel.EXTREME.isYunmaiActive()).isTrue()
        assertThat(ActivityLevel.HEAVY.isYunmaiActive()).isTrue()
        assertThat(ActivityLevel.MODERATE.isYunmaiActive()).isFalse()
        assertThat(ActivityLevel.MILD.isYunmaiActive()).isFalse()
        assertThat(ActivityLevel.SEDENTARY.isYunmaiActive()).isFalse()
    }

//    FIXME: uses bodyFat
//    @Test
//    fun constructor_setsFitnessFlag_indirectlyVisibleInMuscleValues() {
//        val fit = YunmaiLib(1, 180f, ActivityLevel.EXTREME)
//        val normal = YunmaiLib(1, 180f, ActivityLevel.MILD)
//        val bf = 20f
//        assertThat(fit.getMuscle(bf)).isGreaterThan(normal.getMuscle(bf))
//        assertThat(fit.getSkeletalMuscle(bf)).isGreaterThan(normal.getSkeletalMuscle(bf))
//    }
}
