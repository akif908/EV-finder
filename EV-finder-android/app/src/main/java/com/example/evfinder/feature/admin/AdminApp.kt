package com.example.evfinder.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private data class AdminDestination(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

private val adminDestinations = listOf(
    AdminDestination("admin_dashboard", "Overview", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
    AdminDestination("admin_users", "Users", Icons.Filled.People, Icons.Outlined.People),
    AdminDestination("admin_bookings", "Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    AdminDestination("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun AdminApp(onSessionExpired: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = EvColors.Background,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(EvColors.Background).padding(horizontal = 8.dp, vertical = 8.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                adminDestinations.forEach { dest ->
                    val selected = currentRoute == dest.route
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (selected) EvColors.PrimaryDim else Color.Transparent)
                            .clickable {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, dest.label,
                            tint = if (selected) EvColors.Primary else EvColors.OnSurfaceVar, modifier = Modifier.size(22.dp))
                        Text(dest.label, color = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                            fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "admin_dashboard", modifier = Modifier.padding(padding)) {
            composable("admin_dashboard") { AdminDashboardScreen(onSessionExpired) }
            composable("admin_users") { AdminUsersScreen(onSessionExpired) }
            composable("admin_bookings") { AdminBookingsScreen(onSessionExpired) }
            composable("profile") { ProfileScreen(onLogout = onSessionExpired) }
        }
    }
}
