package com.orbit.mobile.feature.projects

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitAvatar
import com.orbit.mobile.core.ui.components.OrbitBadge
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.CreateProjectRequest
import com.orbit.mobile.data.dto.CreateTeamRequest
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.data.dto.UpdateProjectRequest
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.HashAvatar
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Status options
private val STATUS_OPTIONS = listOf("ACTIVE", "ON_HOLD", "COMPLETED")

@Composable
private fun statusLabel(value: String): String = when (value) {
    "ON_HOLD" -> stringResource(R.string.status_on_hold)
    "COMPLETED" -> stringResource(R.string.status_completed)
    else -> stringResource(R.string.status_active)
}

// Sheet title
@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    val colors = OrbitTheme.colors
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}

// Status picker
@Composable
fun StatusChips(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        STATUS_OPTIONS.forEach { option ->
            val active = selected == option
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(20.dp),
                color = if (active) OrbitPrimary else OrbitTheme.colors.surface2,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (active) OrbitPrimary else OrbitTheme.colors.borderStrong
                )
            ) {
                Text(
                    text = statusLabel(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) Color.White else OrbitTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// Member pick
@Composable
fun MemberPickList(
    title: String,
    subtitle: String,
    people: List<Triple<String, String, String>>,
    selected: Set<String>,
    loading: Boolean,
    emptyText: String,
    onToggle: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
            Text(
                text = stringResource(R.string.pj_selected_count, selected.size),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 170.dp)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .padding(6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                loading -> Text(
                    text = stringResource(R.string.state_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(10.dp)
                )
                people.isEmpty() -> Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(10.dp)
                )
                else -> people.forEach { (id, name, email) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected.contains(id),
                            onCheckedChange = { onToggle(id) },
                            colors = CheckboxDefaults.colors(checkedColor = OrbitPrimary)
                        )
                        HashAvatar(name = name, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Project form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormSheet(
    editing: ProjectDto?,
    teams: List<TeamDto>,
    busy: Boolean,
    loadMembers: suspend (String) -> List<MembershipDto>,
    onDismiss: () -> Unit,
    onCreate: (CreateProjectRequest) -> Unit,
    onUpdate: (String, UpdateProjectRequest) -> Unit,
    externalError: UiText?
) {
    val colors = OrbitTheme.colors
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var status by remember {
        mutableStateOf(
            (editing?.status ?: "ACTIVE").uppercase().replace(" ", "_").replace("-", "_")
        )
    }
    var progress by remember { mutableStateOf((editing?.progress ?: 0).toFloat()) }
    var dueDate by remember { mutableStateOf(editing?.dueDate?.take(10) ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    var directoryLoading by remember { mutableStateOf(true) }
    var subManagers by remember { mutableStateOf<List<MembershipDto>>(emptyList()) }
    var staff by remember { mutableStateOf<List<MembershipDto>>(emptyList()) }
    var selectedSubs by remember {
        mutableStateOf(editing?.subAdmins?.mapNotNull { it.id }?.toSet() ?: emptySet())
    }
    var selectedStaff by remember {
        mutableStateOf(editing?.staff?.mapNotNull { it.id }?.toSet() ?: emptySet())
    }
    val teamId = teams.firstOrNull()?.teamId

    // Load directory
    LaunchedEffect(teamId) {
        directoryLoading = true
        if (teamId != null) {
            val members = loadMembers(teamId)
            subManagers = members.filter { it.role == "sub_admin" || it.role == "subadmin" }
            staff = members.filter { it.role == "member" || it.role == "staff" }
        }
        directoryLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SheetTitle(
                title = if (editing == null) stringResource(R.string.pj_create_title)
                else stringResource(R.string.pj_edit_title),
                subtitle = stringResource(R.string.pj_create_sub)
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                OrbitTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.pj_name_label),
                    placeholder = stringResource(R.string.pj_name_placeholder),
                    enabled = !busy
                )
                Column {
                    Text(
                        text = stringResource(R.string.pj_status_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    StatusChips(selected = status, onSelect = { status = it })
                }
                OrbitTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.pj_desc_label),
                    placeholder = stringResource(R.string.pj_desc_placeholder),
                    singleLine = false,
                    minLines = 3,
                    enabled = !busy
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.pj_progress_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${progress.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = OrbitPrimary,
                            activeTrackColor = OrbitPrimary
                        )
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.pj_due_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (colors.isDark) colors.surface2 else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            colors.borderStrong
                        )
                    ) {
                        Text(
                            text = dueDate.ifBlank { stringResource(R.string.pj_due_placeholder) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dueDate.isBlank()) colors.textMuted else colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                }
                MemberPickList(
                    title = stringResource(R.string.pj_sub_managers),
                    subtitle = stringResource(R.string.pj_sub_managers_sub),
                    people = subManagers.map {
                        Triple(it.userId, it.userFullName ?: it.userEmail ?: "", it.userEmail ?: "")
                    },
                    selected = selectedSubs,
                    loading = directoryLoading,
                    emptyText = stringResource(R.string.pj_no_sub_managers),
                    onToggle = { id ->
                        selectedSubs =
                            if (selectedSubs.contains(id)) selectedSubs - id else selectedSubs + id
                    }
                )
                MemberPickList(
                    title = stringResource(R.string.pj_staff),
                    subtitle = stringResource(R.string.pj_staff_sub),
                    people = staff.map {
                        Triple(it.userId, it.userFullName ?: it.userEmail ?: "", it.userEmail ?: "")
                    },
                    selected = selectedStaff,
                    loading = directoryLoading,
                    emptyText = stringResource(R.string.pj_no_staff),
                    onToggle = { id ->
                        selectedStaff =
                            if (selectedStaff.contains(id)) selectedStaff - id
                            else selectedStaff + id
                    }
                )
                externalError?.let { AuthErrorBox(message = it.asString()) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrbitButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = onDismiss,
                        variant = OrbitButtonVariant.Ghost,
                        modifier = Modifier.weight(1f)
                    )
                    OrbitButton(
                        text = if (editing == null) stringResource(R.string.pj_create_button)
                        else stringResource(R.string.action_save),
                        onClick = {
                            if (editing == null) {
                                onCreate(
                                    CreateProjectRequest(
                                        title = title,
                                        description = description,
                                        status = status,
                                        progress = progress.toInt(),
                                        dueDate = dueDate.ifBlank { null },
                                        subAdminIds = selectedSubs.toList(),
                                        staffIds = selectedStaff.toList(),
                                        teamId = teamId
                                    )
                                )
                            } else {
                                onUpdate(
                                    editing.projectId,
                                    UpdateProjectRequest(
                                        title = title,
                                        description = description,
                                        status = status,
                                        progress = progress.toInt(),
                                        dueDate = dueDate.ifBlank { null },
                                        subAdminIds = selectedSubs.toList(),
                                        staffIds = selectedStaff.toList()
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && !busy,
                        loading = busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Date dialog
    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// Team form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamFormSheet(
    editing: TeamDto?,
    users: List<UserDto>,
    currentRole: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateTeamRequest) -> Unit,
    onUpdate: (String, String, String) -> Unit,
    externalError: UiText?
) {
    val colors = OrbitTheme.colors
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var managers by remember { mutableStateOf(emptySet<String>()) }
    var subManagers by remember { mutableStateOf(emptySet<String>()) }
    var staff by remember { mutableStateOf(emptySet<String>()) }

    // Role hierarchy: admin picks sub-admins + staff; sub-admin only staff
    val canPickHigher = currentRole == "admin"

    val managerUsers = users.filter { it.role == "admin" || it.role == "manager" }
    val subUsers = users.filter { it.role == "sub_admin" }
    val staffUsers = users.filter { it.role == "staff" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SheetTitle(
                title = if (editing == null) stringResource(R.string.tm_create_title)
                else stringResource(R.string.tm_edit_title)
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                OrbitTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.tm_name_label),
                    placeholder = stringResource(R.string.tm_name_placeholder),
                    enabled = !busy
                )
                OrbitTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.pj_desc_label),
                    placeholder = stringResource(R.string.tm_desc_placeholder),
                    singleLine = false,
                    minLines = 2,
                    enabled = !busy
                )
                if (editing == null) {
                    // Admin-only pickers
                    if (canPickHigher) {
                        MemberPickList(
                            title = stringResource(R.string.tm_managers),
                            subtitle = stringResource(R.string.tm_managers_sub),
                            people = managerUsers.map { Triple(it.id, it.name, it.email) },
                            selected = managers,
                            loading = false,
                            emptyText = stringResource(R.string.tm_no_users),
                            onToggle = { id ->
                                managers = if (managers.contains(id)) managers - id else managers + id
                            }
                        )
                        MemberPickList(
                            title = stringResource(R.string.pj_sub_managers),
                            subtitle = stringResource(R.string.pj_sub_managers_sub),
                            people = subUsers.map { Triple(it.id, it.name, it.email) },
                            selected = subManagers,
                            loading = false,
                            emptyText = stringResource(R.string.pj_no_sub_managers),
                            onToggle = { id ->
                                subManagers =
                                    if (subManagers.contains(id)) subManagers - id else subManagers + id
                            }
                        )
                    }
                    // Staff picker (all)
                    MemberPickList(
                        title = stringResource(R.string.pj_staff),
                        subtitle = stringResource(R.string.pj_staff_sub),
                        people = staffUsers.map { Triple(it.id, it.name, it.email) },
                        selected = staff,
                        loading = false,
                        emptyText = stringResource(R.string.pj_no_staff),
                        onToggle = { id ->
                            staff = if (staff.contains(id)) staff - id else staff + id
                        }
                    )
                }
                externalError?.let { AuthErrorBox(message = it.asString()) }
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
                            if (editing == null) {
                                onCreate(
                                    CreateTeamRequest(
                                        name = name.trim(),
                                        description = description.trim(),
                                        managerIds = managers.toList(),
                                        subManagerIds = subManagers.toList(),
                                        staffIds = staff.toList()
                                    )
                                )
                            } else {
                                onUpdate(editing.teamId, name.trim(), description.trim())
                            }
                        },
                        enabled = name.isNotBlank() && !busy,
                        loading = busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Members viewer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMembersSheet(
    team: TeamDto,
    loadMembers: suspend (String) -> List<MembershipDto>,
    onDismiss: () -> Unit
) {
    val colors = OrbitTheme.colors
    var loading by remember { mutableStateOf(true) }
    var members by remember { mutableStateOf<List<MembershipDto>>(emptyList()) }

    LaunchedEffect(team.teamId) {
        loading = true
        members = loadMembers(team.teamId)
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            SheetTitle(
                title = team.name,
                subtitle = stringResource(R.string.tm_members_title)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    loading -> Text(
                        text = stringResource(R.string.state_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    members.isEmpty() -> Text(
                        text = stringResource(R.string.tm_no_members),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    else -> members.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrbitAvatar(
                                name = m.userFullName ?: m.userEmail ?: "U",
                                size = 36.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = m.userFullName
                                        ?: stringResource(R.string.tm_unknown_user),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = m.userEmail ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            OrbitBadge(text = m.role, color = OrbitPrimary)
                        }
                    }
                }
            }
        }
    }
}

// Members editor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTeamMembersSheet(
    team: TeamDto,
    users: List<UserDto>,
    busy: Boolean,
    loadMembers: suspend (String) -> List<MembershipDto>,
    onDismiss: () -> Unit,
    onSave: (List<MembershipDto>, Map<String, String>) -> Unit
) {
    val colors = OrbitTheme.colors
    var loading by remember { mutableStateOf(true) }
    var current by remember { mutableStateOf<List<MembershipDto>>(emptyList()) }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(team.teamId) {
        loading = true
        current = loadMembers(team.teamId)
        selected = current.map { it.userId }.toSet()
        loading = false
    }

    // Role mapping
    fun membershipRole(user: UserDto): String = when (user.role) {
        "admin", "manager" -> "manager"
        "sub_admin" -> "subadmin"
        else -> "member"
    }

    val eligible = users.filter { it.role in setOf("admin", "manager", "sub_admin", "staff") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            SheetTitle(
                title = team.name,
                subtitle = stringResource(R.string.tm_edit_members_title)
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (loading) {
                        Text(
                            text = stringResource(R.string.state_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        eligible.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected.contains(user.id),
                                    onCheckedChange = {
                                        selected = if (selected.contains(user.id)) {
                                            selected - user.id
                                        } else {
                                            selected + user.id
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = OrbitPrimary)
                                )
                                HashAvatar(name = user.name, size = 28.dp)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OrbitBadge(text = user.role, color = OrbitPrimary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrbitButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = onDismiss,
                        variant = OrbitButtonVariant.Ghost,
                        modifier = Modifier.weight(1f)
                    )
                    OrbitButton(
                        text = stringResource(R.string.action_save),
                        onClick = {
                            val targets = eligible
                                .filter { selected.contains(it.id) }
                                .associate { it.id to membershipRole(it) }
                            onSave(current, targets)
                        },
                        enabled = !busy && !loading,
                        loading = busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
