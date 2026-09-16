package com.example.evfinder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.ui.theme.EvColors

// ─── Green gradient CTA button ─────────────────────────────────────────────────
@Composable
fun EvPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = if (enabled)
                    Brush.horizontalGradient(listOf(EvColors.Primary, EvColors.PrimaryDark))
                else
                    Brush.horizontalGradient(listOf(EvColors.PrimaryDim, EvColors.PrimaryDim)),
                alpha = alpha
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = EvColors.OnPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = EvColors.OnPrimary, modifier = Modifier.size(18.dp))
                }
                Text(
                    text,
                    color = if (enabled) EvColors.OnPrimary else EvColors.OnSurfaceVar,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ─── Outlined secondary button (dark bordered) ────────────────────────────────
@Composable
fun EvOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(EvColors.SurfaceHigh)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingContent?.invoke()
            Text(text, color = EvColors.OnSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

// ─── Dark card container ───────────────────────────────────────────────────────
@Composable
fun EvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = EvColors.Surface,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val mod = if (onClick != null) modifier.clip(shape).clickable(onClick = onClick) else modifier
    Card(
        modifier = mod,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

// ─── Section header label ────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EvColors.OnSurfaceVar,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

// ─── Pulsing live dot badge ────────────────────────────────────────────────────
@Composable
fun LiveDot(live: Boolean = true, modifier: Modifier = Modifier) {
    val color = if (live) EvColors.Primary else EvColors.OnSurfaceVar
    val pulse = rememberInfiniteTransition(label = "pulse")
    val a by pulse.animateFloat(
        1f, 0.3f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dot_alpha"
    )
    Spacer(
        modifier
            .size(8.dp)
            .alpha(if (live) a else 0.5f)
            .background(color, CircleShape)
    )
}

// ─── Bolt icon chip (station status) ─────────────────────────────────────────
@Composable
fun StatusPill(label: String, isActive: Boolean = true) {
    val bg = if (isActive) EvColors.PrimaryDim else EvColors.SurfaceHigh
    val fg = if (isActive) EvColors.Primary else EvColors.OnSurfaceVar
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LiveDot(live = isActive)
        Text(label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

// ─── EV App bar row ──────────────────────────────────────────────────────────
@Composable
fun EvTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationContent: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationContent?.invoke()
        if (navigationContent != null) Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
            if (subtitle != null)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
        }
        actions?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) }
    }
}

// ─── Divider with label ───────────────────────────────────────────────────────
@Composable
fun LabeledDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = EvColors.SurfaceBorder)
        Text(
            " $text ",
            style = MaterialTheme.typography.labelSmall,
            color = EvColors.OnSurfaceVar
        )
        HorizontalDivider(Modifier.weight(1f), color = EvColors.SurfaceBorder)
    }
}

// ─── Circle map marker (fuel / LPG POIs) ──────────────────────────────────────
fun evCircleMarker(context: android.content.Context, color: Int): android.graphics.drawable.Drawable {
    val sizePx = (14 * context.resources.displayMetrics.density).toInt()
    val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, paint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
}

// ─── Animated processing view ─────────────────────────────────────────────────
@Composable
fun AnimatedProcessingView() {
    val inf = rememberInfiniteTransition(label = "proc")
    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "proc_a")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Bolt,
                null,
                tint = EvColors.Primary.copy(alpha = a),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Processing payment…", color = EvColors.OnSurfaceVar)
        }
    }
}
