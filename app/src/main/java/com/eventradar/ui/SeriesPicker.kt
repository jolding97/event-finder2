@file:OptIn(ExperimentalMaterial3Api::class)

package com.eventradar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eventradar.data.Interest
import com.eventradar.sources.Series
import com.eventradar.sources.SeriesCatalog

/**
 * Tick racing series to follow. Each ticked series becomes an interest backed by its calendar feed;
 * unticking removes it (and its events).
 */
@Composable
fun SeriesPickerSheet(
    interests: List<Interest>,
    onFollow: (Series, ukOnly: Boolean) -> Unit,
    onUnfollow: (Interest) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var ukOnly by remember { mutableStateOf(false) }
    val followed = interests.filter { it.feedUrl != null }.associateBy { it.feedUrl!!.lowercase() }
    val matches = SeriesCatalog.all.filter {
        query.isBlank() || it.name.contains(query.trim(), true) || it.group.contains(query.trim(), true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
    ) {
        Column(Modifier.fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
            Text("Racing calendars", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tick a series to put every round on your calendar. Feeds from toomuchracing.com — " +
                    "next season's dates appear as they're announced.",
                style = MaterialTheme.typography.bodySmall, color = Muted,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search e.g. F1, rally, superbikes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Only UK rounds for series I tick now")
                    Text(
                        "Off = every round worldwide (great for watching on TV). Change per series later on the Interests tab.",
                        style = MaterialTheme.typography.bodySmall, color = Muted,
                    )
                }
                Switch(checked = ukOnly, onCheckedChange = { ukOnly = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Flare))
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                SeriesCatalog.groups.forEach { group ->
                    val inGroup = matches.filter { it.group == group }
                    if (inGroup.isEmpty()) return@forEach
                    item(key = "g-$group") {
                        Text(
                            group.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Signal,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                        )
                    }
                    items(inGroup, key = { it.feedUrl }) { series ->
                        val existing = followed[series.feedUrl.lowercase()]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (existing != null) onUnfollow(existing) else onFollow(series, ukOnly)
                                }
                                .padding(vertical = 2.dp),
                        ) {
                            Checkbox(
                                checked = existing != null,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = Flare),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(series.name)
                                if (existing != null) {
                                    Text(
                                        if (existing.ukOnly) "Following · UK rounds only" else "Following · all rounds",
                                        style = MaterialTheme.typography.bodySmall, color = Muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
