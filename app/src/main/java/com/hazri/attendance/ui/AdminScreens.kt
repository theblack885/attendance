@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hazri.attendance.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hazri.attendance.AppViewModel
import com.hazri.attendance.data.Employee
import com.hazri.attendance.data.LeaveStatus
import com.hazri.attendance.data.Status
import com.hazri.attendance.data.resolveStatus
import com.hazri.attendance.data.summarize
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val ListPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)

// ---------------------------------------------------------------- DASHBOARD

@Composable
fun AdminDashboard(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val emps by vm.employees.collectAsState()
    val leaves by vm.leaves.collectAsState()
    val holidays by vm.holidays.collectAsState()
    var dateStr by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(dateStr)
    val recs by remember(date) { vm.attendanceForDate(date) }.collectAsState(initial = emptyList())
    val hol = remember(holidays) { holidays.map { it.date }.toSet() }
    val active = remember(emps) { emps.filter { it.active } }
    val byEmp = remember(recs) { recs.associateBy { it.empId } }
    val rows = remember(active, byEmp, leaves, hol, s, date) {
        active.map { e -> e to resolveStatus(date, e, byEmp[e.id], leaves, hol, s) }
    }
    val present = rows.count { it.second == Status.PRESENT }
    val late = rows.count { it.second == Status.LATE }
    val half = rows.count { it.second == Status.HALF_DAY }
    val onLeave = rows.count { it.second == Status.LEAVE }
    val absent = rows.count { it.second == Status.ABSENT || it.second == Status.PENDING }
    val attended = present + late + half
    val rate = if (active.isEmpty()) 0f else attended.toFloat() / active.size
    var markFor by remember { mutableStateOf<Employee?>(null) }

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Dashboard", s.company) }
        item { DaySwitcher(date) { dateStr = it.toString() } }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Attendance rate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(rate * 100).toInt()}%", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { rate },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
                Text("$attended of ${active.size} employees attended", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Present", "$present", statusColor(Status.PRESENT), Modifier.weight(1f))
                    StatTile("Late", "$late", statusColor(Status.LATE), Modifier.weight(1f))
                    StatTile("Half day", "$half", statusColor(Status.HALF_DAY), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("On leave", "$onLeave", statusColor(Status.LEAVE), Modifier.weight(1f))
                    StatTile("Absent / not in", "$absent", statusColor(Status.ABSENT), Modifier.weight(1f))
                    StatTile("Total staff", "${active.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
        }
        item { SectionTitle("Employees") }
        if (rows.isEmpty()) {
            item {
                Text(
                    "No employees yet. Go to the Staff tab to add your first employee.",
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(rows, key = { it.first.id }) { (e, st) ->
            val rec = byEmp[e.id]
            AppCard(Modifier.fillMaxWidth(), onClick = { markFor = e }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(e.name, 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (rec?.checkIn != null) "${fmtTime(rec?.checkIn)}  →  ${fmtTime(rec?.checkOut)}"
                            else e.department.ifBlank { e.empCode },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusChip(st)
                }
            }
        }
    }

    markFor?.let { e ->
        MarkDialog(
            emp = e,
            date = date,
            current = byEmp[e.id],
            settings = s,
            onDismiss = { markFor = null },
            onSave = { status, inT, outT, note -> vm.markAttendance(e.id, date, status, inT, outT, note) },
            onReset = { vm.clearAttendance(e.id, date) }
        )
    }
}

// ---------------------------------------------------------------- STAFF

@Composable
fun AdminStaff(vm: AppViewModel) {
    val emps by vm.employees.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Employee?>(null) }
    var adding by remember { mutableStateOf(false) }
    val filtered = remember(emps, query) {
        emps.filter { it.name.contains(query, true) || it.empCode.contains(query, true) || it.department.contains(query, true) }
    }

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Staff", "${emps.size} employees") }
        item {
            Button(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add employee")
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search name, ID or department") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )
        }
        items(filtered, key = { it.id }) { e ->
            AppCard(Modifier.fillMaxWidth(), onClick = { editing = e }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(e.name, 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(e.empCode, e.designation, e.department).filter { it.isNotBlank() }.joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!e.active) {
                        Text("Inactive", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (adding) {
        EmployeeDialog(
            initial = null,
            suggestedCode = "EMP" + (emps.size + 1).toString().padStart(3, '0'),
            onDismiss = { adding = false },
            onSave = { e, done -> vm.saveEmployee(e, done) },
            onDelete = null
        )
    }
    editing?.let { cur ->
        EmployeeDialog(
            initial = cur,
            suggestedCode = cur.empCode,
            onDismiss = { editing = null },
            onSave = { e, done -> vm.saveEmployee(e, done) },
            onDelete = { vm.deleteEmployee(it.id) }
        )
    }
}

// ---------------------------------------------------------------- LEAVES

@Composable
fun AdminLeaves(vm: AppViewModel) {
    val all by vm.leaves.collectAsState()
    val emps by vm.employees.collectAsState()
    var filter by rememberSaveable { mutableStateOf(LeaveStatus.PENDING) }
    val names = remember(emps) { emps.associate { it.id to it.name } }
    val shown = remember(all, filter) { if (filter == "ALL") all else all.filter { it.status == filter } }

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Leave requests", "${all.count { it.status == LeaveStatus.PENDING }} pending") }
        item {
            ChoiceChips(
                listOf(LeaveStatus.PENDING to "Pending", LeaveStatus.APPROVED to "Approved", LeaveStatus.REJECTED to "Rejected", "ALL" to "All"),
                filter
            ) { filter = it }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    "Nothing here.",
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(shown, key = { it.id }) { l ->
            LeaveCard(l, names[l.empId] ?: "Unknown") {
                if (l.status == LeaveStatus.PENDING) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { vm.decideLeave(l.id, false) }, modifier = Modifier.weight(1f)) { Text("Reject") }
                        Button(onClick = { vm.decideLeave(l.id, true) }, modifier = Modifier.weight(1f)) { Text("Approve") }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- REPORTS

@Composable
fun AdminReports(vm: AppViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val emps by vm.employees.collectAsState()
    val leaves by vm.leaves.collectAsState()
    val holidays by vm.holidays.collectAsState()
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthStr)
    val recs by remember(month) { vm.monthRecords(month) }.collectAsState(initial = emptyList())
    val hol = remember(holidays) { holidays.map { it.date }.toSet() }
    val active = remember(emps) { emps.filter { it.active } }
    val sums = remember(active, recs, leaves, hol, s, month) { active.map { it to summarize(it, month, recs, leaves, hol, s) } }
    var detail by remember { mutableStateOf<Employee?>(null) }

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Reports", "Monthly attendance summary") }
        item { MonthSwitcher(month) { monthStr = it.toString() } }
        item {
            Button(
                onClick = {
                    vm.exportCsv(month) { file ->
                        if (file == null) {
                            vm.message.value = "Could not create the report"
                        } else {
                            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Attendance report $month")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Share attendance report"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export CSV (Excel)")
            }
        }
        if (sums.isEmpty()) {
            item {
                Text(
                    "Add employees to see reports.",
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(sums, key = { it.first.id }) { (e, sum) ->
            AppCard(Modifier.fillMaxWidth(), onClick = { detail = e }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(e.name, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.empCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(fmtDur(sum.workedMs), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Present", "${sum.present}", statusColor(Status.PRESENT), Modifier.weight(1f))
                    StatTile("Late", "${sum.late}", statusColor(Status.LATE), Modifier.weight(1f))
                    StatTile("Half", "${sum.half}", statusColor(Status.HALF_DAY), Modifier.weight(1f))
                    StatTile("Absent", "${sum.absent}", statusColor(Status.ABSENT), Modifier.weight(1f))
                    StatTile("Leave", "${sum.leave}", statusColor(Status.LEAVE), Modifier.weight(1f))
                }
            }
        }
    }

    detail?.let { e ->
        val mine = recs.filter { it.empId == e.id }.associateBy { it.date }
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text("${e.name} - " + month.format(DateTimeFormatter.ofPattern("MMM yyyy"))) },
            text = {
                Column {
                    MonthCalendar(month, statusOf = { d -> resolveStatus(d, e, mine[d.toString()], leaves, hol, s) })
                    Spacer(Modifier.height(8.dp))
                    StatusLegend()
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Close") } }
        )
    }
}

// ---------------------------------------------------------------- SETTINGS

@Composable
fun AdminSettings(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val holidays by vm.holidays.collectAsState()
    var company by remember { mutableStateOf(s.company) }
    var pinDialog by remember { mutableStateOf(false) }
    var holidayDialog by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Settings", "Rules, holidays and security") }

        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Company", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it; vm.updateSettings { st -> st.copy(company = it) } },
                    label = { Text("Company name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Shift & rules", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField("Shift start", s.start, { t -> vm.updateSettings { it.copy(shiftStart = t.toString()) } }, Modifier.weight(1f))
                    TimeField("Shift end", s.end, { t -> vm.updateSettings { it.copy(shiftEnd = t.toString()) } }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Stepper("Late after (grace)", "${s.graceMin} min", onMinus = {
                    vm.updateSettings { it.copy(graceMin = (it.graceMin - 5).coerceAtLeast(0)) }
                }, onPlus = {
                    vm.updateSettings { it.copy(graceMin = (it.graceMin + 5).coerceAtMost(120)) }
                })
                Stepper("Half day if worked under", "${s.halfDayHours} hrs", onMinus = {
                    vm.updateSettings { it.copy(halfDayHours = (it.halfDayHours - 1).coerceAtLeast(1)) }
                }, onPlus = {
                    vm.updateSettings { it.copy(halfDayHours = (it.halfDayHours + 1).coerceAtMost(12)) }
                })
            }
        }

        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Weekly off", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (d in 1..7) {
                        val label = java.time.DayOfWeek.of(d).getDisplayName(JTextStyle.SHORT, Locale.ENGLISH)
                        FilterChip(
                            selected = d in s.weeklyOff,
                            onClick = {
                                vm.updateSettings { st ->
                                    st.copy(weeklyOff = if (d in st.weeklyOff) st.weeklyOff - d else st.weeklyOff + d)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Holidays", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { holidayDialog = true }) { Text("+ Add") }
                }
                if (holidays.isEmpty()) {
                    Text("No holidays added.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                holidays.forEach { h ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                            Text(h.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                runCatching { LocalDate.parse(h.date).format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) }.getOrDefault(h.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.deleteHoliday(h.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete holiday", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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
            ) { Text("Change admin PIN") }
        }
        item {
            Button(
                onClick = { vm.logout() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pinDialog) {
        ChangePinDialog(onDismiss = { pinDialog = false }, onSubmit = { old, new, done -> vm.changeAdminPin(old, new, done) })
    }
    if (holidayDialog) {
        HolidayDialog(onDismiss = { holidayDialog = false }, onSave = { d, n -> vm.addHoliday(d, n) })
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onMinus, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("-") }
        Text(value, Modifier.width(78.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = onPlus, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("+") }
    }
}
