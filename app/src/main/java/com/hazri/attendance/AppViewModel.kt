package com.hazri.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hazri.attendance.data.AppDatabase
import com.hazri.attendance.data.AppSettings
import com.hazri.attendance.data.Attendance
import com.hazri.attendance.data.Employee
import com.hazri.attendance.data.Holiday
import com.hazri.attendance.data.LeaveRequest
import com.hazri.attendance.data.LeaveStatus
import com.hazri.attendance.data.Role
import com.hazri.attendance.data.Session
import com.hazri.attendance.data.SettingsStore
import com.hazri.attendance.data.Status
import com.hazri.attendance.data.statusShort
import com.hazri.attendance.data.summarize
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val store = SettingsStore(app)

    val settings: StateFlow<AppSettings> = store.flow
    val session = MutableStateFlow(store.loadSession())
    val message = MutableStateFlow<String?>(null)

    val employees = dao.employees().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val leaves = dao.leaves().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val holidays = dao.holidays().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateSettings(transform: (AppSettings) -> AppSettings) = store.update(transform)

    fun attendanceOn(empId: Long, date: LocalDate) = dao.attendanceOnFlow(empId, date.toString())
    fun attendanceForDate(date: LocalDate) = dao.attendanceBetween(date.toString(), date.toString())
    fun monthRecords(m: YearMonth) = dao.attendanceBetween(m.atDay(1).toString(), m.atEndOfMonth().toString())

    // ---------- Session ----------
    private fun setSession(s: Session) {
        session.value = s
        store.saveSession(s)
    }

    fun loginAdmin(pin: String, onResult: (String?) -> Unit) {
        if (pin == settings.value.adminPin) {
            setSession(Session(Role.ADMIN, 0))
            onResult(null)
        } else onResult("Wrong admin PIN")
    }

    fun loginEmployee(code: String, pin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val e = dao.employeeByCode(code.trim())
            when {
                e == null -> onResult("Employee ID not found")
                !e.active -> onResult("Account is inactive. Please contact admin.")
                e.pin != pin -> onResult("Wrong PIN")
                else -> {
                    setSession(Session(Role.EMP, e.id))
                    onResult(null)
                }
            }
        }
    }

    fun logout() = setSession(Session())

    // ---------- Employee actions ----------
    fun checkIn(empId: Long) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val ex = dao.attendanceOn(empId, today)
            if (ex?.checkIn != null) {
                message.value = "Already checked in today"
                return@launch
            }
            val s = settings.value
            val late = LocalTime.now().isAfter(s.start.plusMinutes(s.graceMin.toLong()))
            val status = if (late) Status.LATE else Status.PRESENT
            val base = ex ?: Attendance(empId = empId, date = today, status = status)
            dao.upsertAttendance(base.copy(checkIn = System.currentTimeMillis(), checkOut = null, status = status))
            message.value = if (late) "Checked in - marked Late" else "Checked in. Have a great day!"
        }
    }

    fun checkOut(empId: Long) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val ex = dao.attendanceOn(empId, today)
            val inMs = ex?.checkIn
            if (ex == null || inMs == null) {
                message.value = "Please check in first"
                return@launch
            }
            if (ex.checkOut != null) {
                message.value = "Already checked out"
                return@launch
            }
            val now = System.currentTimeMillis()
            val minMs = settings.value.halfDayHours * 3_600_000L
            val status = if (now - inMs < minMs) Status.HALF_DAY else ex.status
            dao.upsertAttendance(ex.copy(checkOut = now, status = status))
            message.value = "Checked out. See you tomorrow!"
        }
    }

    fun applyLeave(empId: Long, type: String, from: LocalDate, to: LocalDate, reason: String) {
        viewModelScope.launch {
            dao.insertLeave(
                LeaveRequest(empId = empId, type = type, fromDate = from.toString(), toDate = to.toString(), reason = reason.trim())
            )
            message.value = "Leave request sent"
        }
    }

    fun cancelLeave(id: Long) {
        viewModelScope.launch { dao.deleteLeave(id) }
    }

    fun changeEmployeePin(empId: Long, old: String, new: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val e = employees.value.find { it.id == empId }
            when {
                e == null -> onResult("Employee not found")
                e.pin != old -> onResult("Current PIN is wrong")
                new.length < 4 -> onResult("New PIN must be at least 4 digits")
                else -> {
                    dao.upsertEmployee(e.copy(pin = new))
                    message.value = "PIN changed"
                    onResult(null)
                }
            }
        }
    }

    // ---------- Admin actions ----------
    fun changeAdminPin(old: String, new: String, onResult: (String?) -> Unit) {
        when {
            old != settings.value.adminPin -> onResult("Current PIN is wrong")
            new.length < 4 -> onResult("New PIN must be at least 4 digits")
            else -> {
                store.update { it.copy(adminPin = new) }
                message.value = "Admin PIN changed"
                onResult(null)
            }
        }
    }

    fun saveEmployee(e: Employee, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val code = e.empCode.trim()
            val clash = dao.employeeByCode(code)
            if (clash != null && clash.id != e.id) {
                onResult("Employee ID $code already exists")
                return@launch
            }
            dao.upsertEmployee(e.copy(empCode = code, name = e.name.trim()))
            message.value = "Employee saved"
            onResult(null)
        }
    }

    fun deleteEmployee(id: Long) {
        viewModelScope.launch {
            dao.deleteAttendanceOf(id)
            dao.deleteLeavesOf(id)
            dao.deleteEmployee(id)
            message.value = "Employee deleted"
        }
    }

    fun markAttendance(empId: Long, date: LocalDate, status: String, inT: LocalTime?, outT: LocalTime?, note: String) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            fun ms(t: LocalTime?): Long? = t?.let { date.atTime(it).atZone(zone).toInstant().toEpochMilli() }
            val withTimes = status == Status.PRESENT || status == Status.LATE || status == Status.HALF_DAY
            val ex = dao.attendanceOn(empId, date.toString())
            val base = ex ?: Attendance(empId = empId, date = date.toString(), status = status)
            dao.upsertAttendance(
                base.copy(
                    status = status,
                    checkIn = if (withTimes) ms(inT) else null,
                    checkOut = if (withTimes) ms(outT) else null,
                    note = note.trim()
                )
            )
            message.value = "Attendance updated"
        }
    }

    fun clearAttendance(empId: Long, date: LocalDate) {
        viewModelScope.launch {
            dao.clearAttendance(empId, date.toString())
            message.value = "Reset to automatic"
        }
    }

    fun decideLeave(id: Long, approve: Boolean) {
        viewModelScope.launch {
            dao.setLeaveStatus(id, if (approve) LeaveStatus.APPROVED else LeaveStatus.REJECTED)
            message.value = if (approve) "Leave approved" else "Leave rejected"
        }
    }

    fun addHoliday(date: LocalDate, name: String) {
        viewModelScope.launch {
            dao.upsertHoliday(Holiday(date = date.toString(), name = name.trim()))
            message.value = "Holiday added"
        }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch { dao.deleteHoliday(id) }
    }

    /** Builds a CSV for the month (summary + day-by-day codes) and hands the file to [onDone]. */
    fun exportCsv(month: YearMonth, onDone: (File?) -> Unit) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val recs = monthRecords(month).first()
                    val hol = holidays.value.map { it.date }.toSet()
                    val lv = leaves.value
                    val s = settings.value
                    fun q(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
                    val sb = StringBuilder()
                    sb.append("Emp ID,Name,Department,Present,Late,Half Day,Absent,Leave,Worked Hours")
                    for (d in 1..month.lengthOfMonth()) sb.append(',').append(d)
                    sb.append('\n')
                    for (e in employees.value.filter { it.active }) {
                        val sum = summarize(e, month, recs, lv, hol, s)
                        val mine = recs.filter { it.empId == e.id }.associateBy { it.date }
                        sb.append(q(e.empCode)).append(',').append(q(e.name)).append(',').append(q(e.department)).append(',')
                            .append(sum.present).append(',').append(sum.late).append(',').append(sum.half).append(',')
                            .append(sum.absent).append(',').append(sum.leave).append(',')
                            .append(String.format("%.1f", sum.workedMs / 3_600_000.0))
                        for (d in 1..month.lengthOfMonth()) {
                            val date = month.atDay(d)
                            val st = com.hazri.attendance.data.resolveStatus(date, e, mine[date.toString()], lv, hol, s)
                            sb.append(',').append(statusShort(st))
                        }
                        sb.append('\n')
                    }
                    val dir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
                    File(dir, "Attendance_$month.csv").apply { writeText(sb.toString()) }
                }.getOrNull()
            }
            onDone(file)
        }
    }
}
