package com.example.evfinder.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evfinder.ui.theme.EvColors

// ─── Primary CTA: solid green, rounded-lg (8dp), 56dp tall ────────────────────
@Composable
fun EvPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) EvColors.Primary else EvColors.PrimaryDim)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = EvColors.OnPrimary, strokeWidth = 2.5.dp)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let { Icon(it, null, tint = EvColors.OnPrimary, modifier = Modifier.size(18.dp)) }
                Text(
                    text,
                    color = if (enabled) EvColors.OnPrimary else EvColors.OnSurfaceVar,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                trailingIcon?.let { Icon(it, null, tint = EvColors.OnPrimary, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

// ─── Secondary: transparent, outline border, on-surface text ─────────────────
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .border(1.dp, EvColors.Outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leadingContent?.invoke()
            Text(text, color = EvColors.OnSurface, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ─── Tonal card: surface-container, rounded-xl (12dp) ────────────────────────
@Composable
fun EvCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = EvColors.Surface,
    cornerRadius: Dp = 12.dp,
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

// ─── Section label: uppercase, tracked, on-surface-variant ───────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EvColors.OnSurfaceVar,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

// ─── Pulsing live dot ────────────────────────────────────────────────────────
@Composable
fun LiveDot(live: Boolean = true, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val color = if (live) EvColors.Primary else EvColors.OnSurfaceVar
    val pulse = rememberInfiniteTransition(label = "pulse")
    val a by pulse.animateFloat(1f, 0.35f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
    Spacer(modifier.size(size).alpha(if (live) a else 0.5f).background(color, CircleShape))
}

// ─── Status pill: primary/10 background, primary text, full radius ───────────
@Composable
fun StatusPill(label: String, isActive: Boolean = true, showDot: Boolean = true) {
    val bg = if (isActive) EvColors.Primary.copy(alpha = 0.15f) else EvColors.SurfaceHighest
    val fg = if (isActive) EvColors.Primary else EvColors.OnSurfaceVar
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (showDot) LiveDot(live = isActive, size = 6.dp)
        Text(label.uppercase(), color = fg, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.6.sp)
    }
}

// ─── Filter chip: active = primary/10 bg + primary border/text ───────────────
@Composable
fun EvFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EvColors.Primary.copy(alpha = 0.12f) else EvColors.SurfaceHigh)
            .border(
                1.dp,
                if (selected) EvColors.Primary.copy(alpha = 0.35f) else EvColors.OutlineVariant,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let {
            Icon(it, null, tint = if (selected) EvColors.Primary else EvColors.OnSurface,
                modifier = Modifier.size(14.dp))
        }
        Text(
            label,
            color = if (selected) EvColors.Primary else EvColors.OnSurface,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── App bar: title + optional subtitle + nav/actions ────────────────────────
@Composable
fun EvTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationContent: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationContent?.invoke()
        if (navigationContent != null) Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = EvColors.OnBackground,
                fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar)
            }
        }
        actions?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) }
    }
}

// ─── Divider with centered uppercase label ───────────────────────────────────
@Composable
fun LabeledDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.75f)) {
        HorizontalDivider(Modifier.weight(1f), color = EvColors.OutlineVariant)
        Text(
            "  ${text.uppercase()}  ",
            style = MaterialTheme.typography.labelSmall,
            color = EvColors.OnSurfaceVar,
            letterSpacing = 0.8.sp
        )
        HorizontalDivider(Modifier.weight(1f), color = EvColors.OutlineVariant)
    }
}

// ─── Text field: input bg, subtle border, green focus ring ───────────────────
@Composable
fun EvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    uppercase: Boolean = false
) {
    Column(modifier) {
        label?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = EvColors.OnSurfaceVar,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = EvColors.OnSurfaceVar.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = EvColors.OnSurface,
                letterSpacing = if (uppercase) 1.sp else 0.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EvColors.Primary,
                unfocusedBorderColor = EvColors.InputBorder,
                focusedContainerColor = EvColors.InputBackground,
                unfocusedContainerColor = EvColors.InputBackground,
                cursorColor = EvColors.Primary,
                focusedTextColor = EvColors.OnSurface,
                unfocusedTextColor = EvColors.OnSurface,
                focusedLeadingIconColor = EvColors.Primary,
                unfocusedLeadingIconColor = EvColors.OnSurfaceVar
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon?.let {
                { Icon(it, null, modifier = Modifier.size(20.dp)) }
            }
        )
    }
}

// ─── Stat block: label above, mono value below ───────────────────────────────
@Composable
fun EvStatBlock(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = EvColors.OnSurface) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EvColors.SurfaceHigh)
            .padding(12.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
            color = EvColors.OnSurfaceVar, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor,
            fontWeight = FontWeight.SemiBold)
    }
}

// ─── Step progress bar (booking flow stepper) ────────────────────────────────
@Composable
fun EvStepBar(current: Int, total: Int, labels: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val step = index + 1
            val done = step < current
            val active = step == current
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (done || active) EvColors.Primary
                            else EvColors.Surface
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Step $step",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) EvColors.Primary else EvColors.OnSurfaceVar,
                    letterSpacing = 0.6.sp
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done || active) EvColors.OnSurface else EvColors.OnSurfaceVar
                )
            }
        }
    }
}

// ─── Circle map marker (fuel / LPG POIs) ─────────────────────────────────────
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

// ─── Animated processing view (payment) ──────────────────────────────────────
@Composable
fun EvProcessingView(message: String = "Processing payment…") {
    val pulse = rememberInfiniteTransition(label = "proc")
    val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(EvColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                Modifier.size(44.dp).alpha(a),
                color = EvColors.Primary,
                strokeWidth = 3.dp
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(message, color = EvColors.OnSurface, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Please don't close this screen", style = MaterialTheme.typography.labelSmall,
            color = EvColors.OnSurfaceVar)
    }
}
