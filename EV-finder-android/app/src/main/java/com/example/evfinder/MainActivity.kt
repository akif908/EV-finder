package com.example.evfinder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.evfinder.core.storage.TokenStore
import com.example.evfinder.feature.admin.AdminApp
import com.example.evfinder.feature.auth.LoginScreen
import com.example.evfinder.feature.auth.RegisterScreen
import com.example.evfinder.feature.booking.BookingsScreen
import com.example.evfinder.feature.booking.BookingScreen
import com.example.evfinder.feature.booking.BookingReceiptScreen
import com.example.evfinder.feature.booking.PaymentScreen
import com.example.evfinder.feature.home.HomeScreen
import com.example.evfinder.feature.map.MapScreen
import com.example.evfinder.feature.notifications.NotificationsScreen
import com.example.evfinder.feature.operator.OperatorApp
import com.example.evfinder.feature.profile.ProfileScreen
import com.example.evfinder.feature.station.StationDetailScreen
import com.example.evfinder.feature.vehicle.VehiclesScreen
import com.example.evfinder.ui.theme.EVFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(this)
        setContent {
            EVFinderTheme {
                when (tokenStore.role) {
                    "OPERATOR" -> OperatorApp(onSessionExpired = { tokenStore.clear(); recreate() })
                    "ADMIN" -> AdminApp(onSessionExpired = { tokenStore.clear(); recreate() })
                    else -> EVFinderApp(
                        startDestination = if (tokenStore.isLoggedIn) "home" else "login",
                        tokenStore = tokenStore
                    )
                }
            }
        }
    }
}

// USER navigation per project context §8 — same pill design as operator/admin.
private val userBottomDestinations = listOf(
    Triple("home", R.string.nav_stations, Icons.Filled.EvStation to Icons.Outlined.EvStation),
    Triple("bookings", R.string.nav_bookings, Icons.Filled.CalendarMonth to Icons.Outlined.CalendarMonth),
    Triple("vehicles", R.string.nav_vehicles, Icons.Filled.DirectionsCar to Icons.Outlined.DirectionsCar),
    Triple("profile", R.string.nav_profile, Icons.Filled.Tune to Icons.Outlined.Tune)
)

private val bottomRoutes = userBottomDestinations.map { it.first }.toSet()

/** Resolved at composition so the string resources stay translatable. */
@Composable
private fun userNavDestinations() = userBottomDestinations.map { (route, labelRes, icons) ->
    com.example.evfinder.ui.components.EvNavDestination(
        route, stringResource(labelRes), icons.first, icons.second
    )
}

