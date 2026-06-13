package com.orbit.mobile.feature.itportal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.orbit.mobile.core.theme.OrbitTeal
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.Downloader
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.SessionLogDto
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.KpiCard
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.projects.ConfirmDeleteDialog
import kotlinx.coroutines.launch

// Ten console sections mirroring the web ITPortal sidebar
private enum class ItSection {
    DASHBOARD, USERS, ROLES, SECURITY, SESSIONS, AUDIT, HEALTH, SETTINGS, PROFILE
}

/** IT console: a section chip row drives which admin panel is rendered below. */
@Composable
fun ITPortalScreen(
    viewModel: ItViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(ItSection.DASHBOARD) }

    // User-management sheet targets (shared with USERS section)
    var showCreate by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<UserDto?>(null) }
    var resetUser by remember { mutableStateOf<UserDto?>(null) }
    var deleteUser by remember { mutableStateOf<UserDto?>(null) }
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

        // Section selector
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ItSection.entries.forEach { item ->
                val active = section == item
                Surface(
                    onClick = { section = item },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) OrbitPrimary else colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) OrbitPrimary else colors.borderStrong
                    )
                ) {
                    Text(
                        text = itSectionLabel(item),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when {
            state.loading -> LoadingHint()
            else -> when (section) {
                ItSection.DASHBOARD -> ItDashboardSection(state)
                ItSection.USERS -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrbitButton(
                        text = stringResource(R.string.it_create_user),
                        onClick = { showCreate = true },
                        fullWidth = true
                    )
                    state.filteredUsers.forEach { user ->
                        UserRow(
                            user = user,
                            onEdit = { editUser = user },
                            onResetPassword = { resetUser = user },
                            onForceLogout = { viewModel.forceLogout(user.id) },
                            onDelete = { deleteUser = user }
                        )
                    }
                }
                ItSection.ROLES -> RoleMatrixCard()
                ItSection.SECURITY -> ItSecuritySection(viewModel, state.busy)
                ItSection.SESSIONS -> SessionList(
                    sessions = state.sessions,
                    title = stringResource(R.string.title_session_logs)
                )
                ItSection.AUDIT -> ItAuditSection(state)
                ItSection.HEALTH -> ItHealthSection(state)
                ItSection.SETTINGS -> ItSettingsSection(viewModel, state)
                ItSection.PROFILE -> SessionList(
                    sessions = state.mySessions,
                    title = stringResource(R.string.it_my_sessions)
                )
            }
        }
    }
    }

    // Shared management sheets
    if (showCreate || editUser != null) {
        UserFormSheet(
            editing = editUser,
            busy = state.busy,
            externalError = sheetError,
            onDismiss = {
                showCreate = false
                editUser = null
                sheetError = null
            },
            onCreate = { body ->
                viewModel.createUser(body) { ok, err ->
                    if (ok) showCreate = false else sheetError = err
                }
            },
            onUpdate = { id, body ->
                viewModel.updateUser(id, body) { ok, err ->
                    if (ok) editUser = null else sheetError = err
                }
            }
        )
    }
    resetUser?.let { user ->
        ResetPasswordSheet(
            user = user,
            busy = state.busy,
            externalError = sheetError,
            onDismiss = {
                resetUser = null
                sheetError = null
            },
            onConfirm = { password ->
                viewModel.resetPassword(user.id, password) { ok, err ->
                    if (ok) resetUser = null else sheetError = err
                }
            }
        )
    }
    deleteUser?.let { user ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.it_delete_user_title),
            message = stringResource(R.string.it_delete_user_message, user.email),
            busy = state.busy,
            onConfirm = { viewModel.deleteUser(user.id) { deleteUser = null } },
            onDismiss = { deleteUser = null }
        )
    }
}

// Maps each section enum to its localized chip label
@Composable
private fun itSectionLabel(section: ItSection): String = when (section) {
    ItSection.DASHBOARD -> stringResource(R.string.nav_dashboard)
    ItSection.USERS -> stringResource(R.string.it_section_users)
    ItSection.ROLES -> stringResource(R.string.it_section_roles)
    ItSection.SECURITY -> stringResource(R.string.it_section_security)
    ItSection.SESSIONS -> stringResource(R.string.title_session_logs)
    ItSection.AUDIT -> stringResource(R.string.it_section_audit)
    ItSection.HEALTH -> stringResource(R.string.it_section_health)
    ItSection.SETTINGS -> stringResource(R.string.nav_settings)
    ItSection.PROFILE -> stringResource(R.string.profile_settings)
}

/** KPI overview: users, live sessions, error rate and storage. */
@Composable
private fun ItDashboardSection(state: ItState) {
    val spark = listOf(1, 2, 2, 3, 3, 4)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                label = stringResource(R.string.stats_total_members),
                value = (state.health?.totalUsers ?: state.users.size).toString(),
                color = OrbitPrimary,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.it_active_sessions),
                value = (state.health?.activeSessions ?: 0).toString(),
                color = OrbitSuccess,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                label = stringResource(R.string.it_error_rate),
                value = state.health?.errorRate ?: "—",
                color = OrbitWarning,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.it_storage),
                value = "${state.health?.storageUsageMb ?: 0} MB",
                color = OrbitPurple,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Security actions: force-logout-all + per-user suspension hint. */
