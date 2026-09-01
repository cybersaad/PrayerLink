package com.prayerlink.app.ui.navigation

/**
 * Type-safe navigation routes for the three main screens.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Settings  : Screen("settings")
    data object About     : Screen("about")
}