@Composable
fun EVFinderApp(startDestination: String, tokenStore: TokenStore) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomRoutes

    // Keep the bell badge current for the whole signed-in session.
    LaunchedEffect(Unit) {
        com.example.evfinder.feature.notifications.UnreadNotifications.start(this)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color(0xFF131313),
        bottomBar = {
            if (showBottomBar) {
                com.example.evfinder.ui.components.EvBottomNavBar(
                    destinations = userNavDestinations(),
                    selectedRoute = currentRoute,
                    onSelect = { dest ->
                        // Tab switching pops back to the tab root instead of
                        // navigating with saveState/restoreState. Two things
                        // went wrong with the idiomatic version here:
                        //   1. it anchored on graph.startDestinationId, which
                        //      is "login" whenever the app was launched
                        //      signed out — sign-in pops login, so the anchor
                        //      matched nothing and every tap duplicated the
                        //      destination, rotting the back stack.
                        //   2. restoreState could replay a previously saved
                        //      stack, so tapping "Stations" restored the
                        //      Bookings screen sitting on top of home.
                        val poppedToRoot = navController.popBackStack("home", inclusive = false)
                        if (!poppedToRoot) {
                            navController.navigate("home") { popUpTo(0) { inclusive = true } }
                        }
                        if (navController.currentDestination?.route != dest.route) {
                            navController.navigate(dest.route) { launchSingleTop = true }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // Always apply the Scaffold insets. Discarding them when the bottom
            // bar is hidden pushed pushed screens (notifications, station,
            // payment) under the status bar, so their back buttons collided with
            // the phone's top edge. Operator/admin always apply this.
            modifier = Modifier.padding(padding)
        ) {
            // ---- Auth ----
            composable("login") {
                val activityContext = LocalContext.current
                LoginScreen(
                    onAuthenticated = {
                        if (tokenStore.role == "OPERATOR" || tokenStore.role == "ADMIN") {
                            (activityContext as? ComponentActivity)?.recreate()
                        } else {
                            navController.navigate("home") { popUpTo(0) { inclusive = true } }
                        }
                    },
                    onGoToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                val activityContext = LocalContext.current
                RegisterScreen(
                    onAuthenticated = {
                        if (tokenStore.role == "OPERATOR" || tokenStore.role == "ADMIN") {
                            (activityContext as? ComponentActivity)?.recreate()
                        } else {
                            navController.navigate("home") { popUpTo(0) { inclusive = true } }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            // ---- Main tabs ----
            composable("home") {
                val context = LocalContext.current
                HomeScreen(
                    onStationClick = { stationId -> navController.navigate("station/$stationId") },
                    onOpenMap = { navController.navigate("map") },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onSessionExpired = {
                        tokenStore.clear()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    onGetDirections = { lat, lng, name -> openDirections(context, lat, lng, name) }
                )
            }
            composable("map") {
                val context = LocalContext.current
                MapScreen(
                    onStationClick = { stationId -> navController.navigate("station/$stationId") },
                    onGetDirections = { lat, lng, name -> openDirections(context, lat, lng, name) }
                )
            }
            composable("bookings") {
                BookingsScreen(
                    onSessionExpired = {
                        tokenStore.clear()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    onStationClick = { stationId -> navController.navigate("station/$stationId") },
                    onOpenNotifications = { navController.navigate("notifications") }
                )
            }
            composable("vehicles") { VehiclesScreen() }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        tokenStore.clear()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    onOpenReportIssue = { navController.navigate("report") }
                )
            }

            // ---- Pushed screens ----
            composable("notifications") {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable("report") {
                com.example.evfinder.feature.support.ReportIssueScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("report/{stationId}/{stationName}") { entry ->
                com.example.evfinder.feature.support.ReportIssueScreen(
                    onBack = { navController.popBackStack() },
                    stationId = entry.arguments?.getString("stationId"),
                    stationName = entry.arguments?.getString("stationName")
                )
            }
            composable("station/{stationId}") { entry ->
                val stationId = entry.arguments?.getString("stationId") ?: return@composable
                val context = LocalContext.current
                StationDetailScreen(
                    stationId = stationId,
                    onBack = { navController.popBackStack() },
                    onBookService = { _, serviceId -> navController.navigate("book/$serviceId") },
                    onGetDirections = { lat, lng, name -> openDirections(context, lat, lng, name) },
                    onReportIssue = { id, name ->
                        navController.navigate("report/$id/${Uri.encode(name)}")
                    }
                )
            }
            composable("book/{serviceId}") { entry ->
                val serviceId = entry.arguments?.getString("serviceId") ?: return@composable
                BookingScreen(
                    serviceId = serviceId,
                    onBookingCreated = { bookingId ->
                        navController.navigate("payment/$bookingId") {
                            popUpTo("book/$serviceId") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("receipt/{bookingId}") { entry ->
                val bookingId = entry.arguments?.getString("bookingId") ?: return@composable
                val context = LocalContext.current
                BookingReceiptScreen(
                    bookingId = bookingId,
                    onBack = { navController.popBackStack() },
                    onGetDirections = { lat, lng, name -> openDirections(context, lat, lng, name) },
                    onDone = {
                        navController.navigate("bookings") {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("payment/{bookingId}") { entry ->
                val bookingId = entry.arguments?.getString("bookingId") ?: return@composable
                PaymentScreen(
                    bookingId = bookingId,
                    onBack = { navController.popBackStack() },
                    onDone = { booking ->
                        // successful payment (or "back") lands on the receipt
                        navController.navigate("receipt/$booking") {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

private fun openDirections(context: android.content.Context, lat: Double, lng: Double, name: String) {
    // Google Maps turn-by-turn if installed, else any geo app
    val gmm = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))
        .setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(gmm)
    } catch (e: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($name)"))
        )
    }
}