@Composable
private fun ItSecuritySection(viewModel: ItViewModel, busy: Boolean) {
    val colors = OrbitTheme.colors
    var confirmAll by remember { mutableStateOf(false) }
    DashCard {
        DashCardHeader(
            title = stringResource(R.string.it_section_security),
            subtitle = stringResource(R.string.it_security_sub)
        )
        Text(
            text = stringResource(R.string.it_security_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))
        OrbitButton(
            text = stringResource(R.string.it_logout_all),
            onClick = { confirmAll = true },
            variant = OrbitButtonVariant.Danger,
            loading = busy,
            fullWidth = true
        )
    }
    if (confirmAll) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.it_logout_all),
            message = stringResource(R.string.it_logout_all_message),
            busy = busy,
            onConfirm = {
                viewModel.logoutAll()
                confirmAll = false
            },
            onDismiss = { confirmAll = false }
        )
    }
}

/** Audit feed reused from system/audit-logs. */
@Composable
private fun ItAuditSection(state: ItState) {
    val colors = OrbitTheme.colors
    DashCard {
        DashCardHeader(
            title = stringResource(R.string.it_section_audit),
            subtitle = stringResource(R.string.dash_recent_events, state.auditLogs.size)
        )
        state.auditLogs.take(30).forEach { log ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HashAvatar(name = log.userName ?: "System", size = 24.dp)
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = log.userName ?: "System",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = log.actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = timeAgo(log.time).asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

/** Health card: backend status pill + raw stats. */
@Composable
private fun ItHealthSection(state: ItState) {
    val colors = OrbitTheme.colors
    DashCard {
        DashCardHeader(title = stringResource(R.string.it_section_health))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TinyPill(
                state.health?.status ?: "unknown",
                if (state.health?.status == "connected") OrbitSuccess else OrbitDanger
            )
            Spacer(Modifier.width(8.dp))
            TinyPill(
                "${stringResource(R.string.it_storage)} ${state.health?.storageUsageMb ?: 0}MB",
                OrbitTeal
            )
        }
    }
}

/** Settings: maintenance switch + max upload size editor. */
@Composable
private fun ItSettingsSection(viewModel: ItViewModel, state: ItState) {
    val colors = OrbitTheme.colors
    var maxUpload by remember { mutableStateOf("") }

    // Seed the field once health data arrives
    LaunchedEffect(state.health?.maxUploadSizeMb) {
        maxUpload = (state.health?.maxUploadSizeMb ?: 5).toString()
    }

    DashCard {
        DashCardHeader(title = stringResource(R.string.it_settings_title))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.it_maintenance_mode),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.it_maintenance_sub),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
            Switch(
                checked = state.health?.maintenanceMode == true,
                onCheckedChange = { viewModel.updateSettings(maintenance = it) },
                colors = SwitchDefaults.colors(checkedTrackColor = OrbitDanger)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.it_max_upload).uppercase(),
            style = OrbitTextStyles.techLabel,
            color = colors.textMuted
        )
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbitTextField(
                value = maxUpload,
                onValueChange = { maxUpload = it },
                modifier = Modifier.weight(1f)
            )
            OrbitButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    maxUpload.toIntOrNull()?.let { viewModel.updateSettings(maxUpload = it) }
                },
                enabled = maxUpload.toIntOrNull() != null && !state.busy
            )
        }
    }
}

/** Reusable session log list (all sessions or just mine). */
@Composable
fun SessionList(sessions: List<SessionLogDto>, title: String) {
    val colors = OrbitTheme.colors
    DashCard {
        DashCardHeader(
            title = title,
            subtitle = stringResource(R.string.it_sessions_sub, sessions.size)
        )
        if (sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.tp_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        sessions.forEach { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HashAvatar(
                    name = session.name ?: session.email ?: "U",
                    size = 30.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name ?: session.email ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${session.userAgent ?: "—"} · ${session.ipAddress ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeAgo(session.loginTime).asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    TinyPill(
                        stringResource(
                            R.string.it_duration,
                            session.durationMinutes.toInt()
                        ),
                        OrbitPrimary
                    )
                }
            }
        }
    }
}

/** Plain-text credentials registry with a download action. */
@Composable
fun CredentialsCard(text: String?, onDownload: () -> Unit) {
    val colors = OrbitTheme.colors
    DashCard {
        DashCardHeader(
            title = stringResource(R.string.nav_credentials),
            subtitle = stringResource(R.string.it_credentials_sub)
        )
        Text(
            text = text ?: stringResource(R.string.tp_no_data),
            style = OrbitTextStyles.techLabel,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))
        OrbitButton(
            text = stringResource(R.string.it_download_file),
            onClick = onDownload,
            variant = OrbitButtonVariant.Secondary,
            fullWidth = true
        )
    }
}
