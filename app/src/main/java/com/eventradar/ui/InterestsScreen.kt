@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.eventradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eventradar.data.EventEntity
import com.eventradar.data.interests
import com.eventradar.data.location
import com.eventradar.data.Interest

private val categories = listOf("Motorsport", "Music", "Sport", "Comedy", "Theatre", "Festival", "Other")

private val suggestions = listOf(
    "WEC" to "Motorsport",
    "British Rally Championship" to "Motorsport",
    "BTCC" to "Motorsport",
    "British Superbikes" to "Motorsport",
    "Goodwood" to "Motorsport",
    "Premiership Rugby" to "Sport",
    "Six Nations" to "Sport",
    "Stand-up comedy" to "Comedy",
)

@Composable
fun InterestsScreen(
    interests: List<Interest>,
    events: List<EventEntity>,
    onAdd: (String, String, String?, Boolean) -> Unit,
    onUpdate: (Interest) -> Unit,
    onDelete: (Interest) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Music") }
    var feed by rememberSaveable { mutableStateOf("") }
    var ukOnly by rememberSaveable { mutableStateOf(true) }
    var showFeed by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Interest?>(null) }
    val focus = LocalFocusManager.current
    val counts = remember(events) {
        events.flatMap { it.interests }.groupingBy { it }.eachCount()
    }

    fun submit() {
        if (text.isBlank()) return
        onAdd(text, category, feed.ifBlank { null }, ukOnly)
        text = ""
        feed = ""
        showFeed = false
        focus.clearFocus()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(
                Modifier.clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("What are you into?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Be specific — an artist, a team, a racing series or a type of event. " +
                        "Each interest is searched across the UK.",
                    style = MaterialTheme.typography.bodySmall, color = Muted,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("e.g. Foo Fighters, WEC, Leeds Rhinos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { submit() },
                        enabled = text.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Flare),
                    ) { Icon(Icons.Filled.Add, "Add interest") }
                }
                if (showFeed) {
                    FeedFields(feed, { feed = it }, ukOnly, { ukOnly = it })
                } else {
                    TextButton(onClick = { showFeed = true }) {
                        Text("+ Add a calendar feed (optional, free)", color = Signal)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Flare.copy(alpha = 0.2f),
                                selectedLabelColor = Flare,
                            ),
                        )
                    }
                }
            }
        }

        val unused = suggestions.filter { s -> interests.none { it.name.equals(s.first, ignoreCase = true) } }
        if (unused.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quick add", style = MaterialTheme.typography.labelLarge, color = Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        unused.forEach { (name, cat) ->
                            AssistChip(onClick = { onAdd(name, cat, null, true) }, label = { Text("+ $name") })
                        }
                    }
                }
            }
        }

        item {
            Text(
                if (interests.isEmpty()) "No interests yet" else "Your interests (${interests.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(interests, key = { it.id }) { interest ->
            Surface(
                onClick = { editing = interest },
                color = Panel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(tagColor(interest.name)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(interest.name, style = MaterialTheme.typography.titleMedium)
                        val n = counts[interest.name] ?: 0
                        Text(
                            listOfNotNull(
                                interest.category,
                                if (n == 1) "1 event" else "$n events",
                                interest.feedUrl?.let { if (interest.ukOnly) "feed · UK only" else "feed" },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = Muted,
                        )
                    }
                    IconButton(onClick = { onDelete(interest) }) {
                        Icon(Icons.Filled.Delete, "Remove ${interest.name}", tint = Muted)
                    }
                }
            }
        }
    }

    editing?.let { i ->
        EditInterestDialog(
            interest = i,
            onSave = { onUpdate(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun FeedFields(url: String, onUrl: (String) -> Unit, ukOnly: Boolean, onUkOnly: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrl,
            label = { Text("Calendar feed link (.ics or webcal://)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Many series publish a subscribable calendar — look for \"Add to calendar\", " +
                "\"Subscribe\" or \"iCal\" on the series' website or on fan calendar sites, and copy the link.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Only keep UK rounds", modifier = Modifier.weight(1f))
            Switch(
                checked = ukOnly,
                onCheckedChange = onUkOnly,
                colors = SwitchDefaults.colors(checkedTrackColor = Flare),
            )
        }
    }
}

@Composable
private fun EditInterestDialog(interest: Interest, onSave: (Interest) -> Unit, onDismiss: () -> Unit) {
    var feed by remember { mutableStateOf(interest.feedUrl.orEmpty()) }
    var ukOnly by remember { mutableStateOf(interest.ukOnly) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(interest.name) },
        text = { FeedFields(feed, { feed = it }, ukOnly, { ukOnly = it }) },
        confirmButton = {
            Button(
                onClick = { onSave(interest.copy(feedUrl = feed.trim().ifBlank { null }, ukOnly = ukOnly)) },
                colors = ButtonDefaults.buttonColors(containerColor = Flare),
            ) { Text("Save & search") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
