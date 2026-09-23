package com.example.evfinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.ui.theme.EvColors
import kotlinx.coroutines.launch

/**
 * Bell button with an unread badge. When the unread count *increases* the bell
 * swings and the badge pops, so a new notification is noticeable from any
 * screen instead of silently changing a number.
 *
 * @param unread       current unread count (0 hides the badge)
 * @param onClick      opens the notification inbox
 */
@Composable
fun EvNotificationBell(
    unread: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animatable states for the "new notification" flourish
    val swing = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    var previousCount by remember { mutableIntStateOf(unread) }

    LaunchedEffect(unread) {
        if (unread > previousCount) {
            // Bell swings, badge pops, and a green ring flashes behind it.
            swing.snapTo(0f)
            badgeScale.snapTo(0.5f)
            glow.snapTo(1f)
            launch { swing.animateTo(0f, keyframes {
                durationMillis = 900
                0f at 0 using FastOutSlowInEasing
                -20f at 90
                16f at 190
                -11f at 290
                6f at 390
                -2f at 500
                0f at 620
            }) }
            launch {
                badgeScale.animateTo(1f, spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ))
            }
            glow.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
        }
        previousCount = unread
    }

    // While anything is unread, keep a slow breathing pulse on the badge so it
    // stays noticeable after the initial swing.
    val pulse by rememberInfiniteTransition(label = "bellPulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // Outer box is deliberately NOT clipped: the badge sits on its top-right
    // corner and must be able to overflow the button, otherwise it gets cut off
    // by the button's rounded-corner clip.
    Box(modifier.size(36.dp)) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EvColors.SurfaceHigh)
                .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = if (unread > 0) "Notifications, $unread unread" else "Notifications",
                tint = if (unread > 0) EvColors.Primary else EvColors.OnSurface,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = swing.value }
            )
        }

        if (unread > 0) {
            // flash ring on arrival, centred on the icon
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .graphicsLayer {
                        val s = 1f + glow.value * 0.6f
                        scaleX = s; scaleY = s
                        alpha = glow.value * 0.5f
                    }
                    .clip(CircleShape)
                    .background(EvColors.Primary.copy(alpha = 0.35f))
            )
            // count badge — sits above the button's top edge
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .graphicsLayer { scaleX = badgeScale.value; scaleY = badgeScale.value }
                    .clip(CircleShape)
                    .background(EvColors.Primary)
                    .border(1.5.dp, EvColors.Background, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (unread > 99) "99+" else unread.toString(),
                    color = EvColors.OnPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.graphicsLayer { alpha = pulse }
                )
            }
        }
    }
}
