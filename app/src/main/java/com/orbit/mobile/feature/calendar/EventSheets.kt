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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.priorityColor
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.data.dto.EventCreateRequest
import com.orbit.mobile.data.dto.EventDto
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.TinyPill
import java.time.LocalDate

// Color presets
private val EVENT_COLORS = listOf(
    "#1D6EF5", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#06B6D4", "#F97316"
)

// Chip row
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            val active = selected == key
            Surface(
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(18.dp),
                color = if (active) OrbitPrimary else colors.surface2,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (active) OrbitPrimary else colors.borderStrong
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color.White else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// Field label
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = OrbitTextStyles.techLabel,
        color = OrbitTheme.colors.textMuted,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

// Event form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormSheet(
    editing: EventDto?,
    initialDate: LocalDate,
    users: List<UserDto>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (EventCreateRequest) -> Unit,
    onUpdate: (String, EventCreateRequest) -> Unit
) {
    val colors = OrbitTheme.colors
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var type by remember { mutableStateOf(editing?.type ?: "meeting") }
    var startDate by remember {
        mutableStateOf(editing?.startDate?.take(10) ?: initialDate.toString())
    }
    var endDate by remember { mutableStateOf(editing?.endDate?.take(10) ?: "") }
    var startTime by remember { mutableStateOf(editing?.startTime ?: "") }
    var endTime by remember { mutableStateOf(editing?.endTime ?: "") }
    var priority by remember { mutableStateOf(editing?.priority ?: "medium") }
    var location by remember { mutableStateOf(editing?.location ?: "") }
    var meetingLink by remember { mutableStateOf(editing?.meetingLink ?: "") }
    var visibility by remember { mutableStateOf(editing?.visibility ?: "workspace") }
    var reminder by remember { mutableStateOf(editing?.reminderMinutes) }
    var recurrence by remember { mutableStateOf(editing?.recurrence ?: "none") }
    var color by remember { mutableStateOf(editing?.color) }
    var attendees by remember {
        mutableStateOf(editing?.attendeeIds?.toSet() ?: emptySet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editing == null) stringResource(R.string.ev_create_title)
                else stringResource(R.string.ev_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )

            OrbitTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.ev_title_label),
                placeholder = stringResource(R.string.ev_title_placeholder)
            )

            Column {
                FieldLabel(stringResource(R.string.ev_type_label))
                ChipRow(
                    options = EVENT_TYPES.map { it to eventTypeLabel(it) },
                    selected = type,
                    onSelect = { type = it }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = stringResource(R.string.ev_start_date),
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
                OrbitTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = stringResource(R.string.ev_end_date),
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = stringResource(R.string.ev_start_time),
                    placeholder = "HH:MM",
                    modifier = Modifier.weight(1f)
                )
                OrbitTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = stringResource(R.string.ev_end_time),
                    placeholder = "HH:MM",
                    modifier = Modifier.weight(1f)
                )
            }

            Column {
                FieldLabel(stringResource(R.string.ct_priority_label))
                ChipRow(
                    options = EVENT_PRIORITIES.map { it to it.uppercase() },
                    selected = priority,
                    onSelect = { priority = it }
                )
            }

            OrbitTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.pj_desc_label),
                placeholder = stringResource(R.string.ev_desc_placeholder),
                singleLine = false,
                minLines = 2
            )

            OrbitTextField(
                value = location,
                onValueChange = { location = it },
                label = stringResource(R.string.ev_location_label),
                placeholder = stringResource(R.string.ev_location_placeholder)
            )

            OrbitTextField(
                value = meetingLink,
                onValueChange = { meetingLink = it },
                label = stringResource(R.string.ev_link_label),
                placeholder = "https://…"
            )

            Column {
                FieldLabel(stringResource(R.string.ev_visibility_label))
                ChipRow(
                    options = EVENT_VISIBILITIES.map {
                        it to when (it) {
                            "private" -> stringResource(R.string.ct_visibility_private)
                            "team" -> stringResource(R.string.ct_visibility_team)
                            "project_members" -> stringResource(R.string.ev_vis_project)
                            else -> stringResource(R.string.ev_vis_workspace)
                        }
                    },
                    selected = visibility,
                    onSelect = { visibility = it }
                )
            }

            Column {
                FieldLabel(stringResource(R.string.ev_reminder_label))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    REMINDER_OPTIONS.forEach { minutes ->
                        val active = reminder == minutes
                        Surface(
                            onClick = { reminder = if (active) null else minutes },
                            shape = RoundedCornerShape(18.dp),
                            color = if (active) OrbitWarning else colors.surface2,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                colors.borderStrong
                            )
                        ) {
                            Text(
                                text = when (minutes) {
                                    15 -> stringResource(R.string.ev_rem_15)
                                    60 -> stringResource(R.string.ev_rem_60)
                                    720 -> stringResource(R.string.ev_rem_720)
                                    else -> stringResource(R.string.ev_rem_1440)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Column {
                FieldLabel(stringResource(R.string.ev_recurrence_label))
                ChipRow(
                    options = EVENT_RECURRENCES.map {
                        it to when (it) {
                            "daily" -> stringResource(R.string.ev_rec_daily)
                            "weekly" -> stringResource(R.string.ev_rec_weekly)
                            "monthly" -> stringResource(R.string.ev_rec_monthly)
                            "yearly" -> stringResource(R.string.ev_rec_yearly)
                            else -> stringResource(R.string.ev_rec_none)
                        }
                    },
                    selected = recurrence,
                    onSelect = { recurrence = it }
                )
            }

            Column {
                FieldLabel(stringResource(R.string.ev_color_label))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EVENT_COLORS.forEach { hex ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        val active = color == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    if (active) 3.dp else 1.dp,
                                    if (active) colors.textPrimary else colors.border,
                                    CircleShape
                                )
                                .clickable { color = if (active) null else hex }
                        )
                    }
                }
            }

            Column {
                FieldLabel(stringResource(R.string.ev_attendees_label))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                        .padding(4.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    users.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = attendees.contains(user.id),
                                onCheckedChange = {
                                    attendees = if (attendees.contains(user.id)) {
                                        attendees - user.id
                                    } else attendees + user.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = OrbitPrimary)
                            )
                            HashAvatar(name = user.name, size = 24.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = OrbitButtonVariant.Ghost,
                    modifier = Modifier.weight(1f)
                )
                OrbitButton(
                    text = if (editing == null) stringResource(R.string.action_create)
                    else stringResource(R.string.action_save),
                    onClick = {
                        val body = EventCreateRequest(
                            title = title.trim(),
                            description = description.ifBlank { null },
                            type = type,
                            startDate = startDate.trim(),
                            endDate = endDate.trim().ifBlank { null },
                            startTime = startTime.trim().ifBlank { null },
                            endTime = endTime.trim().ifBlank { null },
                            priority = priority,
                            location = location.ifBlank { null },
                            meetingLink = meetingLink.ifBlank { null },
                            attendeeIds = attendees.toList(),
                            visibility = visibility,
                            reminderMinutes = reminder,
                            recurrence = recurrence,
                            color = color
                        )
                        if (editing == null) onCreate(body) else onUpdate(editing.id, body)
                    },
                    enabled = title.isNotBlank() && startDate.isNotBlank() && !busy,
                    loading = busy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Event detail
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: EventDto,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRsvp: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    val tone = eventTone(event)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(tone, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TinyPill(eventTypeLabel(event.type), tone)
                TinyPill(event.priority.uppercase(), priorityColor(event.priority))
                TinyPill(event.visibility, colors.textSecondary)
            }
            event.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            DetailRow(
                stringResource(R.string.ev_start_date),
                event.startDate + (event.startTime?.let { " $it" } ?: "")
            )
            event.endDate?.let {
                DetailRow(
                    stringResource(R.string.ev_end_date),
                    it + (event.endTime?.let { e -> " $e" } ?: "")
                )
            }
            event.location?.takeIf { it.isNotBlank() }?.let {
                DetailRow(stringResource(R.string.ev_location_label), it)
            }
            event.meetingLink?.takeIf { it.isNotBlank() }?.let {
                DetailRow(stringResource(R.string.ev_link_label), it)
            }
            if (event.reminderMinutes != null) {
                DetailRow(
                    stringResource(R.string.ev_reminder_label),
                    "${event.reminderMinutes}m"
                )
            }
            if (event.recurrence != "none") {
                DetailRow(stringResource(R.string.ev_recurrence_label), event.recurrence)
            }
            if (event.attendeeIds.isNotEmpty()) {
                DetailRow(
                    stringResource(R.string.ev_attendees_label),
                    event.attendeeIds.size.toString()
                )
            }

            // RSVP
            FieldLabel(stringResource(R.string.ev_rsvp_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitButton(
                    text = stringResource(R.string.ev_rsvp_attending),
                    onClick = { onRsvp("attending") },
                    modifier = Modifier.weight(1f)
                )
                OrbitButton(
                    text = stringResource(R.string.ev_rsvp_tentative),
                    onClick = { onRsvp("tentative") },
                    variant = OrbitButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                OrbitButton(
                    text = stringResource(R.string.ev_rsvp_declined),
                    onClick = { onRsvp("declined") },
                    variant = OrbitButtonVariant.Ghost,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isOwner) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrbitButton(
                        text = stringResource(R.string.action_edit),
                        onClick = onEdit,
                        variant = OrbitButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    OrbitButton(
                        text = stringResource(R.string.action_delete),
                        onClick = onDelete,
                        variant = OrbitButtonVariant.Danger,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Detail row
@Composable
private fun DetailRow(label: String, value: String) {
    val colors = OrbitTheme.colors
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary
        )
    }
}
