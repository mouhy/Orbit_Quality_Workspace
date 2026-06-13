package com.orbit.mobile.feature.settings

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.ThemeMode
import com.orbit.mobile.core.ui.components.OrbitAvatar
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.ProfileUpdateRequest
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.itportal.SessionList
import com.orbit.mobile.feature.workspace.readUploadPart

// Four web-parity settings tabs
private enum class SettingsTab { PROFILE, SECURITY, NOTIFICATIONS, APPEARANCE }

/** Settings page: Profile / Security / Notifications / Appearance. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SettingsTab.PROFILE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        // Tab selector
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsTab.entries.forEach { item ->
                val active = tab == item
                val label = when (item) {
                    SettingsTab.PROFILE -> stringResource(R.string.st_tab_profile)
                    SettingsTab.SECURITY -> stringResource(R.string.st_tab_security)
                    SettingsTab.NOTIFICATIONS -> stringResource(R.string.cd_notifications)
                    SettingsTab.APPEARANCE -> stringResource(R.string.st_tab_appearance)
                }
                Surface(
                    onClick = { tab = item },
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
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when {
            state.loading -> LoadingHint()
            else -> when (tab) {
                SettingsTab.PROFILE -> ProfileTab(viewModel, state)
                SettingsTab.SECURITY -> SecurityTab(viewModel, state)
                SettingsTab.NOTIFICATIONS -> NotificationsTab(viewModel)
                SettingsTab.APPEARANCE -> AppearanceTab(viewModel)
            }
        }
    }
}

/** Profile tab: avatar management + editable identity fields. */
@Composable
private fun ProfileTab(viewModel: SettingsViewModel, state: SettingsState) {
    val colors = OrbitTheme.colors
    val context = LocalContext.current
    val me = state.me

    // Local field buffers seeded from the loaded profile
    var name by remember(me) { mutableStateOf(me?.name ?: "") }
    var bio by remember(me) { mutableStateOf(me?.bio ?: "") }
    var jobTitle by remember(me) { mutableStateOf(me?.jobTitle ?: "") }
    var department by remember(me) { mutableStateOf(me?.department ?: "") }
    var country by remember(me) { mutableStateOf(me?.country ?: "") }
    var timezone by remember(me) { mutableStateOf(me?.timezone ?: "") }
    var formError by remember { mutableStateOf<UiText?>(null) }
    var saved by remember { mutableStateOf(false) }

    // Avatar picker → base64 data-URI upload
    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { picked ->
            readUploadPart(context, picked)?.let {
                viewModel.uploadAvatar(it.bytes, it.mimeType)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrbitAvatar(
                    name = me?.name ?: "",
                    imageUrl = me?.avatar,
                    size = 64.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = me?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = me?.email ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    TinyPill((me?.role ?: "").replace("_", " "), OrbitPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitButton(
                    text = stringResource(R.string.st_upload_avatar),
                    onClick = { avatarPicker.launch("image/*") },
                    variant = OrbitButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                OrbitButton(
                    text = stringResource(R.string.st_remove_avatar),
                    onClick = viewModel::deleteAvatar,
                    variant = OrbitButtonVariant.Ghost,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        DashCard {
            DashCardHeader(title = stringResource(R.string.st_profile_details))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OrbitTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.field_name)
                )
                OrbitTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = stringResource(R.string.st_bio),
                    singleLine = false,
                    minLines = 2
                )
                OrbitTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = stringResource(R.string.st_job_title)
                )
                OrbitTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = stringResource(R.string.st_department)
                )
                OrbitTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = stringResource(R.string.st_country)
                )
                OrbitTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = stringResource(R.string.st_timezone)
                )
                formError?.let { AuthErrorBox(message = it.asString()) }
                if (saved) {
                    TinyPill(stringResource(R.string.st_saved), OrbitSuccess)
                }
                OrbitButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        saved = false
                        viewModel.saveProfile(
                            ProfileUpdateRequest(
                                name = name.trim(),
                                bio = bio.ifBlank { null },
                                jobTitle = jobTitle.ifBlank { null },
                                department = department.ifBlank { null },
                                country = country.ifBlank { null },
                                timezone = timezone.ifBlank { null }
                            )
                        ) { ok, err ->
                            saved = ok
                            formError = err
                        }
                    },
                    enabled = name.isNotBlank() && !state.busy,
                    loading = state.busy,
                    fullWidth = true
                )
            }
        }
    }
}

/** Security tab: change own password + own login sessions. */
@Composable
private fun SecurityTab(viewModel: SettingsViewModel, state: SettingsState) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<UiText?>(null) }
    var saved by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashCard {
            DashCardHeader(title = stringResource(R.string.st_change_password))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OrbitTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.it_new_password),
                    placeholder = stringResource(R.string.setup_password_placeholder)
                )
                OrbitTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = stringResource(R.string.setup_confirm_label),
                    isError = confirm.isNotEmpty() && confirm != password,
                    errorText = stringResource(R.string.setup_mismatch)
                )
                formError?.let { AuthErrorBox(message = it.asString()) }
                if (saved) {
                    TinyPill(stringResource(R.string.st_saved), OrbitSuccess)
                }
                OrbitButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        saved = false
                        viewModel.changePassword(password) { ok, err ->
                            saved = ok
                            formError = err
                            if (ok) {
                                password = ""
                                confirm = ""
                            }
                        }
                    },
                    enabled = password.length >= 8 && password == confirm && !state.busy,
                    loading = state.busy,
                    fullWidth = true
                )
            }
        }
        SessionList(
            sessions = state.mySessions,
            title = stringResource(R.string.it_my_sessions)
        )
    }
}

/** Notifications tab: three locally-persisted toggles. */
@Composable
private fun NotificationsTab(viewModel: SettingsViewModel) {
    val push by viewModel.notifPush.collectAsStateWithLifecycle()
    val email by viewModel.notifEmail.collectAsStateWithLifecycle()
    val mentions by viewModel.notifMentions.collectAsStateWithLifecycle()

    DashCard {
        DashCardHeader(
            title = stringResource(R.string.cd_notifications),
            subtitle = stringResource(R.string.st_notif_sub)
        )
        NotifToggle(
            label = stringResource(R.string.st_notif_push),
            checked = push
        ) { viewModel.setFlag(PREF_NOTIF_PUSH, it) }
        NotifToggle(
            label = stringResource(R.string.st_notif_email),
            checked = email
        ) { viewModel.setFlag(PREF_NOTIF_EMAIL, it) }
        NotifToggle(
            label = stringResource(R.string.st_notif_mentions),
            checked = mentions
        ) { viewModel.setFlag(PREF_NOTIF_MENTIONS, it) }
    }
}

// Single labeled switch row
@Composable
private fun NotifToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = OrbitPrimary)
        )
    }
}

/** Appearance tab: Light / Dark / System theme selector (default Dark). */
@Composable
private fun AppearanceTab(viewModel: SettingsViewModel) {
    val colors = OrbitTheme.colors
    val current by viewModel.themeMode.collectAsStateWithLifecycle()

    DashCard {
        DashCardHeader(
            title = stringResource(R.string.st_tab_appearance),
            subtitle = stringResource(R.string.st_appearance_sub)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val active = current == mode
                val label = when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }
                Surface(
                    onClick = { viewModel.setTheme(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) OrbitPrimary else colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) OrbitPrimary else colors.borderStrong
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
