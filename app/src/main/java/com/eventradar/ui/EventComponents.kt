@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.eventradar.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.eventradar.data.EventEntity
import com.eventradar.data.interests
import com.eventradar.data.location
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

fun EventEntity.dateLabel(): String {
    val d = LocalDate.parse(startDate).format(dayFmt)
    return if (startTime != null) "$d · $startTime" else d
}

/** Image with a coloured placeholder showing the interest initial while loading / if missing. */
@Composable
fun EventThumb(event: EventEntity, modifier: Modifier = Modifier) {
    val tag = event.interests.firstOrNull() ?: event.title
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(tagColor(tag).copy(alpha = 0.85f), PanelHigh))
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                event.title.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(PanelHigh)) {
        if (event.imageUrl == null) fallback() else SubcomposeAsyncImage(
            model = event.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { fallback() },
            error = { fallback() },
        )
    }
}

@Composable
fun InterestTag(name: String) {
    val c = tagColor(name)
    Text(
        name.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = c,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun EventCard(event: EventEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Panel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            EventThumb(event, Modifier.width(112.dp).height(84.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(event.dateLabel(), style = MaterialTheme.typography.bodySmall, color = Signal)
                event.location?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    event.interests.firstOrNull()?.let { InterestTag(it) }
                    Text(
                        event.price ?: "Price TBC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (event.price != null) MaterialTheme.colorScheme.onSurface else Muted,
                    )
                }
            }
        }
    }
}

@Composable
fun EventDetailSheet(event: EventEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Panel,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EventThumb(event, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                event.interests.forEach { InterestTag(it) }
            }
            Text(event.title, style = MaterialTheme.typography.headlineSmall)

            DetailRow(Icons.Filled.DateRange, event.dateLabel())
            DetailRow(Icons.Filled.Place, event.location ?: "Venue TBC")
            DetailRow(Icons.Filled.ShoppingCart, event.price ?: "Price not listed — check the event page")
            DetailRow(Icons.Filled.Info, "Found via ${event.source}")

            Button(
                onClick = { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(event.url))) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Flare),
            ) { Text("Open event page", fontWeight = FontWeight.SemiBold) }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { addToCalendar(context, event) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Add to calendar")
                }
                OutlinedButton(onClick = { share(context, event) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to handle this", Toast.LENGTH_SHORT).show()
    }
}

/** Opens the phone's calendar app pre-filled — no calendar permission needed. */
private fun addToCalendar(context: Context, e: EventEntity) {
    val zone = ZoneId.of("Europe/London")
    val date = LocalDate.parse(e.startDate)
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, e.title)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, e.location ?: "")
        .putExtra(CalendarContract.Events.DESCRIPTION, listOfNotNull(e.price, e.url).joinToString("\n"))
    if (e.startTime != null) {
        val start = date.atTime(LocalTime.parse(e.startTime)).atZone(zone)
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toInstant().toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start.plusHours(3).toInstant().toEpochMilli())
    } else {
        // All-day events are expressed in UTC midnight-to-midnight.
        val start = date.atStartOfDay(ZoneId.of("UTC"))
        intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toInstant().toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start.plusDays(1).toInstant().toEpochMilli())
    }
    open(context, intent)
}

private fun share(context: Context, e: EventEntity) {
    val text = listOfNotNull(e.title, e.dateLabel(), e.location, e.url).joinToString("\n")
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    open(context, Intent.createChooser(send, "Share event"))
}
