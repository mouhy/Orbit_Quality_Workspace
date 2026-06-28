package com.orbit.mobile.feature.founder

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTeal
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.FounderAccountCreateRequest
import com.orbit.mobile.data.dto.FounderAccountDto
import com.orbit.mobile.data.dto.FounderAccountPatchRequest
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.projects.ConfirmDeleteDialog

// Must match backend ALLOWED_QUALITY_SYSTEMS
private val QUALITY_SYSTEMS = listOf(
    "iso-9001", "iso-27001", "iso-45001", "iso-14001", "naqaae"
)

private val QUALITY_SYSTEM_LABELS = mapOf(
    "iso-9001"  to "ISO 9001",
    "iso-27001" to "ISO 27001",
    "iso-45001" to "ISO 45001",
    "iso-14001" to "ISO 14001",
    "naqaae"    to "NAQAAE"
)

/** Founder's IT staff management: create, rename, reset password, toggle status, delete. */
@Composable
fun FounderAccountsScreen(
    viewModel: FounderViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Active sheet/dialog targets
    var showCreate by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<FounderAccountDto?>(null) }
    var resetAccount by remember { mutableStateOf<FounderAccountDto?>(null) }
    var deleteAccount by remember { mutableStateOf<FounderAccountDto?>(null) }
    var sheetError by remember { mutableStateOf<UiText?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        // Search + create row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbitTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
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
                onClick = { showCreate = true },
                leadingIcon = Icons.Outlined.Add
            )
        }

        when {
            state.loading -> LoadingHint()
            else -> state.filteredAccounts.forEach { account ->
                val active = account.isActive != false && account.status != "suspended"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HashAvatar(name = account.name.ifBlank { "U" }, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = account.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                TinyPill("it_staff", OrbitTeal)
                                TinyPill(
                                    if (active) stringResource(R.string.it_status_active)
                                    else stringResource(R.string.it_status_suspended),
                                    if (active) OrbitSuccess else OrbitDanger
                                )
                            }
                        }
                        // Edit / reset / toggle-status / delete actions
                        IconButton(
                            onClick = { editAccount = account },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { resetAccount = account },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Key,
                                contentDescription = stringResource(R.string.it_reset_password),
                                tint = OrbitWarning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleStatus(account.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.fd_toggle_status),
                                tint = OrbitPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { deleteAccount = account },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = OrbitDanger,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Create/edit sheet — create requires a password, edit only name/email
    if (showCreate || editAccount != null) {
        FounderAccountFormSheet(
            editing = editAccount,
            busy = state.busy,
            externalError = sheetError,
            onDismiss = {
                showCreate = false
                editAccount = null
                sheetError = null
            },
            onCreate = { body ->
                viewModel.createAccount(body) { ok, err ->
                    if (ok) showCreate = false else sheetError = err
                }
            },
            onUpdate = { id, body ->
                viewModel.patchAccount(id, body) { ok, err ->
                    if (ok) editAccount = null else sheetError = err
                }
            }
        )
    }

    resetAccount?.let { account ->
        FounderResetSheet(
            account = account,
            busy = state.busy,
            externalError = sheetError,
            onDismiss = {
                resetAccount = null
                sheetError = null
            },
            onConfirm = { password ->
                viewModel.resetAccountPassword(account.id, password) { ok, err ->
                    if (ok) resetAccount = null else sheetError = err
                }
            }
        )
    }

    deleteAccount?.let { account ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.it_delete_user_title),
            message = stringResource(R.string.it_delete_user_message, account.email),
            busy = state.busy,
            onConfirm = { viewModel.deleteAccount(account.id) { deleteAccount = null } },
            onDismiss = { deleteAccount = null }
        )
    }
}

/** Account form: name + email always, password only in create mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FounderAccountFormSheet(
    editing: FounderAccountDto?,
    busy: Boolean,
    externalError: UiText?,
    onDismiss: () -> Unit,
    onCreate: (FounderAccountCreateRequest) -> Unit,
    onUpdate: (String, FounderAccountPatchRequest) -> Unit
) {
    val colors = OrbitTheme.colors
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var email by remember { mutableStateOf(editing?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var qualitySystem by remember { mutableStateOf(QUALITY_SYSTEMS.first()) }
    var qsExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editing == null) stringResource(R.string.fd_new_it_account)
                else stringResource(R.string.it_edit_user),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            OrbitTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.setup_full_name),
                placeholder = stringResource(R.string.setup_name_placeholder)
            )
            OrbitTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder)
            )
            if (editing == null) {
                OrbitTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.login_password_label),
                    placeholder = stringResource(R.string.setup_password_placeholder)
                )
                // Quality system picker — matches backend ALLOWED_QUALITY_SYSTEMS
                Text(
                    text = stringResource(R.string.fd_quality_system),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = qsExpanded,
                    onExpandedChange = { qsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = QUALITY_SYSTEM_LABELS[qualitySystem] ?: qualitySystem,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = qsExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrbitPrimary,
                            unfocusedBorderColor = colors.borderStrong,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedContainerColor = if (colors.isDark) colors.surface2 else colors.surface,
                            unfocusedContainerColor = if (colors.isDark) colors.surface2 else colors.surface
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = qsExpanded,
                        onDismissRequest = { qsExpanded = false }
                    ) {
                        QUALITY_SYSTEMS.forEach { qs ->
                            DropdownMenuItem(
                                text = { Text(QUALITY_SYSTEM_LABELS[qs] ?: qs) },
                                onClick = {
                                    qualitySystem = qs
                                    qsExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            externalError?.let { AuthErrorBox(message = it.asString()) }
            OrbitButton(
                text = if (editing == null) stringResource(R.string.action_create)
                else stringResource(R.string.action_save),
                onClick = {
                    if (editing == null) {
                        onCreate(
                            FounderAccountCreateRequest(
                                fullName = name.trim(),
                                email = email.trim(),
                                password = password,
                                qualitySystem = qualitySystem
                            )
                        )
                    } else {
                        onUpdate(
                            editing.id,
                            FounderAccountPatchRequest(
                                fullName = name.trim(),
                                email = email.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank() &&
                    (editing != null || password.length >= 8) && !busy,
                loading = busy,
                fullWidth = true
            )
        }
    }
}

/** Password reset sheet for one IT account. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FounderResetSheet(
    account: FounderAccountDto,
    busy: Boolean,
    externalError: UiText?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    var password by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.it_reset_password),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            OrbitTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.it_new_password),
                placeholder = stringResource(R.string.setup_password_placeholder)
            )
            externalError?.let { AuthErrorBox(message = it.asString()) }
            OrbitButton(
                text = stringResource(R.string.action_save),
                onClick = { onConfirm(password) },
                enabled = password.length >= 8 && !busy,
                loading = busy,
                fullWidth = true
            )
        }
    }
}
