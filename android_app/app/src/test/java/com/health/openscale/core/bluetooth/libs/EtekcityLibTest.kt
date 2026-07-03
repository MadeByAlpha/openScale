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
import com.health.openscale.core.bluetooth.libs.utils.user
import org.junit.Test

/**
 * Unit tests for [EtekcityLib].
 */
class EtekcityLibTest {

    val lib = EtekcityLib(user(30, 180f, true), 80f, 527f)

    @Test
    fun bmi_isComputedCorrectly_forTypicalMale() {
        assertThat(lib.bmi).isWithin(EPS).of(24.69136f)
        assertThat(lib.bodyFatPercent).isWithin(EPS).of(17.7f)
        assertThat(lib.fatFreeWeight).isWithin(EPS).of(65.84f)
        assertThat(lib.visceralFatPercent).isWithin(EPS).of(7.64163f)
        assertThat(lib.waterPercent).isWithin(EPS).of(59.4206f)
        assertThat(lib.basalMetabolicRate).isWithin(EPS).of(1792.144f)
        assertThat(lib.musclePercent).isWithin(EPS).of(53.1658f)
        assertThat(lib.boneMassKg).isWithin(EPS).of(3.292f)
        assertThat(lib.subcutaneousFat).isWithin(EPS).of(15.3993f)
        assertThat(lib.muscleMass).isWithin(EPS).of(62.548f)
        assertThat(lib.proteinPercentage).isWithin(EPS).of(18.7644f)
        assertThat(lib.weightScore).isEqualTo(76)
        assertThat(lib.fatScore).isEqualTo(97)
        assertThat(lib.bmiScore).isEqualTo(89)
        assertThat(lib.healthScore).isEqualTo(87)
        assertThat(lib.metabolicAge).isEqualTo(29)
    }

//    @Test
//    fun bmi_monotonicity_weightUp_heightSame_increases() {
//        assertThat(lib.run { copy(weightKg = weightKg + 5.0) }.bmi).isGreaterThan(lib.bmi)
//    }
//
//    @Test
//    fun bmi_monotonicity_heightUp_weightSame_decreases() {
//        assertThat(lib.run { copy(heightM = heightM + 0.05) }.bmi).isLessThan(lib.bmi)
//    }
}
