package com.orbit.mobile.feature.auth

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.asString

// Setup screen
@Composable
fun SetupScreen(
    onGoLogin: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = OrbitTheme.colors

    // Navigate out
    LaunchedEffect(state.goLogin) {
        if (state.goLogin) onGoLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.done) {
            SetupDoneCard()
        } else {
            SetupFormCard(state, viewModel, onGoLogin)
        }
    }
}

// Success card
@Composable
private fun SetupDoneCard() {
    val colors = OrbitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(16.dp))
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(OrbitSuccess.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, OrbitSuccess.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = OrbitSuccess
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.setup_done_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_done_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// Form card
@Composable
private fun SetupFormCard(
    state: SetupUiState,
    viewModel: SetupViewModel,
    onGoLogin: () -> Unit
) {
    val colors = OrbitTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colors.borderStrong, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(OrbitPrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            OrbitPrimary.copy(alpha = 0.16f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = OrbitPrimary
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(24.dp))

            // Full name
            OrbitTextField(
                value = state.fullName,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.setup_full_name),
                placeholder = stringResource(R.string.setup_name_placeholder),
                enabled = !state.loading
            )
            Spacer(Modifier.height(16.dp))

            // Email
            OrbitTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.setup_email_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !state.loading
            )
            Spacer(Modifier.height(16.dp))

            // Password
            OrbitTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.login_password_label),
                placeholder = stringResource(R.string.setup_password_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                enabled = !state.loading,
                trailing = {
                    PasswordEyeButton(
                        visible = state.showPassword,
                        onToggle = viewModel::togglePassword,
                        contentDescriptionShow = stringResource(R.string.cd_show_password),
                        contentDescriptionHide = stringResource(R.string.cd_hide_password)
                    )
                }
            )

            // Strength meter
            if (state.password.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                StrengthMeter(state)
            }
            Spacer(Modifier.height(16.dp))

            // Confirm
            OrbitTextField(
                value = state.confirm,
                onValueChange = viewModel::onConfirmChange,
                label = stringResource(R.string.setup_confirm_label),
                placeholder = stringResource(R.string.setup_confirm_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.showConfirm) VisualTransformation.None
                else PasswordVisualTransformation(),
                enabled = !state.loading,
                isError = state.confirmMismatch,
                errorText = stringResource(R.string.setup_mismatch),
                trailing = {
                    PasswordEyeButton(
                        visible = state.showConfirm,
                        onToggle = viewModel::toggleConfirm,
                        contentDescriptionShow = stringResource(R.string.cd_show_password),
                        contentDescriptionHide = stringResource(R.string.cd_hide_password)
                    )
                }
            )
            Spacer(Modifier.height(16.dp))

            // Privilege notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrbitPurple.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, OrbitPurple.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 10.dp, top = 2.dp)
                        .size(14.dp),
                    tint = OrbitPurple
                )
                Text(
                    text = stringResource(R.string.setup_privilege_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = OrbitPurple
                )
            }

            state.error?.let {
                Spacer(Modifier.height(14.dp))
                AuthErrorBox(message = it.asString())
            }
            Spacer(Modifier.height(18.dp))

            AuthGradientButton(
                text = stringResource(R.string.setup_submit),
                loadingText = stringResource(R.string.setup_submitting),
                loading = state.loading,
                enabled = state.canSubmit,
                onClick = viewModel::submit
            )
        }

        // Footer link
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.setup_have_account),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.size(4.dp))
            Surface(onClick = onGoLogin, color = Color.Transparent) {
                Text(
                    text = stringResource(R.string.action_login),
                    style = MaterialTheme.typography.bodySmall,
                    color = OrbitPrimary,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

// Strength bars
@Composable
private fun StrengthMeter(state: SetupUiState) {
    val colors = OrbitTheme.colors
    val strength = state.strength
    val tone = when (strength.tone) {
        StrengthTone.DANGER -> OrbitDanger
        StrengthTone.WARNING -> OrbitWarning
        StrengthTone.SUCCESS -> OrbitSuccess
        StrengthTone.NONE -> Color.Transparent
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..4).forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (i <= strength.score) tone else colors.border,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
        strength.labelRes?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.labelSmall,
                color = tone
            )
        }
    }
}
