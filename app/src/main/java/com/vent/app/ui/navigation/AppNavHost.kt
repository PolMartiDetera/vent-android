package com.vent.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vent.app.ui.marine.MarineScreen
import com.vent.app.ui.now.NowScreen


@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentRoute?.startsWith(destination.label, ignoreCase = true) == true ||
                        (currentRoute?.contains(destination.route.split("?")[0]) == true)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.routeFor()) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.NOW.routeFor(),
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.NOW.routeFor()) {
                NowScreen(
                    onOpenCompass = {
                        navController.navigate(Destination.MARINE.routeFor("compass"))
                    },
                )
            }
            composable(
                route = Destination.MARINE.route,
                arguments = listOf(
                    navArgument("focus") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                ),
            ) { backStackEntry ->
                val focus = backStackEntry.arguments?.getString("focus").orEmpty()
                MarineScreen(focus = focus)
            }
            composable(Destination.MAP.routeFor()) { PlaceholderScreen("Map") }
            composable(Destination.SETTINGS.routeFor()) { Text(text = "Settings") }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name — coming soon")
    }
}
