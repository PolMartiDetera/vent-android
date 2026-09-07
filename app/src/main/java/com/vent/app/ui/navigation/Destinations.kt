package com.vent.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    NOW("now", "Now", Icons.Filled.Home),
    MARINE("marine?focus={focus}", "Marine", Icons.Filled.Explore) {
        override fun routeFor(vararg args: String): String = "marine?focus=${args.getOrNull(0) ?: ""}"
    },
    MAP("map", "Map", Icons.Filled.Map),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
    ;

    open fun routeFor(vararg args: String): String = route
}
