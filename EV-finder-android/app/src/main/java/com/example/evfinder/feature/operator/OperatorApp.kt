package com.example.evfinder.feature.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.evfinder.feature.profile.ProfileScreen
import com.example.evfinder.ui.theme.EvColors

/**
 * Operator shell: role-based section per project context §9 — same single app,
 * different navigation for OPERATOR accounts. Backend still enforces every rule.
 */
private data class OperatorDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val operatorDestinations = listOf(
    OperatorDestination("op_dashboard", "Dashboard", Icons.Filled.SpaceDashboard, Icons.Outlined.SpaceDashboard),
    OperatorDestination("op_stations", "Stations", Icons.Filled.EvStation, Icons.Outlined.EvStation),
    OperatorDestination("op_bookings", "Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    OperatorDestination("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun OperatorApp(onSessionExpired: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = EvColors.Background,
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(EvColors.Background)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    operatorDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) EvColors.PrimaryDim else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label,
                                tint = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                dest.label,
                                color = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "op_dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("op_dashboard") { OperatorDashboardScreen(onSessionExpired) }
            composable("op_stations") { OperatorStationsScreen(onSessionExpired) }
            composable("op_bookings") { OperatorBookingsScreen(onSessionExpired) }
            composable("profile") { ProfileScreen(onLogout = onSessionExpired) }
        }
    }
}

@Composable
fun OperatorBookingsScreen(x0: () -> Unit) {
    TODO("Not yet implemented")
}
