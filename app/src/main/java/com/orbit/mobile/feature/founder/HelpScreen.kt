package com.orbit.mobile.feature.founder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader

/** Static help center mirroring the web HelpPage sections. */
@Composable
fun HelpScreen() {
    val colors = OrbitTheme.colors

    // Section emoji + title + body triples, all localized
    val sections = listOf(
        Triple("🚀", R.string.help_start_title, R.string.help_start_body),
        Triple("📁", R.string.help_projects_title, R.string.help_projects_body),
        Triple("🤖", R.string.help_ai_title, R.string.help_ai_body),
        Triple("💬", R.string.help_channels_title, R.string.help_channels_body),
        Triple("🛟", R.string.help_support_title, R.string.help_support_body)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.title_help),
                subtitle = stringResource(R.string.help_subtitle)
            )
        }
        sections.forEach { (emoji, titleRes, bodyRes) ->
            DashCard {
                Row {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(bodyRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
