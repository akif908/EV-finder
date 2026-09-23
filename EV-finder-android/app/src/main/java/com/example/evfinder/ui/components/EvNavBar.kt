package com.example.evfinder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.ui.theme.EvColors

data class EvNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Shared bottom navigation — pill layout with the electric-green selected
 * state used across the app (user, operator and admin shells). Items also
 * tint green while hovered (mouse / stylus), so the bar feels alive on
 * tablets and desktop previews, not just on touch.
 */
@Composable
fun EvBottomNavBar(
    destinations: List<EvNavDestination>,
    selectedRoute: String?,
    onSelect: (EvNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(EvColors.Background)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        destinations.forEach { dest ->
            EvNavItem(
                destination = dest,
                selected = selectedRoute == dest.route ||
                    selectedRoute?.startsWith("${dest.route}/") == true,
                onClick = { onSelect(dest) }
            )
        }
    }
}

@Composable
private fun EvNavItem(
    destination: EvNavDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        when {
            selected -> EvColors.PrimaryDim
            hovered  -> EvColors.Primary.copy(alpha = 0.10f)
            else     -> Color.Transparent
        },
        tween(150), label = "navbg"
    )
    val contentTint by animateColorAsState(
        when {
            selected -> EvColors.Primary
            hovered  -> EvColors.Primary.copy(alpha = 0.7f)
            else     -> EvColors.OnSurfaceVar
        },
        tween(150), label = "navtint"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (selected) destination.selectedIcon else destination.unselectedIcon,
            destination.label,
            tint = contentTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            destination.label,
            color = contentTint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
