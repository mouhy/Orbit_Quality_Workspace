package com.orbit.mobile.feature.shell

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.orbit.mobile.R

// Inner routes
object InnerRoutes {
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val TASKS = "tasks"
    const val CREATE_TASK = "create-task"
    const val CALENDAR = "calendar"
    const val TEAM = "team"
    const val TEAMS = "teams"
    const val PORTFOLIO = "portfolio"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val TASKMASTER = "taskmaster"
    const val SUBADMIN = "subadmin"
    const val TASKFLOW = "taskflow"
    const val WORKSPACE = "workspace"
    const val CONFIGURATION = "configuration"
    const val SESSION_LOGS = "session-logs"
    const val IT_PORTAL = "it-portal"
    const val MY_PROJECTS = "my-projects"
    const val STAFF_TASK = "staff-task"
    const val QUALITY_CONTROL = "quality-control"
    const val QUALITY_INSIGHTS = "quality-insights"
    const val FOUNDER = "founder"
    const val FOUNDER_ACCOUNTS = "founder-accounts"
    const val HELP = "help"

    // Public channel
    const val TASKFLOW_PUBLIC = "$TASKFLOW?projectId=public-group"
}

// Menu item
data class OrbitNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

// Role menu
fun menuForRole(role: String?): List<OrbitNavItem> = when (role) {
    "admin" -> listOf(
        OrbitNavItem(InnerRoutes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Home),
        OrbitNavItem(InnerRoutes.PROJECTS, R.string.nav_workspace, Icons.Outlined.Folder),
        OrbitNavItem(InnerRoutes.CALENDAR, R.string.nav_calendar, Icons.Outlined.CalendarMonth),
        OrbitNavItem(InnerRoutes.TEAM, R.string.nav_team, Icons.Outlined.Group),
        OrbitNavItem(InnerRoutes.REPORTS, R.string.nav_reports, Icons.Outlined.BarChart),
        OrbitNavItem(InnerRoutes.QUALITY_CONTROL, R.string.nav_quality_control, Icons.Outlined.CheckCircle),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
        OrbitNavItem(InnerRoutes.CONFIGURATION, R.string.nav_user_management, Icons.Outlined.ManageAccounts)
    )
    "quality_control", "quality_manager" -> listOf(
        OrbitNavItem(InnerRoutes.QUALITY_INSIGHTS, R.string.nav_quality_insights, Icons.Outlined.Insights),
        OrbitNavItem(InnerRoutes.QUALITY_CONTROL, R.string.nav_reports, Icons.Outlined.CheckCircle),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    "sub_admin" -> listOf(
        OrbitNavItem(InnerRoutes.SUBADMIN, R.string.nav_dashboard, Icons.Outlined.SpaceDashboard),
        OrbitNavItem(InnerRoutes.PROJECTS, R.string.nav_workspace, Icons.Outlined.Folder),
        OrbitNavItem(InnerRoutes.CALENDAR, R.string.nav_calendar, Icons.Outlined.CalendarMonth),
        OrbitNavItem(InnerRoutes.TEAM, R.string.nav_team, Icons.Outlined.Group),
        OrbitNavItem(InnerRoutes.REPORTS, R.string.nav_reports, Icons.Outlined.BarChart),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    "staff" -> listOf(
        OrbitNavItem(InnerRoutes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Home),
        OrbitNavItem(InnerRoutes.MY_PROJECTS, R.string.nav_my_projects, Icons.Outlined.Folder),
        OrbitNavItem(InnerRoutes.TASKFLOW_PUBLIC, R.string.nav_public_channel, Icons.Outlined.Campaign),
        OrbitNavItem(InnerRoutes.CALENDAR, R.string.nav_calendar, Icons.Outlined.CalendarMonth),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    "founder" -> listOf(
        OrbitNavItem(InnerRoutes.FOUNDER, R.string.nav_command_center, Icons.Outlined.Home),
        OrbitNavItem(InnerRoutes.FOUNDER_ACCOUNTS, R.string.nav_it_accounts, Icons.Outlined.ManageAccounts),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    "it_staff" -> listOf(
        OrbitNavItem(InnerRoutes.IT_PORTAL, R.string.nav_it_console, Icons.Outlined.Terminal),
        OrbitNavItem(InnerRoutes.CONFIGURATION, R.string.nav_user_management, Icons.Outlined.ManageAccounts),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    "manager" -> listOf(
        OrbitNavItem(InnerRoutes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Home),
        OrbitNavItem(InnerRoutes.PROJECTS, R.string.nav_projects, Icons.Outlined.Folder),
        OrbitNavItem(InnerRoutes.TEAM, R.string.nav_my_team, Icons.Outlined.Group),
        OrbitNavItem(InnerRoutes.REPORTS, R.string.nav_reports, Icons.Outlined.BarChart),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    else -> listOf(
        OrbitNavItem(InnerRoutes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Home),
        OrbitNavItem(InnerRoutes.PROJECTS, R.string.nav_projects, Icons.Outlined.Folder),
        OrbitNavItem(InnerRoutes.TASKS, R.string.nav_tasks, Icons.Outlined.TaskAlt),
        OrbitNavItem(InnerRoutes.CALENDAR, R.string.nav_calendar, Icons.Outlined.CalendarMonth),
        OrbitNavItem(InnerRoutes.TEAM, R.string.nav_team, Icons.Outlined.Group),
        OrbitNavItem(InnerRoutes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
}

// Screen title
@StringRes
fun routeTitleRes(route: String?, role: String?): Int = when (route?.substringBefore("?")) {
    InnerRoutes.DASHBOARD -> R.string.nav_dashboard
    InnerRoutes.PROJECTS -> if (role == "admin" || role == "sub_admin") R.string.nav_workspace else R.string.nav_projects
    InnerRoutes.TASKS -> R.string.nav_tasks
    InnerRoutes.CREATE_TASK -> R.string.title_create_task
    InnerRoutes.CALENDAR -> R.string.nav_calendar
    InnerRoutes.TEAM -> R.string.nav_team
    InnerRoutes.TEAMS -> R.string.title_teams
    InnerRoutes.PORTFOLIO -> R.string.title_portfolio
    InnerRoutes.REPORTS -> R.string.nav_reports
    InnerRoutes.SETTINGS -> R.string.nav_settings
    InnerRoutes.TASKMASTER -> R.string.title_taskmaster
    InnerRoutes.SUBADMIN -> R.string.nav_subadmin_portal
    InnerRoutes.TASKFLOW -> R.string.title_channel
    InnerRoutes.WORKSPACE -> R.string.title_board
    InnerRoutes.CONFIGURATION -> R.string.nav_user_management
    InnerRoutes.SESSION_LOGS -> R.string.title_session_logs
    InnerRoutes.IT_PORTAL -> R.string.nav_it_console
    InnerRoutes.MY_PROJECTS -> R.string.nav_my_projects
    InnerRoutes.STAFF_TASK -> R.string.title_task_details
    InnerRoutes.QUALITY_CONTROL -> R.string.nav_qc_dashboard
    InnerRoutes.QUALITY_INSIGHTS -> R.string.nav_quality_insights
    InnerRoutes.FOUNDER -> R.string.nav_command_center
    InnerRoutes.FOUNDER_ACCOUNTS -> R.string.nav_it_accounts
    InnerRoutes.HELP -> R.string.title_help
    else -> R.string.app_name
}
