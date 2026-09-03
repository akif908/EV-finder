package com.example.evfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.evfinder.core.storage.TokenStore
import com.example.evfinder.feature.auth.LoginScreen
import com.example.evfinder.feature.auth.RegisterScreen
import com.example.evfinder.feature.booking.BookingsScreen
import com.example.evfinder.feature.booking.BookingScreen
import com.example.evfinder.feature.booking.PaymentScreen
import com.example.evfinder.feature.home.HomeScreen
import com.example.evfinder.feature.station.StationDetailScreen
import com.example.evfinder.ui.theme.EVFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(this)
        setContent {
            EVFinderTheme {
                // Start at login on first launch, home when a JWT is already stored.
                EVFinderApp(
                    startDestination = if (tokenStore.isLoggedIn) "home" else "login",
                    tokenStore = tokenStore
                )
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

// USER navigation per project context §8.
// OPERATOR/ADMIN screens will swap this set based on the logged-in role
// (real authorization always enforced by the backend).
private val userBottomDestinations = listOf(
    BottomDestination("home", R.string.nav_home, Icons.Filled.Home),
    BottomDestination("map", R.string.nav_map, Icons.Filled.Map),
    BottomDestination("bookings", R.string.nav_bookings, Icons.Filled.CalendarMonth),
    BottomDestination("vehicles", R.string.nav_vehicles, Icons.Filled.ElectricCar),
    BottomDestination("profile", R.string.nav_profile, Icons.Filled.Person)
)

private val bottomRoutes = userBottomDestinations.map { it.route }.toSet()

@Composable
fun EVFinderApp(startDestination: String, tokenStore: TokenStore) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    userBottomDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(if (showBottomBar) padding else androidx.compose.foundation.layout.PaddingValues(0.dp))
        ) {
            // ---- Auth (no bottom bar) ----
            composable("login") {
                LoginScreen(
                    onAuthenticated = {
                        navController.navigate("home") { popUpTo(0) { inclusive = true } }
                    },
                    onGoToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onAuthenticated = {
                        navController.navigate("home") { popUpTo(0) { inclusive = true } }
                    },
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            // ---- Main tabs ----
            composable("home") {
                HomeScreen(
                    onStationClick = { stationId -> navController.navigate("station/$stationId") },
                    onSessionExpired = {
                        tokenStore.clear()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("bookings") {
                BookingsScreen(onSessionExpired = {
                    tokenStore.clear()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                })
            }
            userBottomDestinations
                .filter { it.route != "home" && it.route != "bookings" }
                .forEach { dest ->
                    composable(dest.route) { PlaceholderScreen(stringResource(dest.labelRes)) }
                }

            // ---- Station details (pushed, no bottom bar) ----
            composable("station/{stationId}") { entry ->
                val stationId = entry.arguments?.getString("stationId") ?: return@composable
                StationDetailScreen(
                    stationId = stationId,
                    onBookService = { _, serviceId ->
                        navController.navigate("book/$serviceId")
                    }
                )
            }

            // ---- Booking flow (pushed, no bottom bar) ----
            composable("book/{serviceId}") { entry ->
                val serviceId = entry.arguments?.getString("serviceId") ?: return@composable
                BookingScreen(
                    serviceId = serviceId,
                    onBookingCreated = { bookingId ->
                        navController.navigate("payment/$bookingId") {
                            popUpTo("book/$serviceId") { inclusive = true }
                        }
                    }
                )
            }
            composable("payment/{bookingId}") { entry ->
                val bookingId = entry.arguments?.getString("bookingId") ?: return@composable
                PaymentScreen(
                    bookingId = bookingId,
                    onDone = {
                        navController.navigate("bookings") {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "$title — screen to be implemented",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
