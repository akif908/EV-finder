package com.example.evfinder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.ui.theme.EvColors

/**
 * Mobile design widgets shared across screens (from the Voltage Mobility
 * phone mockups): progress rings/bars, segmented tabs, toggles, stat tiles.
 */

// ─── Thin progress bar (steppers, KPIs, charging) ─────────────────────────────
@Composable
fun EvProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = EvColors.Primary,
    track: Color = EvColors.SurfaceHighest,
    height: Dp = 6.dp
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(500), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

// ─── Circular progress ring (battery SOC, charging %) ────────────────────────
@Composable
fun EvProgressRing(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    stroke: Dp = 7.dp,
    color: Color = EvColors.Primary,
    track: Color = EvColors.SurfaceHighest,
    centerLabel: String? = null,
    centerSub: String? = null
) {
    val fraction = (percent.coerceIn(0, 100)) / 100f
    val animated by animateFloatAsState(fraction, tween(700), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel ?: "$percent%",
                style = MaterialTheme.typography.titleLarge,
                color = EvColors.OnSurface, fontWeight = FontWeight.Bold
            )
            centerSub?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
        }
    }
}

// ─── Segmented tabs (Upcoming/Past/Cancelled, CCS/NACS) ──────────────────────
@Composable
fun EvSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    counts: List<Int>? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EvColors.SurfaceLowest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) EvColors.SurfaceHigh else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) EvColors.Primary else EvColors.OnSurfaceVar,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
                counts?.getOrNull(index)?.takeIf { it > 0 }?.let { count ->
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EvColors.Primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text("$count", style = MaterialTheme.typography.labelSmall,
                            color = EvColors.Primary)
                    }
                }
            }
        }
    }
}

// ─── Toggle switch (settings rows) ───────────────────────────────────────────
@Composable
fun EvToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 48.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) EvColors.Primary else EvColors.SurfaceHighest)
            .clickable { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) EvColors.OnPrimary else EvColors.OnSurfaceVar)
        )
    }
}

// ─── 36–40dp icon tile ───────────────────────────────────────────────────────
@Composable
fun EvIconTile(
    icon: ImageVector,
    tint: Color = EvColors.Primary,
    size: Dp = 40.dp,
    container: Color = EvColors.SurfaceHigh,
    radius: Dp = 12.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

// ─── Dashboard KPI tile with optional progress footer ───────────────────────
@Composable
fun EvStatTile(
    icon: ImageVector,
    caption: String,
    value: String,
    unit: String? = null,
    footer: String? = null,
    footerIcon: ImageVector? = null,
    accent: Color = EvColors.Primary,
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    com.example.evfinder.ui.components.EvCard(modifier, color = EvColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(caption.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = EvColors.OnSurfaceVar, letterSpacing = 0.6.sp,
                    modifier = Modifier.weight(1f))
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall,
                    color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                unit?.let {
                    Spacer(Modifier.width(3.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = accent)
                }
            }
            progress?.let {
                Spacer(Modifier.height(8.dp))
                EvProgressBar(it, color = accent, height = 5.dp)
            }
            footer?.let {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    footerIcon?.let { fi ->
                        Icon(fi, null, tint = accent, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(it, style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
        }
    }
}

// ─── Compact spec tile (station card bento: kW / connector / open) ───────────
@Composable
fun EvSpecTile(
    icon: ImageVector?,
    value: String,
    caption: String,
    accent: Color = EvColors.Primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(EvColors.Surface)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon?.let {
            Icon(it, null, tint = accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.height(5.dp))
        }
        Text(value, style = MaterialTheme.typography.labelLarge, color = EvColors.OnSurface,
            fontWeight = FontWeight.Bold, maxLines = 1)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar,
            fontSize = 10.sp, maxLines = 1)
    }
}

// ─── Section header (uppercase, tracked, optional trailing action) ───────────
@Composable
fun EvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = EvColors.Primary,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge,
            color = accent, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

// ─── Setting / list row ──────────────────────────────────────────────────────
@Composable
fun EvSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: Color = EvColors.Primary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EvIconTile(icon, tint = accent, size = 38.dp, container = EvColors.SurfaceHighest)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = EvColors.OnSurface)
            subtitle?.let {
                Spacer(Modifier.height(1.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
            }
        }
        trailing?.invoke()
    }
}

// ─── Status chip with icon (Confirmed / Parked / Completed…) ────────────────
@Composable
fun EvStatusChip(
    label: String,
    accent: Color = EvColors.Primary,
    icon: ImageVector? = null,
    pulsingDot: Boolean = false
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        when {
            pulsingDot -> LiveDot(live = true, size = 6.dp)
            icon != null -> Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent,
            fontWeight = FontWeight.SemiBold)
    }
}

// ─── Big CTA (checkout / confirm actions) ────────────────────────────────────
@Composable
fun EvWideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    container: Color = EvColors.PrimaryContainer,
    contentColor: Color = EvColors.OnPrimary,
    enabled: Boolean = true
) {
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) container else EvColors.SurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.let {
            Icon(it, null, tint = if (enabled) contentColor else EvColors.OnSurfaceVar,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) contentColor else EvColors.OnSurfaceVar,
            fontWeight = FontWeight.Bold
        )
        trailingIcon?.let {
            Spacer(Modifier.width(8.dp))
            Icon(it, null, tint = if (enabled) contentColor else EvColors.OnSurfaceVar,
                modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Check row (checkbox + label, used in forms) ─────────────────────────────
@Composable
fun EvCheckRow(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) EvColors.Primary else EvColors.SurfaceHighest)
                .border(1.dp, if (checked) EvColors.Primary else EvColors.OutlineVariant,
                    RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Filled.Check, null, tint = EvColors.OnPrimary,
                modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
    }
}

// ─── Dashed ticket perforation divider (receipt card) ───────────────────────
@Composable
fun EvPerforation(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().alpha(0.6f), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 14.dp, height = 22.dp).clip(RoundedCornerShape(50)).background(EvColors.Background))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(EvColors.OutlineVariant)
        )
        Box(Modifier.size(width = 14.dp, height = 22.dp).clip(RoundedCornerShape(50)).background(EvColors.Background))
    }
}
