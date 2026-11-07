package com.personal.diary.ui

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 倒数日配置：SharedPreferences 存 name 与 targetEpochDay 两个键。
 * 剩余 = ChronoUnit.DAYS.between(today, target)。
 */
class CountdownStore(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("countdown", Context.MODE_PRIVATE)

    fun name(): String = sp.getString(KEY_NAME, "") ?: ""

    /** 未设置时返回 null。 */
    fun target(): LocalDate? {
        val day = sp.getLong(KEY_TARGET, NOT_SET)
        if (day == NOT_SET) return null
        return LocalDate.ofEpochDay(day)
    }

    fun save(name: String, target: LocalDate) {
        sp.edit()
            .putString(KEY_NAME, name)
            .putLong(KEY_TARGET, target.toEpochDay())
            .apply()
    }

    /** 顶部右侧展示文字；未设置时返回 null，由 UI 显示"设置倒数日"。 */
    fun displayText(today: LocalDate = LocalDate.now()): String? {
        val name = name()
        val target = target() ?: return null
        if (name.isBlank()) return null
        val remain = ChronoUnit.DAYS.between(today, target)
        return when {
            remain > 0L -> "$name ${remain}天"
            remain == 0L -> "$name 今天"
            else -> "$name 已过 ${-remain} 天"
        }
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_TARGET = "targetEpochDay"
        const val NOT_SET = Long.MIN_VALUE
    }
}
