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


/**
 * This provides as far as possible science-based algorithms which are as far as possible based on impedance.
 * Use this for scales which don't provide all the measurements or in general as a replacement for a scale's
 * built-in formulas which are often unscientific and ignore impedance.
 *
 * The formulas strive to be consistent within themselves and with established formulas.
 * For example, this paper mentions a formula for TBW (total body water) vs. FFM (fat-free mass):
 * https://pmc.ncbi.nlm.nih.gov/articles/PMC11625996/
 * TBW / FFM = 0.732
 * So, our water percentage and body fat percentage and fat-free mass should be consistent with that formula.
 *
 * Also, when taking a normal person, all formulas should produce values that are considered normal.
 * For example, the TBW should be 59% for an average male, but can range from 43-73%.
 *
 * Finally, let's get to the problems:
 * We assume that the calculated absolute values are imprecise. One reason is that consumer scales are imprecise.
 * Also, impedance can vary quite a bit based on what you did that day.
 * For example, when you sweat a lot (i.e. your body loses water), this can influence the impedance.
 * So, don't measure directly after a workout, but better wait for at least 2h.
 * Ideally you keep all conditions as similar as possible when doing measurements and avoid measuring directly
 * after workout or a big lunch break.
 *
 * So does this mean all measurements are useless? Not quite:
 * We can still focus on tracking relative changes over longer time periods. You can still see that your muscle mass
 * has increased or your body fat has decreased over several months. Also, you might see these changes in the data
 * earlier than in the mirror. At least it can be fun and motivating to track.
 *
 * Assumption about impedance measurements:
 * For a man who is around 180cm tall and has normal BMI, the scale should report an impedance of around 500 ± 100.
 * Even such a large error won't have a huge influence on the calculated body fat, so you can at least approximately
 * track how well in shape you are.
 * However, if your scale reports an impedance of e.g. 250 or 800 then better don't use this class.
 */
@Suppress("detekt:MagicNumber")
class StandardImpedanceLib(
    user: ScaleUser,
    weightKg: Float,
    impedance: Float
) : ImpedanceLib(user, weightKg, impedance) {

    /**
     * Reusable constant for H_cm^2 / R which appears in several impedance-based formulas.
     */
    val h2rCoeff = heightCm * heightCm / impedance

    /**
     * FFM / fat-free mass according to Sun SS et al. (2003).
     *
     * https://www.researchgate.net/publication/10940351_Sun_SS_Chumlea_WC_Heymsfield_SB_Lukaski_HC_Schoeller_D_Friedl_K_Kuczmarski_RJ_Flegal_KM_Johnson_CL_Hubbard_VSDevelopment_of_bioelectrical_impedance_analysis_prediction_equations_for_body_composition_w
     */
    override val lbmKg: Float by lazy {
        if (isMale) {
            -10.68f + 0.65f * h2rCoeff + 0.26f * weightKg + 0.02f * impedance
        } else {
            -9.53f + 0.69f * h2rCoeff + 0.17f * weightKg + 0.02f * impedance
        }
    }
    override val bcmKg: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    override val bodyFatPercent: Float = (1f - lbmKg / weightKg) * 100f

    /**
     * TBW / total body water according to Sun SS et al. (2003).
     *
     * https://www.researchgate.net/publication/10940351_Sun_SS_Chumlea_WC_Heymsfield_SB_Lukaski_HC_Schoeller_D_Friedl_K_Kuczmarski_RJ_Flegal_KM_Johnson_CL_Hubbard_VSDevelopment_of_bioelectrical_impedance_analysis_prediction_equations_for_body_composition_w
     *
     * TODO: For children we might want to use the Mellits-Cheek formula.
     */
    val waterKg: Float by lazy {
        val liters =
            if (isMale) 1.2f + 0.45f * h2rCoeff + 0.18f * weightKg
            else 3.75f + 0.45f * h2rCoeff + 0.11f * weightKg

        // Convert liters to kg at an average 36.5°C water temperature across the whole body
        0.99513f * liters
    }

    override val waterPercent: Float by lazy { waterKg.percent }
    override val proteinPercent: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    /**
     * BMR / basal metabolic rate according to Katch-McArdle.
     *
     * TODO: For women we might have to distinguish by age: https://www.mdpi.com/2072-6643/13/2/345
     */
    override val bmrKcal: Float = lbmKg * 21.6f + 370f

    /**
     * Skeletal Muscle Mass (%) according to Janssen et al.
     *
     * Not to be confused with the total muscle mass.
     */
    val muscleMassKg: Float by lazy {
        0.401f * h2rCoeff + 3.825f * male - 0.071f * age + 5.102f
    }

    override val musclePercent: Float by lazy { muscleMassKg.percent }
    override val visceralFatPercent: Float
        get() = throw UnsupportedOperationException("Unsupported on this scale")

    override val boneMassKg: Float by lazy { (if (isMale) 0.057f else 0.05f) * lbmKg }
}
