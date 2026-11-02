@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.eventradar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val periods = listOf(1 to "1 month", 3 to "3 months", 6 to "6 months", 12 to "12 months")
private val untilFmt = DateTimeFormatter.ofPattern("d MMMM yyyy")

/** Asked when the user taps refresh: how far ahead should we look? */
@Composable
fun RefreshDialog(
    interestCount: Int,
    defaultMonths: Int,
    onSearch: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var months by remember { mutableIntStateOf(defaultMonths) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Find events") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Search all $interestCount interest${if (interestCount == 1) "" else "s"} for UK events coming up in the next:",
                    color = Muted,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEach { (m, label) ->
                        FilterChip(
                            selected = months == m,
                            onClick = { months = m },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Flare.copy(alpha = 0.2f),
                                selectedLabelColor = Flare,
                            ),
                        )
                    }
                }
                Text(
                    "Up to ${LocalDate.now().plusMonths(months.toLong()).format(untilFmt)}",
                    style = MaterialTheme.typography.bodySmall, color = Signal,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSearch(months) },
                enabled = interestCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Flare),
            ) { Text("Search") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
