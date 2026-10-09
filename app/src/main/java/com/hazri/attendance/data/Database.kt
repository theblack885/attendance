package com.hazri.attendance.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Employees
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployee(e: Employee): Long

    @Query("SELECT * FROM employees ORDER BY name COLLATE NOCASE")
    fun employees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE empCode = :code COLLATE NOCASE LIMIT 1")
    suspend fun employeeByCode(code: String): Employee?

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployee(id: Long)

    @Query("DELETE FROM attendance WHERE empId = :id")
    suspend fun deleteAttendanceOf(id: Long)

    @Query("DELETE FROM leaves WHERE empId = :id")
    suspend fun deleteLeavesOf(id: Long)

    // Attendance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(a: Attendance): Long

    @Query("SELECT * FROM attendance WHERE empId = :empId AND date = :date LIMIT 1")
    suspend fun attendanceOn(empId: Long, date: String): Attendance?

    @Query("SELECT * FROM attendance WHERE empId = :empId AND date = :date LIMIT 1")
    fun attendanceOnFlow(empId: Long, date: String): Flow<Attendance?>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :from AND :to")
    fun attendanceBetween(from: String, to: String): Flow<List<Attendance>>

    @Query("DELETE FROM attendance WHERE empId = :empId AND date = :date")
    suspend fun clearAttendance(empId: Long, date: String)

    // Leaves
    @Insert
    suspend fun insertLeave(l: LeaveRequest): Long

    @Query("SELECT * FROM leaves ORDER BY createdAt DESC")
    fun leaves(): Flow<List<LeaveRequest>>

    @Query("UPDATE leaves SET status = :status WHERE id = :id")
    suspend fun setLeaveStatus(id: Long, status: String)

    @Query("DELETE FROM leaves WHERE id = :id")
    suspend fun deleteLeave(id: Long)

    // Holidays
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHoliday(h: Holiday): Long

    @Query("SELECT * FROM holidays ORDER BY date")
    fun holidays(): Flow<List<Holiday>>

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHoliday(id: Long)
}

@Database(
    entities = [Employee::class, Attendance::class, LeaveRequest::class, Holiday::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hazri.db"
            ).build().also { instance = it }
        }
    }
}
