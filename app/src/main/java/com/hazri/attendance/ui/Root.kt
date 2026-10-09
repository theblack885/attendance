@file:OptIn(ExperimentalMaterial3Api::class)

package com.hazri.attendance.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hazri.attendance.AppViewModel
import com.hazri.attendance.data.Employee
import com.hazri.attendance.data.Role

@Composable
fun HazriApp(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    HazriTheme(s.theme) {
        val session by vm.session.collectAsState()
        val emps by vm.employees.collectAsState()
        val snack = remember { SnackbarHostState() }
        val msg by vm.message.collectAsState()

        LaunchedEffect(msg) {
            val m = msg
            if (m != null) {
                snack.showSnackbar(m)
                vm.message.value = null
            }
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (session.role) {
                Role.ADMIN -> AdminShell(vm)
                Role.EMP -> {
                    val emp = emps.find { it.id == session.empId }
                    if (emp != null && emp.active) {
                        EmployeeShell(vm, emp)
                    } else {
                        Column(
                            Modifier.fillMaxSize().statusBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            TextButton(onClick = { vm.logout() }) { Text("Back to login") }
                        }
                    }
                }
                else -> LoginScreen(vm)
            }
            SnackbarHost(
                snack,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp)
            )
        }
    }
}

@Composable
fun EmployeeShell(vm: AppViewModel, emp: Employee) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val items = listOf(
        NavItem("Today", Icons.Rounded.Home),
        NavItem("History", Icons.Rounded.DateRange),
        NavItem("Leaves", Icons.Rounded.Create),
        NavItem("Profile", Icons.Rounded.Person)
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { FloatingNav(items, tab) { tab = it } }
    ) { inner ->
        Box(Modifier.padding(bottom = inner.calculateBottomPadding()).statusBarsPadding()) {
            Crossfade(targetState = tab, label = "emp-tab") { t ->
                when (t) {
                    0 -> EmpHome(vm, emp)
                    1 -> EmpHistory(vm, emp)
                    2 -> EmpLeaves(vm, emp)
                    else -> EmpProfile(vm, emp)
                }
            }
        }
    }
}

@Composable
fun AdminShell(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val items = listOf(
        NavItem("Home", Icons.Rounded.Home),
        NavItem("Staff", Icons.Rounded.AccountCircle),
        NavItem("Leaves", Icons.Rounded.Create),
        NavItem("Reports", Icons.Rounded.Info),
        NavItem("Settings", Icons.Rounded.Settings)
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { FloatingNav(items, tab) { tab = it } }
    ) { inner ->
        Box(Modifier.padding(bottom = inner.calculateBottomPadding()).statusBarsPadding()) {
            Crossfade(targetState = tab, label = "admin-tab") { t ->
                when (t) {
                    0 -> AdminDashboard(vm)
                    1 -> AdminStaff(vm)
                    2 -> AdminLeaves(vm)
                    3 -> AdminReports(vm)
                    else -> AdminSettings(vm)
                }
            }
        }
    }
}
