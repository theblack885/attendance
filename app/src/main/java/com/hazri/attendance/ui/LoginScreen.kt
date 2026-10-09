@file:OptIn(ExperimentalMaterial3Api::class)

package com.hazri.attendance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hazri.attendance.AppViewModel

@Composable
fun LoginScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    var admin by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(BrandGradient)) {
        // Hero
        Column(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Text("Hazri", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text(
                s.company,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                "Smart attendance, anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Form sheet
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Welcome back", style = MaterialTheme.typography.headlineSmall)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !admin, onClick = { admin = false; error = null }, label = { Text("Employee") })
                        FilterChip(selected = admin, onClick = { admin = true; error = null }, label = { Text("Admin") })
                    }
                }

                if (!admin) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.replace(" ", ""); error = null },
                        label = { Text("Employee ID") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
                PinField(if (admin) "Admin PIN" else "PIN", pin) { pin = it; error = null }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (admin) vm.loginAdmin(pin) { error = it }
                        else if (code.isBlank() || pin.isBlank()) error = "Enter your Employee ID and PIN"
                        else vm.loginEmployee(code, pin) { error = it }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Log in", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                if (admin && s.adminPin == "0000") {
                    Text(
                        "First time? The default admin PIN is 0000. Please change it from Settings after logging in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!admin) {
                    Text(
                        "Your Employee ID and PIN are given by your admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
