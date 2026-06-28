package com.orbit.mobile.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitTheme

// Logged shell
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    onLoggedOut: () -> Unit,
    shellViewModel: ShellViewModel = hiltViewModel(),
    notifViewModel: NotificationsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val session by shellViewModel.session.collectAsStateWithLifecycle()
    val loggedOut by shellViewModel.loggedOut.collectAsStateWithLifecycle()
    val notifications by notifViewModel.items.collectAsStateWithLifecycle()

    val role = session.role
    val menu = remember(role) { menuForRole(role) }
    val primaryItems = if (menu.size <= 5) menu else menu.take(4)
    val overflowItems = if (menu.size <= 5) emptyList() else menu.drop(4)
    val tabRoutes = remember(menu) { menu.map { it.route.substringBefore("?") }.toSet() }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentBase = currentRoute?.substringBefore("?")

    var showNotifications by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    // Navigate out
    LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }

    // Guarded navigate
    fun go(route: String) {
        val target = Guards.resolve(role, route)
        // Tab check
        val isTab = target.substringBefore("?") in tabRoutes
        navController.navigate(target) {
            if (isTab) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                restoreState = true
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = colors.appBackground,
        topBar = {
            OrbitTopBar(
                title = stringResource(routeTitleRes(currentRoute, role)),
                userName = session.fullName ?: "",
                avatarUrl = session.avatar,
                unreadCount = notifications.count { !it.isRead },
                onBellClick = { showNotifications = true },
                onAvatarClick = { showProfile = true }
            )
        },
        bottomBar = {
            OrbitBottomBar(
                items = primaryItems,
                hasOverflow = overflowItems.isNotEmpty(),
                currentRoute = currentRoute,
                overflowSelected = overflowItems.any {
                    it.route.substringBefore("?") == currentBase
                },
                onItemClick = { go(it.route) },
                onMoreClick = { showMore = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.appBackground)
        ) {
            ShellNavGraph(
                navController = navController,
                role = role,
                onNavigate = { go(it) }
            )
            // Floating bot
            if (currentBase != InnerRoutes.WORKSPACE) {
                com.orbit.mobile.feature.chatbot.ChatbotWidget(
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel()
                )
            }
        }
    }

    // Notifications sheet
    if (showNotifications) {
        NotificationsSheet(
            items = notifications,
            onDismissRequest = { showNotifications = false },
            onMarkAllRead = notifViewModel::markAllRead,
            onDismissItem = notifViewModel::dismiss,
            onItemClick = notifViewModel::markRead,
            onNavigate = { route ->
                // Close + route
                showNotifications = false
                go(route)
            }
        )
    }

    // Profile sheet
    if (showProfile) {
        ProfileSheet(
            session = session,
            onDismissRequest = { showProfile = false },
            onProfileSettings = {
                showProfile = false
                go(InnerRoutes.SETTINGS)
            },
            onLogout = {
                showProfile = false
                shellViewModel.logout()
            }
        )
    }

    // More sheet
    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                overflowItems.forEach { item ->
                    val selected = item.route.substringBefore("?") == currentBase
                    Surface(
                        onClick = {
                            showMore = false
                            go(item.route)
                        },
                        color = if (selected) OrbitPrimary.copy(alpha = 0.1f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selected) OrbitPrimary else colors.textSecondary
                            )
                            Spacer(Modifier.size(12.dp))
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) OrbitPrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// Inner graph
@Composable
private fun ShellNavGraph(
    navController: androidx.navigation.NavHostController,
    role: String?,
    onNavigate: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Guards.startRoute(role)
    ) {
        composable(InnerRoutes.DASHBOARD) {
            com.orbit.mobile.feature.dashboard.DashboardScreen(
                role = role,
                onNavigate = onNavigate
            )
        }

        composable(InnerRoutes.PROJECTS) {
            com.orbit.mobile.feature.projects.ProjectsScreen(onNavigate = onNavigate)
        }

        composable(InnerRoutes.MY_PROJECTS) {
            com.orbit.mobile.feature.projects.StaffProjectsScreen(onNavigate = onNavigate)
        }

        composable(InnerRoutes.TEAMS) {
            com.orbit.mobile.feature.teams.TeamsScreen(onNavigate = onNavigate)
        }

        composable("${InnerRoutes.TEAMS}/{teamId}") {
            com.orbit.mobile.feature.teams.TeamDetailsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = onNavigate
            )
        }

        composable(InnerRoutes.TEAM) {
            com.orbit.mobile.feature.teams.TeamPageScreen()
        }

        composable(InnerRoutes.TASKS) {
            com.orbit.mobile.feature.tasks.TasksScreen()
        }

        composable(InnerRoutes.CREATE_TASK) {
            com.orbit.mobile.feature.tasks.CreateTaskScreen(
                onDone = { navController.popBackStack() }
            )
        }

        composable("${InnerRoutes.PORTFOLIO}/{userId}") {
            com.orbit.mobile.feature.reports.PortfolioScreen()
        }

        composable(InnerRoutes.REPORTS) {
            com.orbit.mobile.feature.reports.ReportsScreen()
        }

        composable(InnerRoutes.SUBADMIN) {
            com.orbit.mobile.feature.reports.SubAdminPortalScreen()
        }

        composable(InnerRoutes.TASKMASTER) {
            com.orbit.mobile.feature.reports.TaskMasterScreen()
        }

        composable(
            route = "${InnerRoutes.STAFF_TASK}?taskId={taskId}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            com.orbit.mobile.feature.aireview.StaffTaskDetailScreen()
        }

        composable(
            route = "${InnerRoutes.WORKSPACE}?projectId={projectId}",
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            com.orbit.mobile.feature.workspace.WorkspaceScreen()
        }

        composable(InnerRoutes.CALENDAR) {
            com.orbit.mobile.feature.calendar.CalendarScreen()
        }

        composable(InnerRoutes.QUALITY_CONTROL) {
            com.orbit.mobile.feature.qc.QCDashboardScreen()
        }

        composable(InnerRoutes.QUALITY_INSIGHTS) {
            com.orbit.mobile.feature.qc.QualityInsightsScreen()
        }

        composable(InnerRoutes.CONFIGURATION) {
            com.orbit.mobile.feature.itportal.ConfigurationScreen()
        }

        composable(InnerRoutes.IT_PORTAL) {
            com.orbit.mobile.feature.itportal.ITPortalScreen()
        }

        composable(InnerRoutes.SESSION_LOGS) {
            com.orbit.mobile.feature.itportal.SessionLogsScreen()
        }

        composable(InnerRoutes.FOUNDER) {
            com.orbit.mobile.feature.founder.FounderDashboardScreen(onNavigate = onNavigate)
        }

        composable(InnerRoutes.FOUNDER_ACCOUNTS) {
            com.orbit.mobile.feature.founder.FounderAccountsScreen()
        }

        composable(InnerRoutes.HELP) {
            com.orbit.mobile.feature.founder.HelpScreen()
        }

        composable(InnerRoutes.SETTINGS) {
            com.orbit.mobile.feature.settings.SettingsScreen()
        }

        val simpleRoutes = listOf(
            InnerRoutes.PORTFOLIO
        )
        simpleRoutes.forEach { route ->
            composable(route) {
                ComingSoonScreen(title = stringResource(routeTitleRes(route, role)))
            }
        }

        // Channel route
        composable(
            route = "${InnerRoutes.TASKFLOW}?projectId={projectId}",
            arguments = listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "public-group"
                }
            )
        ) {
            com.orbit.mobile.feature.channels.TaskFlowScreen()
        }
    }
}
