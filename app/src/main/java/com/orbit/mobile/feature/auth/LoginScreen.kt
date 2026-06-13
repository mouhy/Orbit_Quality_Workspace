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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitLogo
import com.orbit.mobile.core.ui.components.OrbitLogoVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.asString

// Panel gradient
private val BrandGradient = Brush.linearGradient(
    listOf(Color(0xFF0C1A38), Color(0xFF071028), Color(0xFF04091A))
)

// Login screen
@Composable
fun LoginScreen(
    onLoggedIn: (role: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = OrbitTheme.colors

    // Navigate out
    LaunchedEffect(state.loggedInRole) {
        state.loggedInRole?.let { onLoggedIn(it) }
    }

    val panelBg = if (colors.isDark) colors.authPanelBg else colors.surface
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(panelBg)
            .verticalScroll(rememberScrollState())
    ) {
        BrandPanel()
        FormPanel(state, viewModel)
    }
}

// Brand header
@Composable
private fun BrandPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandGradient)
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 28.dp)
    ) {
        // Logo row
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrbitLogo(
                variant = OrbitLogoVariant.Mark,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = OrbitTextStyles.logo,
                color = Color.White
            )
        }
        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.brand_headline),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.brand_subcopy),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(22.dp))

        // Features
        listOf(
            R.string.brand_feature_1,
            R.string.brand_feature_2,
            R.string.brand_feature_3,
            R.string.brand_feature_4
        ).forEach { res ->
            Row(
                modifier = Modifier.padding(bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(OrbitPrimary.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                        .border(
                            1.dp,
                            OrbitPrimary.copy(alpha = 0.3f),
                            RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = OrbitPrimary
                    )
                }
                Spacer(Modifier.size(11.dp))
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Status row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(OrbitSuccess, CircleShape)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.brand_status_ok),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

// Sign-in form
@Composable
private fun FormPanel(state: LoginUiState, viewModel: LoginViewModel) {
    val colors = OrbitTheme.colors
    val panelBg = if (colors.isDark) colors.authPanelBg else colors.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(panelBg)
            .padding(horizontal = 28.dp, vertical = 36.dp)
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(28.dp))

        OrbitTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.login_email_label),
            placeholder = stringResource(R.string.login_email_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !state.loading
        )
        Spacer(Modifier.height(14.dp))

        OrbitTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.login_password_label),
            placeholder = stringResource(R.string.login_password_placeholder),
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

        state.error?.let {
            Spacer(Modifier.height(14.dp))
            AuthErrorBox(message = it.asString())
        }
        Spacer(Modifier.height(16.dp))

        AuthGradientButton(
            text = stringResource(R.string.login_button),
            loadingText = stringResource(R.string.login_button_loading),
            loading = state.loading,
            enabled = state.canSubmit,
            onClick = viewModel::login
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.login_footer),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
