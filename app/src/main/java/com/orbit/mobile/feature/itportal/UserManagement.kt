package com.orbit.mobile.feature.itportal

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.orbit.mobile.core.theme.roleColor
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.RegisterUserRequest
import com.orbit.mobile.data.dto.UpdateUserRequest
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.TinyPill

// Roles assignable when IT/Admin creates or edits an account
val ASSIGNABLE_ROLES = listOf(
    "admin", "manager", "sub_admin", "staff", "quality_control", "it_staff"
)

/** One user row with avatar, status pill and the four admin actions. */
@Composable
fun UserRow(
    user: UserDto,
    onEdit: () -> Unit,
    onResetPassword: () -> Unit,
    onForceLogout: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = OrbitTheme.colors
    val active = user.isActive != false && user.status != "suspended"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HashAvatar(name = user.name.ifBlank { "U" }, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
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
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TinyPill(user.role.replace("_", " "), roleColor(user.role))
                    TinyPill(
                        if (active) stringResource(R.string.it_status_active)
                        else stringResource(R.string.it_status_suspended),
                        if (active) OrbitSuccess else OrbitDanger
                    )
                }
            }
            // Action icons: edit / reset / force-logout / delete
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(onClick = onResetPassword, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Key,
                    contentDescription = stringResource(R.string.it_reset_password),
                    tint = OrbitWarning,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(onClick = onForceLogout, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = stringResource(R.string.it_force_logout),
                    tint = OrbitPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
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

/** Create/edit form: create mode shows password, edit mode shows status toggle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormSheet(
    editing: UserDto?,
    busy: Boolean,
    externalError: UiText?,
    onDismiss: () -> Unit,
    onCreate: (RegisterUserRequest) -> Unit,
    onUpdate: (String, UpdateUserRequest) -> Unit
) {
    val colors = OrbitTheme.colors
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var email by remember { mutableStateOf(editing?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(editing?.role ?: "staff") }
    var active by remember {
        mutableStateOf(editing?.isActive != false && editing?.status != "suspended")
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
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editing == null) stringResource(R.string.it_create_user)
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
            // Password only needed when creating a fresh account
            if (editing == null) {
                OrbitTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.login_password_label),
                    placeholder = stringResource(R.string.setup_password_placeholder)
                )
            }

            // Role dropdown
            Text(
                text = stringResource(R.string.it_role_label).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textMuted
            )
            var roleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it }
            ) {
                // Role anchor
                OutlinedTextField(
                    value = role.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                // Role options
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    ASSIGNABLE_ROLES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replace("_", " ")) },
                            onClick = {
                                role = option
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            // Status toggle is an edit-only concern
            if (editing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.it_status_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = { active = !active },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) OrbitSuccess.copy(alpha = 0.12f)
                        else OrbitDanger.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (active) stringResource(R.string.it_status_active)
                            else stringResource(R.string.it_status_suspended),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) OrbitSuccess else OrbitDanger,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
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
                                RegisterUserRequest(
                                    fullName = name.trim(),
                                    email = email.trim(),
                                    password = password,
                                    role = role
                                )
                            )
                        } else {
                            onUpdate(
                                editing.id,
                                UpdateUserRequest(
                                    name = name.trim(),
                                    email = email.trim(),
                                    role = role,
                                    status = if (active) "active" else "suspended",
                                    isActive = active
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank() && email.isNotBlank() &&
                        (editing != null || password.length >= 8) && !busy,
                    loading = busy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Minimal sheet asking for the new password of a target account. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordSheet(
    user: UserDto,
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
                text = user.email,
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

/** Static reference card describing what each platform role can do. */
@Composable
fun RoleMatrixCard() {
    val colors = OrbitTheme.colors
    val roles = listOf(
        "founder" to stringResource(R.string.role_desc_founder),
        "it_staff" to stringResource(R.string.role_desc_it),
        "admin" to stringResource(R.string.role_desc_admin),
        "manager" to stringResource(R.string.role_desc_manager),
        "sub_admin" to stringResource(R.string.role_desc_subadmin),
        "staff" to stringResource(R.string.role_desc_staff),
        "quality_control" to stringResource(R.string.role_desc_qc)
    )
    DashCard {
        DashCardHeader(
            title = stringResource(R.string.it_role_matrix),
            subtitle = stringResource(R.string.it_role_matrix_sub)
        )
        roles.forEach { (role, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyPill(role.replace("_", " "), roleColor(role))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
