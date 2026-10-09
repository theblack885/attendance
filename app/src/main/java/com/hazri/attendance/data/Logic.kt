package com.hazri.attendance.data

import java.time.LocalDate
import java.time.YearMonth

/** Works out what the status of a given day is for an employee. */
fun resolveStatus(
    date: LocalDate,
    emp: Employee,
    rec: Attendance?,
    leaves: List<LeaveRequest>,
    holidays: Set<String>,
    s: AppSettings,
    today: LocalDate = LocalDate.now()
): String {
    if (rec != null) return rec.status
    val join = runCatching { LocalDate.parse(emp.joinDate) }.getOrDefault(date)
    if (date.isBefore(join)) return Status.NA
    if (date.toString() in holidays) return Status.HOLIDAY
    if (date.dayOfWeek.value in s.weeklyOff) return Status.WEEKOFF
    val onLeave = leaves.any {
        it.empId == emp.id && it.status == LeaveStatus.APPROVED &&
            !date.isBefore(LocalDate.parse(it.fromDate)) && !date.isAfter(LocalDate.parse(it.toDate))
    }
    if (onLeave) return Status.LEAVE
    return when {
        date.isAfter(today) -> Status.FUTURE
        date == today -> Status.PENDING
        else -> Status.ABSENT
    }
}

data class Summary(
    val present: Int = 0,
    val late: Int = 0,
    val half: Int = 0,
    val absent: Int = 0,
    val leave: Int = 0,
    val workedMs: Long = 0
) {
    val attended: Int get() = present + late + half
}

fun summarize(
    emp: Employee,
    month: YearMonth,
    recs: List<Attendance>,
    leaves: List<LeaveRequest>,
    holidays: Set<String>,
    s: AppSettings
): Summary {
    var present = 0
    var late = 0
    var half = 0
    var absent = 0
    var leave = 0
    var worked = 0L
    val mine = recs.filter { it.empId == emp.id }.associateBy { it.date }
    for (day in 1..month.lengthOfMonth()) {
        val d = month.atDay(day)
        val rec = mine[d.toString()]
        when (resolveStatus(d, emp, rec, leaves, holidays, s)) {
            Status.PRESENT -> present++
            Status.LATE -> late++
            Status.HALF_DAY -> half++
            Status.ABSENT -> absent++
            Status.LEAVE -> leave++
        }
        val inMs = rec?.checkIn
        val outMs = rec?.checkOut
        if (inMs != null && outMs != null) worked += (outMs - inMs).coerceAtLeast(0)
    }
    return Summary(present, late, half, absent, leave, worked)
}

fun statusShort(status: String): String = when (status) {
    Status.PRESENT -> "P"
    Status.LATE -> "L"
    Status.HALF_DAY -> "H"
    Status.ABSENT -> "A"
    Status.LEAVE -> "LV"
    Status.HOLIDAY -> "HOL"
    Status.WEEKOFF -> "WO"
    else -> ""
}
