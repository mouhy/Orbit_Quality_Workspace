package com.orbit.mobile.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.projectStatusColor
import com.orbit.mobile.core.ui.components.EmptyState
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitBadge
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.ui.components.StaggeredAppear
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.KpiCard
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.dashboard.daysUntil
import com.orbit.mobile.feature.dashboard.deadlineShortLabel
import com.orbit.mobile.feature.shell.InnerRoutes

// Projects page
@Composable
fun ProjectsScreen(
    onNavigate: (String) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showCreateProject by remember { mutableStateOf(false) }
    var editProject by remember { mutableStateOf<ProjectDto?>(null) }
    var deleteProject by remember { mutableStateOf<ProjectDto?>(null) }
    var showCreateTeam by remember { mutableStateOf(false) }
    var editTeamDetails by remember { mutableStateOf<TeamDto?>(null) }
    var viewMembersTeam by remember { mutableStateOf<TeamDto?>(null) }
    var editMembersTeam by remember { mutableStateOf<TeamDto?>(null) }
    var deleteTeam by remember { mutableStateOf<TeamDto?>(null) }
    var sheetError by remember { mutableStateOf<UiText?>(null) }

    // Return refresh
    OnReturnRefresh { viewModel.refresh() }

    // Pull refresh
    OrbitPullRefresh(
        refreshing = state.loading,
        onRefresh = { viewModel.refresh() }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        // Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProjectsTab.entries.forEach { tab ->
                val active = state.tab == tab
                val label = when (tab) {
                    ProjectsTab.OVERVIEW -> stringResource(R.string.pj_tab_overview)
                    ProjectsTab.PROJECTS -> stringResource(R.string.nav_projects)
                    ProjectsTab.TEAMS -> stringResource(R.string.title_teams)
                }
                Surface(
                    onClick = { viewModel.setTab(tab) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) OrbitPrimary else colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) OrbitPrimary else colors.borderStrong
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        when (state.tab) {
            ProjectsTab.OVERVIEW -> OverviewTab(state)
            ProjectsTab.PROJECTS -> ProjectsTabContent(
                state = state,
                onSearch = viewModel::setSearch,
                onOpen = { onNavigate("${InnerRoutes.WORKSPACE}?projectId=$it") },
                onCreate = { showCreateProject = true },
                onEdit = { editProject = it },
                onDelete = { deleteProject = it },
                onStatus = viewModel::changeStatus
            )
            ProjectsTab.TEAMS -> TeamsTabContent(
                state = state,
                onCreate = { showCreateTeam = true },
                onView = { viewMembersTeam = it },
                onEditMembers = { editMembersTeam = it },
                onEditDetails = { editTeamDetails = it },
                onDelete = { deleteTeam = it }
            )
        }
    }
    }

    // Project sheets
    if (showCreateProject || editProject != null) {
        ProjectFormSheet(
            editing = editProject,
            teams = state.teams,
            busy = state.busy,
            loadMembers = { viewModel.loadMembers(it) },
            onDismiss = {
                showCreateProject = false
                editProject = null
                sheetError = null
            },
            onCreate = { body ->
                viewModel.createProject(body) { ok, err ->
                    if (ok) {
                        showCreateProject = false
                        sheetError = null
                    } else sheetError = err
                }
            },
            onUpdate = { id, body ->
                viewModel.updateProject(id, body) { ok, err ->
                    if (ok) {
                        editProject = null
                        sheetError = null
                    } else sheetError = err
                }
            },
            externalError = sheetError
        )
    }

    // Team sheets
    if (showCreateTeam || editTeamDetails != null) {
        TeamFormSheet(
            editing = editTeamDetails,
            users = state.users,
            currentRole = viewModel.currentRole,
            busy = state.busy,
            onDismiss = {
                showCreateTeam = false
                editTeamDetails = null
                sheetError = null
            },
            onCreate = { body ->
                viewModel.createTeam(body) { ok, err ->
                    if (ok) {
                        showCreateTeam = false
                        sheetError = null
                    } else sheetError = err
                }
            },
            onUpdate = { id, name, desc ->
                viewModel.updateTeam(id, name, desc) { ok, err ->
                    if (ok) {
                        editTeamDetails = null
                        sheetError = null
                    } else sheetError = err
                }
            },
            externalError = sheetError
        )
    }

    viewMembersTeam?.let { team ->
        TeamMembersSheet(
            team = team,
            loadMembers = { viewModel.loadMembers(it) },
            onDismiss = { viewMembersTeam = null }
        )
    }

    editMembersTeam?.let { team ->
        EditTeamMembersSheet(
            team = team,
            users = state.users,
            busy = state.busy,
            loadMembers = { viewModel.loadMembers(it) },
            onDismiss = { editMembersTeam = null },
            onSave = { current, targets ->
                viewModel.syncMembers(team.teamId, current, targets) {
                    editMembersTeam = null
                }
            }
        )
    }

    // Delete dialogs
    deleteProject?.let { project ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.pj_delete_title),
            message = stringResource(R.string.pj_delete_message, project.title),
            busy = state.busy,
            onConfirm = { viewModel.deleteProject(project.projectId) { deleteProject = null } },
            onDismiss = { deleteProject = null }
        )
    }
    deleteTeam?.let { team ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.tm_delete_title),
            message = stringResource(R.string.tm_delete_message, team.name),
            busy = state.busy,
            onConfirm = { viewModel.deleteTeam(team.teamId) { deleteTeam = null } },
            onDismiss = { deleteTeam = null }
        )
    }
}

