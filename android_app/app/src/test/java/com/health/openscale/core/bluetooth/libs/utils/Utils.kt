package com.health.openscale.core.bluetooth.libs.utils

import com.health.openscale.core.bluetooth.data.ScaleUser
import com.health.openscale.core.bluetooth.libs.ImpedanceLib
import com.health.openscale.core.data.ActivityLevel
import com.health.openscale.core.data.GenderType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date


/**
 * General float tolerance.
 */
internal const val EPS = 1E-3f

typealias TypedBuilder<T> = (user: ScaleUser, weightKg: Float, impedanceOhms: Float) -> T

internal fun user(
    age: Long,
    heightCm: Float,
    isMale: Boolean,
    activityLevel: ActivityLevel? = null,
): ScaleUser = ScaleUser(
    birthday = Date.from(LocalDateTime.now().minusYears(age).toInstant(ZoneOffset.UTC)),
    bodyHeight = heightCm,
    gender = if (isMale) GenderType.MALE else GenderType.FEMALE,
    activityLevel = activityLevel ?: ActivityLevel.SEDENTARY,
)

@ConsistentCopyVisibility
@JvmRecord
internal data class InstanceBuilder<T : ImpedanceLib> private constructor(
    val user: ScaleUser,
    val builder: (user: ScaleUser, weightKg: Float, impedanceOhms: Float) -> T,
) {

    constructor(
        age: Long,
        heightCm: Float,
        isMale: Boolean,
        activityLevel: ActivityLevel? = null,
        builder: (user: ScaleUser, weightKg: Float, impedanceOhms: Float) -> T,
    ) : this(user(age, heightCm, isMale, activityLevel), builder)

    operator fun invoke(weightKg: Float, impedanceOhms: Float): T =
        builder(user, weightKg, impedanceOhms)

}
