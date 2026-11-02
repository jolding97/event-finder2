@file:OptIn(ExperimentalMaterial3Api::class)

package com.eventradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eventradar.data.EventEntity
import com.eventradar.data.interests
import com.eventradar.data.location
import com.eventradar.data.Interest
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
private val groupFmt = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun CalendarScreen(
    events: List<EventEntity>,
    interests: List<Interest>,
    hasSources: Boolean,
    onEventClick: (EventEntity) -> Unit,
    onGoToInterests: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    if (!hasSources || interests.isEmpty()) {
        Onboarding(hasSources, interests.isNotEmpty(), onGoToInterests, onGoToSettings)
        return
    }

    var mode by rememberSaveable { mutableStateOf(0) }  // 0 = month, 1 = upcoming list
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDay by remember { mutableStateOf(today) }

    val visible = if (filter == null) events else events.filter { filter in it.interests }
    val byDay = remember(visible) { visible.groupBy { LocalDate.parse(it.startDate) } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            TabRow(
                selectedTabIndex = mode,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { pos ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[mode]), color = Flare)
                },
                divider = {},
            ) {
                Tab(selected = mode == 0, onClick = { mode = 0 }, text = { Text("Month") })
                Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text("Upcoming (${visible.size})") })
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("All", filter == null) { filter = null } }
                items(interests, key = { it.id }) { i ->
                    Chip(i.name, filter == i.name, tagColor(i.name)) { filter = if (filter == i.name) null else i.name }
                }
            }
        }

        if (mode == 0) {
            item {
                MonthGrid(
                    month = month,
                    today = today,
                    selected = selectedDay,
                    byDay = byDay,
                    onPrev = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                    onSelect = { selectedDay = it },
                )
            }
            item {
                Text(
                    selectedDay.format(groupFmt),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            val dayEvents = byDay[selectedDay].orEmpty()
            if (dayEvents.isEmpty()) {
                item { Text("Nothing on this day. Tap a day with a picture or dot.", color = Muted) }
            } else {
                items(dayEvents, key = { it.id }) { EventCard(it) { onEventClick(it) } }
            }
        } else {
            if (visible.isEmpty()) {
                item {
                    Text(
                        "No events yet. Tap refresh at the top, or try broader interests.",
                        color = Muted, modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            byDay.toSortedMap().forEach { (day, list) ->
                item(key = "h$day") {
                    Text(
                        if (day == today) "Today" else day.format(groupFmt),
                        style = MaterialTheme.typography.titleSmall,
                        color = Signal,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(list, key = { it.id }) { EventCard(it) { onEventClick(it) } }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, color: Color = Flare, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color,
        ),
    )
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    byDay: Map<LocalDate, List<EventEntity>>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Panel).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            Text(
                month.format(monthFmt),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
        }
        Row {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, color = Muted, style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        // Monday-first grid; leading blanks for the days before the 1st.
        val lead = month.atDay(1).dayOfWeek.value - 1
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(0.78f)) {
                        if (day != null) DayCell(day, byDay[day].orEmpty(), day == today, day == selected, day.isBefore(today)) {
                            onSelect(day)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    events: List<EventEntity>,
    isToday: Boolean,
    isSelected: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val withImage = events.firstOrNull { it.imageUrl != null } ?: events.firstOrNull()
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (events.isNotEmpty()) PanelHigh else Color.Transparent)
            .then(if (isSelected) Modifier.border(2.dp, Flare, shape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        if (withImage != null) {
            EventThumb(withImage, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99000000), Color(0x22000000), Color(0xAA000000)))))
        }
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
            color = when {
                isToday -> Flare
                isPast -> Muted.copy(alpha = 0.5f)
                else -> Color.White
            },
            modifier = Modifier.align(Alignment.TopStart).padding(5.dp),
        )
        if (events.isNotEmpty()) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                events.flatMap { it.interests }.distinct().take(3).forEach {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(tagColor(it)))
                }
                if (events.size > 1) {
                    Text("${events.size}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Onboarding(hasSources: Boolean, hasInterests: Boolean, onInterests: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your events, found for you", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Tell EventRadar what you're into — racing, rally, your favourite bands, teams — then tap refresh and it fills this calendar with UK events.",
            color = Muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Step(1, "Add a free API key (skip for racing only)", done = hasSources, onClick = onSettings)
        Spacer(Modifier.height(10.dp))
        Step(2, "Add your interests", done = hasInterests, onClick = onInterests)
    }
}

@Composable
private fun Step(n: Int, label: String, done: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !done,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Flare),
    ) { Text(if (done) "✓  $label" else "$n.  $label", fontWeight = FontWeight.SemiBold) }
}