// Delete dialog
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = OrbitDanger
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

// Overview tab
@Composable
private fun OverviewTab(state: ProjectsState) {
    val cp = state.customProjects
    val active = cp.count { it.status.uppercase() == "ACTIVE" }
    val done = cp.count { it.status.uppercase() == "COMPLETED" }
    val onHold = cp.count {
        it.status.uppercase().replace("-", "_").replace(" ", "_") == "ON_HOLD"
    }
    val spark = listOf(0, 1, 1, 2, 2, cp.size)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                label = stringResource(R.string.kpi_total_projects),
                value = cp.size.toString(),
                color = OrbitPrimary,
                spark = spark,
                loading = state.loading,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.kpi_active_projects),
                value = active.toString(),
                color = OrbitSuccess,
                spark = spark,
                loading = state.loading,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                label = stringResource(R.string.status_completed),
                value = done.toString(),
                color = OrbitPurple,
                spark = spark,
                loading = state.loading,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.status_on_hold),
                value = onHold.toString(),
                color = OrbitWarning,
                spark = spark,
                loading = state.loading,
                modifier = Modifier.weight(1f)
            )
        }
        DashCard {
            Text(
                text = stringResource(R.string.pj_recent).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = OrbitTheme.colors.textMuted
            )
            Spacer(Modifier.height(8.dp))
            if (state.loading) {
                LoadingHint()
            } else {
                cp.sortedByDescending { it.updatedAt ?: "" }.take(5).forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(projectStatusColor(p.status), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = OrbitTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeAgo(p.updatedAt).asString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OrbitTheme.colors.textMuted
                        )
                    }
                }
            }
        }
        DashCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = OrbitPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.pj_teams_count, state.teams.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = OrbitTheme.colors.textPrimary
                )
            }
        }
    }
}

// Projects tab
@Composable
private fun ProjectsTabContent(
    state: ProjectsState,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (ProjectDto) -> Unit,
    onDelete: (ProjectDto) -> Unit,
    onStatus: (String, String) -> Unit
) {
    val colors = OrbitTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbitTextField(
                value = state.search,
                onValueChange = onSearch,
                placeholder = stringResource(R.string.field_search_hint),
                modifier = Modifier.weight(1f),
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            OrbitButton(
                text = stringResource(R.string.action_create),
                onClick = onCreate,
                leadingIcon = Icons.Outlined.Add
            )
        }

        when {
            state.loading -> LoadingHint()
            state.filteredProjects.isEmpty() -> EmptyState(
                title = stringResource(R.string.dash_no_projects),
                description = stringResource(R.string.state_empty_desc)
            )
            else -> state.filteredProjects.forEachIndexed { index, p ->
                // Card entrance
                StaggeredAppear(index = index) {
                    ProjectCard(
                        project = p,
                        onOpen = { onOpen(p.projectId) },
                        onEdit = { onEdit(p) },
                        onDelete = { onDelete(p) },
                        onStatus = { onStatus(p.projectId, it) }
                    )
                }
            }
        }
    }
}

