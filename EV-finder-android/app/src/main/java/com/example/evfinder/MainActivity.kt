package com.example.evfinder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.evfinder.feature.map.MapScreen
import com.example.evfinder.feature.operator.OperatorApp
import com.example.evfinder.feature.profile.ProfileScreen
import com.example.evfinder.feature.station.StationDetailScreen
import com.example.evfinder.feature.vehicle.VehiclesScreen
import com.example.evfinder.ui.theme.EVFinderTheme
import com.example.evfinder.ui.theme.EvColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(this)
        setContent {
            EVFinderTheme {
                // Role-based entry (context §9): operators get their own shell;
                // every rule is still enforced by the backend.
                if (tokenStore.role == "OPERATOR") {
                    OperatorApp(onSessionExpired = {
                        tokenStore.clear()
                        recreate() // relaunches at login
                    })
                } else {
                    EVFinderApp(
                        startDestination = if (tokenStore.isLoggedIn) "home" else "login",
                        tokenStore = tokenStore
                    )
                }
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val userBottomDestinations = listOf(
    BottomDestination("home",     R.string.nav_home,     Icons.Filled.EvStation,    Icons.Outlined.EvStation),
    BottomDestination("bookings", R.string.nav_bookings, Icons.Filled.CalendarMonth,Icons.Outlined.CalendarMonth),
    BottomDestination("vehicles", R.string.nav_vehicles, Icons.Filled.ElectricCar,  Icons.Outlined.ElectricCar),
    BottomDestination("profile",  R.string.nav_profile,  Icons.Filled.Person,       Icons.Outlined.Person),
)

private val bottomRoutes = userBottomDestinations.map { it.route }.toSet()

// Screens reached from the Stations tab. The bottom bar stays visible here so the
// user can jump to any tab in the middle of the booking flow.
private val stationFlowRoutes = setOf("station/{stationId}", "book/{serviceId}", "payment/{bookingId}")

@Composable
fun EVFinderApp(startDestination: String, tokenStore: TokenStore) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomRoutes || currentRoute in stationFlowRoutes
    // Booking-flow screens highlight the tab they belong to (Stations).
    val activeRoute = if (currentRoute in bottomRoutes) currentRoute else "home"

    Scaffold(
        containerColor = EvColors.Background,
        bottomBar = {
            if (showBottomBar) {
                EvBottomNav(
                    destinations = userBottomDestinations,
                    currentRoute = activeRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop the start destination inclusively so every tab switch
                            // re-pushes a clean entry. Without `inclusive = true`, the
                            // first time a tab is re-tapped after `popUpTo("home")` left
                            // home on the back stack, the controller short-circuits the
                            // navigation (no-op) because the start destination is still
                            // present and `launchSingleTop` + `restoreState` collapse to
                            // a no-op for the home tab specifically.
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(if (showBottomBar) padding else PaddingValues(0.dp))
        ) {
            // ---- Auth ----
            composable("login") {
                val activityContext = LocalContext.current
                LoginScreen(
                    onAuthenticated = {
                        if (tokenStore.role == "OPERATOR") {
                            // operators enter their own shell: recreate activity
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
                        if (tokenStore.role == "OPERATOR") {
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
                HomeScreen(
                    onStationClick = { stationId -> navController.navigate("station/$stationId") },
                    onOpenMap = { navController.navigate("map") },
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
            // Map: pushed from the Home dashboard (fullscreen)
            composable("map") {
                val context = LocalContext.current
                MapScreen(
                    onStationClick = { stationId ->
                        navController.navigate("station/$stationId")
                    },
                    onGetDirections = { lat, lng, name ->
                        // same Google Maps handoff as station details
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
                )
            }
            // Profile tab
            composable("profile") {
                ProfileScreen(onLogout = {
                    tokenStore.clear()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                })
            }
            // Vehicles tab (full CRUD)
            composable("vehicles") {
                VehiclesScreen()
            }

            // ---- Station details ----
            composable("station/{stationId}") { entry ->
                val stationId = entry.arguments?.getString("stationId") ?: return@composable
                val context = LocalContext.current // capture in composable scope
                StationDetailScreen(
                    stationId = stationId,
                    onBack = { navController.popBackStack() },
                    onBookService = { _, serviceId -> navController.navigate("book/$serviceId") },
                    onGetDirections = { lat, lng, name ->
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
                )
            }

            // ---- Booking flow ----
            composable("book/{serviceId}") { entry ->
                val serviceId = entry.arguments?.getString("serviceId") ?: return@composable
                BookingScreen(
                    serviceId = serviceId,
                    onBack = { navController.popBackStack() },
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
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate("bookings") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

// ─── Premium bottom navigation bar ────────────────────────────────────────────
@Composable
private fun EvBottomNav(
    destinations: List<BottomDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(EvColors.Background)
    ) {
        // Top border line
        Divider(
            Modifier.fillMaxWidth().align(Alignment.TopCenter),
            color = EvColors.SurfaceBorder,
            thickness = 0.5.dp
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            destinations.forEach { dest ->
                val selected = currentRoute == dest.route
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .let {
                            if (selected) it.background(EvColors.PrimaryDim) else it
                        }
                        .clickable { onNavigate(dest.route) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .widthIn(min = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes),
                        tint = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        stringResource(dest.labelRes),
                        color = if (selected) EvColors.Primary else EvColors.OnSurfaceVar,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun Divider(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = EvColors.SurfaceBorder, thickness: androidx.compose.ui.unit.Dp = 1.dp) {
    HorizontalDivider(modifier, color = color, thickness = thickness)
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Construction, null, tint = EvColors.OnSurfaceVar, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
            Text("Coming soon", color = EvColors.OnSurfaceVar, style = MaterialTheme.typography.bodySmall)
        }
    }
}
