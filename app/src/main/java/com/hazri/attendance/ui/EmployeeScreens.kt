@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hazri.attendance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazri.attendance.AppViewModel
import com.hazri.attendance.data.Attendance
import com.hazri.attendance.data.Employee
import com.hazri.attendance.data.LeaveRequest
import com.hazri.attendance.data.LeaveStatus
import com.hazri.attendance.data.Status
import com.hazri.attendance.data.resolveStatus
import com.hazri.attendance.data.summarize
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

fun fmtTime(ms: Long?): String =
    if (ms == null) "--:--" else Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mm a"))

fun fmtDur(ms: Long): String {
    val m = ms / 60000
    return "${m / 60}h ${m % 60}m"
}

fun leaveColor(status: String): Color = when (status) {
    LeaveStatus.APPROVED -> Color(0xFF22C55E)
    LeaveStatus.REJECTED -> Color(0xFFEF4444)
    else -> Color(0xFFF59E0B)
}

@Composable
fun LeaveChip(status: String) {
    val c = leaveColor(status)
    Box(Modifier.clip(CircleShape).background(c.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(status.lowercase().replaceFirstChar { it.uppercase() }, color = c, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- TODAY

@Composable
fun EmpHome(vm: AppViewModel, emp: Employee) {
    val s by vm.settings.collectAsState()
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000)
        }
    }
    val today = now.toLocalDate()
    val month = YearMonth.from(today)
    val rec by remember(emp.id, today) { vm.attendanceOn(emp.id, today) }.collectAsState(initial = null)
    val recs by remember(month) { vm.monthRecords(month) }.collectAsState(initial = emptyList())
    val leaves by vm.leaves.collectAsState()
    val holidays by vm.holidays.collectAsState()
    val hol = remember(holidays) { holidays.map { it.date }.toSet() }
    val sum = remember(recs, leaves, hol, s, emp, month) { summarize(emp, month, recs, leaves, hol, s) }
    val status = resolveStatus(today, emp, rec, leaves, hol, s)

    val nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val inMs = rec?.checkIn
    val outMs = rec?.checkOut
    val worked = if (inMs != null) ((outMs ?: nowMs) - inMs).coerceAtLeast(0) else 0L
    val shiftMs = Duration.between(s.start, s.end).toMillis().coerceAtLeast(3_600_000L)
    val progress = (worked.toFloat() / shiftMs).coerceIn(0f, 1f)
    val canIn = inMs == null
    val canOut = inMs != null && outMs == null

    val greeting = when {
        now.hour < 12 -> "Good morning"
        now.hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(BrandGradient)
                    .padding(22.dp)
            ) {
                Column {
                    Text(greeting, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge)
                    Text(emp.name.trim().substringBefore(' '), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        now.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Shift  " + s.start.format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + s.end.format(DateTimeFormatter.ofPattern("hh:mm a")),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(
                        progress = progress,
                        modifier = Modifier.size(208.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ringBrush = if (canOut || canIn) BrandGradient else Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF06B6A4)))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(fmtDur(worked), style = MaterialTheme.typography.headlineMedium)
                            Text("worked today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            StatusChip(status)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Check-in", fmtTime(inMs), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatTile("Check-out", fmtTime(outMs), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (canIn) vm.checkIn(emp.id) else if (canOut) vm.checkOut(emp.id) },
                        enabled = canIn || canOut,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canOut) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (canIn) "Check In" else if (canOut) "Check Out" else "Done for today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if ((status == Status.HOLIDAY || status == Status.WEEKOFF) && canIn) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Today is a ${if (status == Status.HOLIDAY) "holiday" else "weekly off"}. Check in only if you are working.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { SectionTitle("This month") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Present", "${sum.present}", statusColor(Status.PRESENT), Modifier.weight(1f))
                    StatTile("Late", "${sum.late}", statusColor(Status.LATE), Modifier.weight(1f))
                    StatTile("Absent", "${sum.absent}", statusColor(Status.ABSENT), Modifier.weight(1f))
                    StatTile("Leave", "${sum.leave}", statusColor(Status.LEAVE), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Days attended", "${sum.attended}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile("Hours worked", fmtDur(sum.workedMs), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- HISTORY

@Composable
fun EmpHistory(vm: AppViewModel, emp: Employee) {
    val s by vm.settings.collectAsState()
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthStr)
    val recs by remember(month) { vm.monthRecords(month) }.collectAsState(initial = emptyList())
    val leaves by vm.leaves.collectAsState()
    val holidays by vm.holidays.collectAsState()
    val hol = remember(holidays) { holidays.map { it.date }.toSet() }
    val mine = remember(recs, emp.id) { recs.filter { it.empId == emp.id }.associateBy { it.date } }
    val sum = remember(recs, leaves, hol, s, emp, month) { summarize(emp, month, recs, leaves, hol, s) }
    val today = LocalDate.now()
    val days = remember(month, today) {
        (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { !it.isAfter(today) }.reversed()
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("History", "Your attendance record") }
        item { MonthSwitcher(month) { monthStr = it.toString() } }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                MonthCalendar(month, statusOf = { d -> resolveStatus(d, emp, mine[d.toString()], leaves, hol, s) })
                Spacer(Modifier.height(10.dp))
                StatusLegend()
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Present", "${sum.present}", statusColor(Status.PRESENT), Modifier.weight(1f))
                StatTile("Late", "${sum.late}", statusColor(Status.LATE), Modifier.weight(1f))
                StatTile("Half", "${sum.half}", statusColor(Status.HALF_DAY), Modifier.weight(1f))
                StatTile("Absent", "${sum.absent}", statusColor(Status.ABSENT), Modifier.weight(1f))
            }
        }
        item { SectionTitle("Daily log") }
        items(days, key = { it.toString() }) { d ->
            val rec = mine[d.toString()]
            val st = resolveStatus(d, emp, rec, leaves, hol, s)
            if (st != Status.NA) DayRow(d, st, rec)
        }
    }
}

@Composable
fun DayRow(date: LocalDate, status: String, rec: Attendance?) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${date.dayOfMonth}", style = MaterialTheme.typography.headlineSmall)
                Text(shortDay(date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                StatusChip(status)
                if (rec?.checkIn != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${fmtTime(rec?.checkIn)}  →  ${fmtTime(rec?.checkOut)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!rec?.note.isNullOrBlank()) {
                    Text(rec?.note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val inMs = rec?.checkIn
            val outMs = rec?.checkOut
            if (inMs != null && outMs != null) {
                Text(fmtDur((outMs - inMs).coerceAtLeast(0)), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

// ---------------------------------------------------------------- LEAVES

@Composable
fun EmpLeaves(vm: AppViewModel, emp: Employee) {
    val all by vm.leaves.collectAsState()
    val mine = remember(all, emp.id) { all.filter { it.empId == emp.id } }
    var dialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Leaves", "Apply and track your leave requests") }
        item {
            Button(
                onClick = { dialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Apply for leave")
            }
        }
        if (mine.isEmpty()) {
            item {
                Text(
                    "No leave requests yet.",
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(mine, key = { it.id }) { l ->
            LeaveCard(l, null) {
                if (l.status == LeaveStatus.PENDING) {
                    TextButton(onClick = { vm.cancelLeave(l.id) }) { Text("Cancel request") }
                }
            }
        }
    }

    if (dialog) {
        LeaveDialog(
            onDismiss = { dialog = false },
            onSubmit = { type, from, to, reason -> vm.applyLeave(emp.id, type, from, to, reason) }
        )
    }
}

@Composable
fun LeaveCard(l: LeaveRequest, ownerName: String?, actions: @Composable () -> Unit) {
    val from = LocalDate.parse(l.fromDate)
    val to = LocalDate.parse(l.toDate)
    val days = ChronoUnit.DAYS.between(from, to) + 1
    val fmt = DateTimeFormatter.ofPattern("dd MMM")
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (ownerName != null) Text(ownerName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${l.type} leave",
                    style = if (ownerName != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    color = if (ownerName != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            LeaveChip(l.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (days == 1L) from.format(fmt) else "${from.format(fmt)} - ${to.format(fmt)}  ($days days)",
            style = MaterialTheme.typography.titleSmall
        )
        if (l.reason.isNotBlank()) {
            Text(l.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

// ---------------------------------------------------------------- PROFILE

@Composable
fun EmpProfile(vm: AppViewModel, emp: Employee) {
    val s by vm.settings.collectAsState()
    var pinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(emp.name, 88.dp)
                Spacer(Modifier.height(10.dp))
                Text(emp.name, style = MaterialTheme.typography.headlineSmall)
                Text(emp.empCode, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                InfoRow("Department", emp.department)
                InfoRow("Designation", emp.designation)
                InfoRow("Phone", emp.phone)
                InfoRow("Joined", runCatching { LocalDate.parse(emp.joinDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy")) }.getOrDefault(emp.joinDate))
                InfoRow("Company", s.company)
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ThemeSelector(s.theme) { v -> vm.updateSettings { it.copy(theme = v) } }
            }
        }
        item {
            OutlinedButton(
                onClick = { pinDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Change PIN") }
        }
        item {
            OutlinedButton(
                onClick = { vm.logout() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log out")
            }
        }
    }

    if (pinDialog) {
        ChangePinDialog(
            onDismiss = { pinDialog = false },
            onSubmit = { old, new, done -> vm.changeEmployeePin(emp.id, old, new, done) }
        )
    }
}