// Project card
@Composable
private fun ProjectCard(
    project: ProjectDto,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatus: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    val tone = projectStatusColor(project.status)
    val days = daysUntil(project.dueDate)
    var statusMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = project.description?.ifBlank { null }
                            ?: stringResource(R.string.pj_no_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { statusMenu = true },
                    color = Color.Transparent
                ) {
                    OrbitBadge(text = project.status, color = tone)
                }
            }
            Spacer(Modifier.height(10.dp))

            // Progress bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(colors.border, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((project.progress / 100f).coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(tone, RoundedCornerShape(5.dp))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${project.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Team avatars
                val team = project.subAdmins.mapNotNull { it.name } +
                    project.staff.mapNotNull { it.name }
                Row(modifier = Modifier.weight(1f)) {
                    team.take(4).forEach { n ->
                        Box(modifier = Modifier.padding(end = 2.dp)) {
                            HashAvatar(name = n, size = 20.dp)
                        }
                    }
                    if (team.size > 4) {
                        TinyPill("+${team.size - 4}", tone)
                    }
                }
                if (days != null) {
                    Text(
                        text = deadlineShortLabel(days),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            days < 0 -> OrbitDanger
                            days <= 3 -> OrbitWarning
                            else -> colors.textMuted
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = timeAgo(project.updatedAt).asString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = OrbitDanger,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Status switch
            if (statusMenu) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ACTIVE", "ON_HOLD", "COMPLETED").forEach { s ->
                        Surface(
                            onClick = {
                                statusMenu = false
                                onStatus(s)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = projectStatusColor(s).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = s.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = projectStatusColor(s),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Teams tab
@Composable
private fun TeamsTabContent(
    state: ProjectsState,
    onCreate: () -> Unit,
    onView: (TeamDto) -> Unit,
    onEditMembers: (TeamDto) -> Unit,
    onEditDetails: (TeamDto) -> Unit,
    onDelete: (TeamDto) -> Unit
) {
    val colors = OrbitTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OrbitButton(
            text = stringResource(R.string.tm_create_title),
            onClick = onCreate,
            leadingIcon = Icons.Outlined.Add,
            fullWidth = true
        )
        when {
            state.loading -> LoadingHint()
            state.teams.isEmpty() -> EmptyState(
                title = stringResource(R.string.tm_no_teams),
                description = stringResource(R.string.state_empty_desc)
            )
            else -> state.teams.forEach { team ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        OrbitPurple.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = team.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OrbitPurple
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = team.description?.ifBlank { null }
                                        ?: stringResource(R.string.tm_no_description),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OrbitBadge(
                                text = stringResource(
                                    R.string.tm_members_count,
                                    team.memberCount ?: 1
                                ),
                                color = OrbitPrimary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TeamAction(
                                icon = Icons.Outlined.Visibility,
                                label = stringResource(R.string.tm_view_members)
                            ) { onView(team) }
                            TeamAction(
                                icon = Icons.Outlined.Group,
                                label = stringResource(R.string.tm_edit_members_title)
                            ) { onEditMembers(team) }
                            TeamAction(
                                icon = Icons.Outlined.Edit,
                                label = stringResource(R.string.action_edit)
                            ) { onEditDetails(team) }
                            TeamAction(
                                icon = Icons.Outlined.Delete,
                                label = stringResource(R.string.action_delete),
                                tint = OrbitDanger
                            ) { onDelete(team) }
                        }
                    }
                }
            }
        }
    }
}

// Team action
@Composable
private fun TeamAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = OrbitTheme.colors.textSecondary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = OrbitTheme.colors.surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, OrbitTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
