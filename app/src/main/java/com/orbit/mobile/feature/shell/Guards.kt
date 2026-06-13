package com.orbit.mobile.feature.shell

// Route guards
object Guards {

    // Start route
    fun startRoute(role: String?): String = when (role) {
        "founder" -> InnerRoutes.FOUNDER
        "sub_admin" -> InnerRoutes.SUBADMIN
        "it_staff" -> InnerRoutes.IT_PORTAL
        "quality_control", "quality_manager" -> InnerRoutes.QUALITY_CONTROL
        else -> InnerRoutes.DASHBOARD
    }

    // Resolve target
    fun resolve(role: String?, route: String): String {
        val base = route.substringBefore("?")

        // Staff blocked
        if (role == "staff" && base in setOf(
                InnerRoutes.PROJECTS,
                InnerRoutes.TASKS,
                InnerRoutes.TEAMS,
                InnerRoutes.PORTFOLIO
            )
        ) return InnerRoutes.DASHBOARD

        // SubAdmin only
        if (base == InnerRoutes.TASKMASTER && role != "sub_admin") {
            return InnerRoutes.DASHBOARD
        }

        // QC circle
        if (base in setOf(InnerRoutes.QUALITY_CONTROL, InnerRoutes.QUALITY_INSIGHTS) &&
            role !in setOf("quality_control", "quality_manager", "admin", "manager", "sub_admin")
        ) return InnerRoutes.DASHBOARD

        // Founder only
        if (base in setOf(
                InnerRoutes.FOUNDER,
                InnerRoutes.FOUNDER_ACCOUNTS
            ) && role != "founder"
        ) {
            return InnerRoutes.DASHBOARD
        }

        // Dashboard redirects
        if (base == InnerRoutes.DASHBOARD) {
            return when (role) {
                "sub_admin" -> InnerRoutes.SUBADMIN
                "quality_control", "quality_manager" -> InnerRoutes.QUALITY_CONTROL
                "it_staff" -> InnerRoutes.IT_PORTAL
                else -> route
            }
        }
        return route
    }
}
