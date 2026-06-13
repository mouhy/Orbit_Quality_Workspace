package com.orbit.mobile.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSky
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTeal
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitOrange
import com.orbit.mobile.core.theme.OrbitSlate
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.EventDto
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// Type tone
fun eventTone(event: EventDto): Color {
    event.color?.let { hex ->
        runCatching {
            return Color(android.graphics.Color.parseColor(hex))
        }
    }
    return when (event.type) {
        "project_deadline", "task_deadline" -> OrbitDanger
        "review_session" -> OrbitPurple
        "training_session" -> OrbitSky
        "quality_audit" -> OrbitSuccess
        "team_event" -> OrbitOrange
        "reminder" -> OrbitWarning
        "personal_event" -> OrbitSlate
        "milestone" -> OrbitTeal
        else -> OrbitPrimary
    }
}

// Type label
@Composable
fun eventTypeLabel(type: String): String = when (type) {
    "project_deadline" -> stringResource(R.string.ev_type_project_deadline)
    "review_session" -> stringResource(R.string.ev_type_review_session)
    "training_session" -> stringResource(R.string.ev_type_training_session)
    "quality_audit" -> stringResource(R.string.ev_type_quality_audit)
    "team_event" -> stringResource(R.string.ev_type_team_event)
    "reminder" -> stringResource(R.string.ev_type_reminder)
    "personal_event" -> stringResource(R.string.ev_type_personal_event)
    "task_deadline" -> stringResource(R.string.ev_type_task_deadline)
    "milestone" -> stringResource(R.string.ev_type_milestone)
    else -> stringResource(R.string.ev_type_meeting)
}

// Calendar page
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var editEvent by remember { mutableStateOf<EventDto?>(null) }
    var detailEvent by remember { mutableStateOf<EventDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        // Month header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.setMonth(state.month.minusMonths(1)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = state.month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.setMonth(state.month.plusMonths(1)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.action_next),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            OrbitButton(
                text = stringResource(R.string.ev_new),
                onClick = { showForm = true },
                leadingIcon = Icons.Outlined.Add
            )
        }

        // Type filter
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = { viewModel.setTypeFilter(null) },
                shape = RoundedCornerShape(18.dp),
                color = if (state.typeFilter == null) OrbitPrimary else colors.surface2,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong)
            ) {
                Text(
                    text = stringResource(R.string.sp_filter_all),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (state.typeFilter == null) Color.White else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            EVENT_TYPES.forEach { type ->
                val active = state.typeFilter == type
                Surface(
                    onClick = { viewModel.setTypeFilter(if (active) null else type) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) OrbitPrimary else colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong)
                ) {
                    Text(
                        text = eventTypeLabel(type),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Month grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(14.dp))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            // Weekday row
            Row {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        text = day.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                        style = OrbitTextStyles.techLabel,
                        color = colors.textMuted,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            val firstDay = state.month.atDay(1)
            val offset = firstDay.dayOfWeek.value - 1
            val daysInMonth = state.month.lengthOfMonth()
            val cells = offset + daysInMonth
            val rows = (cells + 6) / 7
            repeat(rows) { row ->
                Row {
                    repeat(7) { col ->
                        val dayIndex = row * 7 + col - offset + 1
                        if (dayIndex in 1..daysInMonth) {
                            val date = state.month.atDay(dayIndex)
                            val isSelected = date == state.selectedDate
                            val isToday = date == LocalDate.now()
                            val dayEvents = state.eventsOn(date)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .padding(2.dp)
                                    .background(
                                        when {
                                            isSelected -> OrbitPrimary.copy(alpha = 0.14f)
                                            isToday -> colors.surface2
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectDate(date) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isToday || isSelected) FontWeight.ExtraBold
                                    else FontWeight.Medium,
                                    color = when {
                                        isSelected -> OrbitPrimary
                                        else -> colors.textPrimary
                                    }
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    dayEvents.take(3).forEach { event ->
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(eventTone(event), CircleShape)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )
                        }
                    }
                }
            }
        }

        // Day events
        Text(
            text = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary
        )
        when {
            state.loading -> LoadingHint()
            state.eventsOn(state.selectedDate).isEmpty() -> Text(
                text = stringResource(R.string.ev_no_events),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            else -> state.eventsOn(state.selectedDate).forEach { event ->
                val tone = eventTone(event)
                Surface(
                    onClick = { detailEvent = event },
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        tone.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp, 36.dp)
                                .background(tone, RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TinyPill(eventTypeLabel(event.type), tone)
                                event.startTime?.let {
                                    Text(
                                        text = it + (event.endTime?.let { e -> " – $e" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Form sheet
    if (showForm || editEvent != null) {
        EventFormSheet(
            editing = editEvent,
            initialDate = state.selectedDate,
            users = state.users,
            busy = state.busy,
            onDismiss = {
                showForm = false
                editEvent = null
            },
            onCreate = { body ->
                viewModel.create(body) { ok, _ -> if (ok) showForm = false }
            },
            onUpdate = { id, body ->
                viewModel.update(id, body) { ok, _ -> if (ok) editEvent = null }
            }
        )
    }

    // Detail sheet
    detailEvent?.let { event ->
        EventDetailSheet(
            event = event,
            isOwner = event.createdBy == viewModel.myUserId ||
                event.ownerId == viewModel.myUserId,
            onDismiss = { detailEvent = null },
            onEdit = {
                editEvent = event
                detailEvent = null
            },
            onDelete = {
                viewModel.delete(event.id) { detailEvent = null }
            },
            onRsvp = { status ->
                viewModel.rsvp(event.id, status)
                detailEvent = null
            }
        )
    }
}
