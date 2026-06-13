package com.orbit.mobile.feature.itportal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.projects.ConfirmDeleteDialog

/**
 * Configuration page = full user management:
 * search, create, edit, password reset, force-logout and delete,
 * plus the static role matrix reference.
 */
@Composable
fun ConfigurationScreen(
    viewModel: ItViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Sheet/dialog targets
    var showCreate by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<UserDto?>(null) }
    var resetUser by remember { mutableStateOf<UserDto?>(null) }
    var deleteUser by remember { mutableStateOf<UserDto?>(null) }
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
            else -> state.filteredUsers.forEach { user ->
                UserRow(
                    user = user,
                    onEdit = { editUser = user },
                    onResetPassword = { resetUser = user },
                    onForceLogout = { viewModel.forceLogout(user.id) },
                    onDelete = { deleteUser = user }
                )
            }
        }

        RoleMatrixCard()
    }

    // Create / edit sheet shares one form
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
