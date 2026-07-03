package com.health.openscale.core.bluetooth.libs.utils

import com.google.common.truth.Truth.assertWithMessage
import com.health.openscale.core.bluetooth.libs.ImpedanceLib
import com.health.openscale.core.data.ActivityLevel
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator

@JvmRecord
internal data class Supports<T : ImpedanceLib>(
    private val builder: TypedBuilder<T>,
    val musclePercent: Boolean = false,
    val bodyFatPercent: Boolean = false,
    val visceralFatPercent: Boolean = false,
    val waterPercent: Boolean = false,
    val proteinPercent: Boolean = false,
    val boneMassKg: Boolean = false,
    val lbmKg: Boolean = false,
    val bcmKg: Boolean = false,
    val bmrKcal: Boolean = false,
    val extras: Map<String, T.() -> Float>? = null,
) {

    fun test(case: String, snapshot: Snapshot) {
        val lib = builder(snapshot.user, snapshot.weightKg, snapshot.impedanceOhms)
        fun assert(field: String, ofSnapshot: Snapshot.() -> Float?, ofLib: T.() -> Float) {
            val expect = requireNotNull(snapshot.ofSnapshot()) { "value \"$field\" of snapshot \"$case\" is null" }
            assertWithMessage("$case:$field").that(lib.ofLib()).isWithin(EPS).of(expect)
        }

        if (musclePercent) assert("musclePercent", { musclePercent }, { musclePercent })
        if (bodyFatPercent) assert("bodyFatPercent", { bodyFatPercent }, { bodyFatPercent })
        if (visceralFatPercent) assert("visceralFatPercent", { visceralFatPercent }, { visceralFatPercent })
        if (waterPercent) assert("waterPercent", { waterPercent }, { waterPercent })
        if (proteinPercent) assert("proteinPercent", { proteinPercent }, { proteinPercent })
        if (boneMassKg) assert("boneMassKg", { boneMassKg }, { boneMassKg })
        if (lbmKg) assert("lbmKg", { lbmKg }, { lbmKg })
        if (bcmKg) assert("bcmKg", { bcmKg }, { bcmKg })
        if (bmrKcal) assert("bmrKcal", { bmrKcal }, { bmrKcal })
        extras?.forEach { (field, getter) -> assert(field, { extras?.get(field) }, getter) }
    }

    fun testAll(snapshots: Map<String, Snapshot>) {
        require(snapshots.isNotEmpty()) { "No snapshots are defined." }
        for ((case, snapshot) in snapshots) test(case, snapshot)
    }

    @Suppress("detekt:LongParameterList")
    fun dump(
        tag: String,
        age: Long,
        heightCm: Float,
        weightKg: Float,
        isMale: Boolean,
        impedanceOhms: Float,
        activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
        @Suppress("Unused") extras: Map<String, Float>? = null,
    ) {
        val lib = builder(user(age, heightCm, isMale, activityLevel), weightKg, impedanceOhms)
        val repr = StringBuilder("$age y/o ").apply { if (!isMale) append("wo") }.append("man: ")
            .append("w=${weightKg}kg h=${heightCm}cm t=${activityLevel.name} imp=${impedanceOhms}Ω | ")

        fun append(field: String, value: Float) {
            repr.append(field).append('=').append(value).append("; ")
        }

        if (musclePercent) append("musclePercent", lib.musclePercent)
        if (bodyFatPercent) append("bodyFatPercent", lib.bodyFatPercent)
        if (visceralFatPercent) append("visceralFatPercent", lib.visceralFatPercent)
        if (waterPercent) append("waterPercent", lib.waterPercent)
        if (proteinPercent) append("proteinPercent", lib.proteinPercent)
        if (boneMassKg) append("boneMassKg", lib.boneMassKg)
        if (lbmKg) append("lbmKg", lib.lbmKg)
        if (bcmKg) append("bcmKg", lib.bcmKg)
        if (bmrKcal) append("bmrKcal", lib.bmrKcal)
        this.extras?.forEach { (field, getter) -> append(field, getter(lib)) }

        println("SNAP $tag -> $repr")
    }

    @Suppress("detekt:LongParameterList")
    fun assertOutputs(
        age: Long,
        heightCm: Float,
        isMale: Boolean,
        weightKg: Float,
        impedanceOhms: Float,
        activityLevel: ActivityLevel? = null,
    ) {
        fun assert(field: String, value: Float) {
            assertWithMessage(field).that(value.isNaN()).isFalse()
            assertWithMessage(field).that(value.isInfinite()).isFalse()
        }

        val lib = builder(user(age, heightCm, isMale, activityLevel), weightKg, impedanceOhms)
        if (musclePercent) assert("musclePercent", lib.musclePercent)
        if (bodyFatPercent) assert("bodyFatPercent", lib.bodyFatPercent)
        if (visceralFatPercent) assert("visceralFatPercent", lib.visceralFatPercent)
        if (waterPercent) assert("waterPercent", lib.waterPercent)
        if (proteinPercent) assert("proteinPercent", lib.proteinPercent)
        if (boneMassKg) assert("boneMassKg", lib.boneMassKg)
        if (lbmKg) assert("lbmKg", lib.lbmKg)
        if (bcmKg) assert("bcmKg", lib.bcmKg)
        if (bmrKcal) assert("bmrKcal", lib.bmrKcal)
        extras?.forEach { (field, getter) -> assert(field, getter(lib)) }
    }

}
