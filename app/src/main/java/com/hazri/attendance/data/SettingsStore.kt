package com.hazri.attendance.data

import android.content.Context
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val company: String = "My Company",
    val shiftStart: String = "09:30",
    val shiftEnd: String = "18:00",
    val graceMin: Int = 15,
    val halfDayHours: Int = 4,
    val weeklyOff: Set<Int> = setOf(7), // 1 = Monday ... 7 = Sunday
    val theme: String = "system", // system | light | dark
    val adminPin: String = "0000"
) {
    val start: LocalTime get() = parse(shiftStart, LocalTime.of(9, 30))
    val end: LocalTime get() = parse(shiftEnd, LocalTime.of(18, 0))

    private fun parse(v: String, fallback: LocalTime): LocalTime =
        runCatching { LocalTime.parse(v) }.getOrDefault(fallback)
}

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("hazri_settings", Context.MODE_PRIVATE)

    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<AppSettings> = _flow

    private fun load(): AppSettings {
        val d = AppSettings()
        val off = sp.getString("weeklyOff", "7").orEmpty()
            .split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        return AppSettings(
            company = sp.getString("company", d.company) ?: d.company,
            shiftStart = sp.getString("shiftStart", d.shiftStart) ?: d.shiftStart,
            shiftEnd = sp.getString("shiftEnd", d.shiftEnd) ?: d.shiftEnd,
            graceMin = sp.getInt("graceMin", d.graceMin),
            halfDayHours = sp.getInt("halfDayHours", d.halfDayHours),
            weeklyOff = off,
            theme = sp.getString("theme", d.theme) ?: d.theme,
            adminPin = sp.getString("adminPin", d.adminPin) ?: d.adminPin
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val n = transform(_flow.value)
        _flow.value = n
        sp.edit()
            .putString("company", n.company)
            .putString("shiftStart", n.shiftStart)
            .putString("shiftEnd", n.shiftEnd)
            .putInt("graceMin", n.graceMin)
            .putInt("halfDayHours", n.halfDayHours)
            .putString("weeklyOff", n.weeklyOff.joinToString(","))
            .putString("theme", n.theme)
            .putString("adminPin", n.adminPin)
            .apply()
    }

    fun loadSession(): Session = Session(
        role = sp.getString("role", Role.NONE) ?: Role.NONE,
        empId = sp.getLong("empId", 0)
    )

    fun saveSession(s: Session) {
        sp.edit().putString("role", s.role).putLong("empId", s.empId).apply()
    }
}
