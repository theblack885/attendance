@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hazri.attendance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hazri.attendance.data.AppSettings
import com.hazri.attendance.data.Attendance
import com.hazri.attendance.data.Employee
import com.hazri.attendance.data.Status
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier, shape = MaterialTheme.shapes.small) {
        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), style = MaterialTheme.typography.titleSmall)
        }
    }
    if (open) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = st) }
    }
}

@Composable
fun TimeField(label: String, time: LocalTime, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier, shape = MaterialTheme.shapes.small) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(time.format(DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.titleSmall)
        }
    }
    if (open) {
        val st = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimeInput(state = st) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalTime.of(st.hour, st.minute))
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PinField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(8)) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") } }
    )
}

@Composable
fun ChoiceChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
fun ThemeSelector(current: String, onChange: (String) -> Unit) {
    ChoiceChips(listOf("system" to "System", "light" to "Light", "dark" to "Dark"), current, onChange)
}

@Composable
fun ChangePinDialog(onDismiss: () -> Unit, onSubmit: (String, String, (String?) -> Unit) -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField("Current PIN", old) { old = it }
                PinField("New PIN (min 4 digits)", new) { new = it }
                PinField("Confirm new PIN", confirm) { confirm = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (new != confirm) error = "PINs do not match"
                else onSubmit(old, new) { err -> if (err == null) onDismiss() else error = err }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

val LeaveTypes = listOf("Casual", "Sick", "Paid", "Unpaid")

@Composable
fun LeaveDialog(onDismiss: () -> Unit, onSubmit: (String, LocalDate, LocalDate, String) -> Unit) {
    var type by remember { mutableStateOf(LeaveTypes.first()) }
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply for leave") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceChips(LeaveTypes.map { it to it }, type) { type = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField("From", from, { from = it; if (to.isBefore(it)) to = it }, Modifier.weight(1f))
                    DateField("To", to, { to = it }, Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = MaterialTheme.shapes.small
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    to.isBefore(from) -> error = "End date cannot be before start date"
                    reason.isBlank() -> error = "Please add a reason"
                    else -> {
                        onSubmit(type, from, to, reason)
                        onDismiss()
                    }
                }
            }) { Text("Send request") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Admin: set / correct the attendance of one employee on one day. */
@Composable
fun MarkDialog(
    emp: Employee,
    date: LocalDate,
    current: Attendance?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (String, LocalTime?, LocalTime?, String) -> Unit,
    onReset: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    fun toTime(ms: Long?, fallback: LocalTime): LocalTime =
        if (ms == null) fallback else Instant.ofEpochMilli(ms).atZone(zone).toLocalTime().withSecond(0).withNano(0)

    var status by remember { mutableStateOf(current?.status ?: Status.PRESENT) }
    var inT by remember { mutableStateOf(toTime(current?.checkIn, settings.start)) }
    var outT by remember { mutableStateOf(toTime(current?.checkOut, settings.end)) }
    var note by remember { mutableStateOf(current?.note ?: "") }
    val needsTimes = status == Status.PRESENT || status == Status.LATE || status == Status.HALF_DAY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(emp.name)
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceChips(
                    listOf(
                        Status.PRESENT to "Present",
                        Status.LATE to "Late",
                        Status.HALF_DAY to "Half day",
                        Status.ABSENT to "Absent",
                        Status.LEAVE to "Leave"
                    ),
                    status
                ) { status = it }
                if (needsTimes) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeField("Check-in", inT, { inT = it }, Modifier.weight(1f))
                        TimeField("Check-out", outT, { outT = it }, Modifier.weight(1f))
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                if (current != null) {
                    TextButton(onClick = { onReset(); onDismiss() }) { Text("Reset to automatic") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(status, if (needsTimes) inT else null, if (needsTimes) outT else null, note)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Admin: add / edit an employee. [initial] null means "new". */
@Composable
fun EmployeeDialog(
    initial: Employee?,
    suggestedCode: String,
    onDismiss: () -> Unit,
    onSave: (Employee, (String?) -> Unit) -> Unit,
    onDelete: ((Employee) -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var code by remember { mutableStateOf(initial?.empCode ?: suggestedCode) }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var dept by remember { mutableStateOf(initial?.department ?: "") }
    var role by remember { mutableStateOf(initial?.designation ?: "") }
    var pin by remember { mutableStateOf(initial?.pin ?: "1234") }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var joined by remember { mutableStateOf(initial?.joinDate?.let { LocalDate.parse(it) } ?: LocalDate.now()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add employee" else "Edit employee") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val shape = MaterialTheme.shapes.small
                OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = shape)
                OutlinedTextField(code, { code = it.replace(" ", "") }, label = { Text("Employee ID (used to log in)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = shape)
                OutlinedTextField(
                    phone, { phone = it.filter { c -> c.isDigit() || c == '+' } }, label = { Text("Phone") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = shape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(dept, { dept = it }, label = { Text("Department") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = shape)
                OutlinedTextField(role, { role = it }, label = { Text("Designation") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = shape)
                PinField("Login PIN (min 4 digits)", pin) { pin = it }
                DateField("Joining date", joined, { joined = it }, Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Active", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete employee", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    name.isBlank() -> error = "Name is required"
                    code.isBlank() -> error = "Employee ID is required"
                    pin.length < 4 -> error = "PIN must be at least 4 digits"
                    else -> {
                        val e = (initial ?: Employee(empCode = code, name = name)).copy(
                            empCode = code, name = name, phone = phone, department = dept.trim(),
                            designation = role.trim(), pin = pin, active = active, joinDate = joined.toString()
                        )
                        onSave(e) { err -> if (err == null) onDismiss() else error = err }
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (confirmDelete && initial != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${initial.name}?") },
            text = { Text("All attendance and leave records of this employee will also be removed. This cannot be undone. (Tip: switch Active off instead to keep history.)") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    onDelete(initial)
                    onDismiss()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun HolidayDialog(onDismiss: () -> Unit, onSave: (LocalDate, String) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add holiday") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField("Date", date, { date = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    name, { name = it }, label = { Text("Holiday name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) error = "Please enter a name" else {
                    onSave(date, name)
                    onDismiss()
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
