package com.hazri.attendance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

object Status {
    const val PRESENT = "PRESENT"
    const val LATE = "LATE"
    const val HALF_DAY = "HALF_DAY"
    const val ABSENT = "ABSENT"
    const val LEAVE = "LEAVE"
    const val HOLIDAY = "HOLIDAY"
    const val WEEKOFF = "WEEKOFF"
    const val PENDING = "PENDING"
    const val FUTURE = "FUTURE"
    const val NA = "NA"
}

object LeaveStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object Role {
    const val NONE = "NONE"
    const val ADMIN = "ADMIN"
    const val EMP = "EMP"
}

@Entity(tableName = "employees", indices = [Index(value = ["empCode"], unique = true)])
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empCode: String,
    val name: String,
    val phone: String = "",
    val department: String = "",
    val designation: String = "",
    val pin: String = "1234",
    val active: Boolean = true,
    val joinDate: String = LocalDate.now().toString()
)

@Entity(tableName = "attendance", indices = [Index(value = ["empId", "date"], unique = true)])
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empId: Long,
    val date: String,
    val checkIn: Long? = null,
    val checkOut: Long? = null,
    val status: String,
    val note: String = ""
)

@Entity(tableName = "leaves")
data class LeaveRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empId: Long,
    val type: String,
    val fromDate: String,
    val toDate: String,
    val reason: String = "",
    val status: String = LeaveStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "holidays", indices = [Index(value = ["date"], unique = true)])
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val name: String
)

data class Session(val role: String = Role.NONE, val empId: Long = 0)
