package com.eventradar.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eventradar.data.AppSettings
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Section("Event sources") {
            Text(
                "Both are free — add either or both. Keys stay on this phone.",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
            KeyField(
                label = "Ticketmaster API key",
                help = "Free. Best for concerts, sport, comedy and theatre.",
                signupUrl = "https://developer.ticketmaster.com/",
                value = settings.ticketmasterKey,
                onValue = { v -> onChange { it.copy(ticketmasterKey = v) } },
            )
            KeyField(
                label = "Skiddle API key",
                help = "Free. UK gigs, club nights and festivals.",
                signupUrl = "https://www.skiddle.com/api/join.php",
                value = settings.skiddleKey,
                onValue = { v -> onChange { it.copy(skiddleKey = v) } },
            )
            Text(
                "Free, no key needed: attach a calendar feed (.ics / webcal link) to any interest " +
                    "on the Interests tab — ideal for racing and rally series.",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
        }

        Section("Searching") {
            Text("Default look-ahead: ${settings.monthsAhead} month${if (settings.monthsAhead == 1) "" else "s"}")
            Text("Used by the daily refresh; you can pick a different period each time you tap refresh.", style = MaterialTheme.typography.bodySmall, color = Muted)
            Slider(
                value = settings.monthsAhead.toFloat(),
                onValueChange = { v -> onChange { it.copy(monthsAhead = v.toInt()) } },
                valueRange = 1f..12f,
                steps = 10,
                colors = SliderDefaults.colors(thumbColor = Flare, activeTrackColor = Flare),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Refresh automatically each day")
                    Text("Runs in the background on any network", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Switch(
                    checked = settings.autoRefresh,
                    onCheckedChange = { v -> onChange { it.copy(autoRefresh = v) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Flare),
                )
            }
            Text(
                if (settings.lastRefresh == 0L) "Never refreshed"
                else "Last full refresh: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(settings.lastRefresh)),
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = Flare),
                    modifier = Modifier.weight(1f),
                ) { Text("Find events now") }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear calendar") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun KeyField(label: String, help: String, signupUrl: String, value: String, onValue: (String) -> Unit) {
    val context = LocalContext.current
    var reveal by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text(label) },
            singleLine = true,
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide" else "Show") }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(help, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(signupUrl))) }
            }) { Text("Get key", color = Signal) }
        }
    }
}
