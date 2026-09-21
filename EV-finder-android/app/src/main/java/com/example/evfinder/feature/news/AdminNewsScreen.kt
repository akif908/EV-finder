package com.example.evfinder.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.NewsDto
import com.example.evfinder.feature.auth.EvTextField
import com.example.evfinder.ui.components.EvCard
import com.example.evfinder.ui.components.EvPrimaryButton
import com.example.evfinder.ui.components.EvTopBar
import com.example.evfinder.ui.theme.EvColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin news management: list everything (drafts included), create/edit,
 * publish toggle, delete. Backend still enforces the ADMIN role on every call.
 */
@Composable
fun AdminNewsScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: AdminNewsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<NewsDto?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NewsDto?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EvColors.Background)
    ) {
        EvTopBar(
            title = "News Management",
            subtitle = "Admin · Energy & Fuel News",
            navigationContent = {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.SurfaceHigh)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = EvColors.OnSurface, modifier = Modifier.size(18.dp))
                }
            },
            actions = {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.PrimaryDim)
                        .clickable {
                            editing = null
                            showForm = true
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Add, null, tint = EvColors.Primary, modifier = Modifier.size(16.dp))
                    Text("New", style = MaterialTheme.typography.labelMedium,
                        color = EvColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        )

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EvColors.Primary)
            }

            state.error != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error!!, color = EvColors.Error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                if (state.error!!.startsWith("Session expired")) {
                    EvPrimaryButton("Log in again", onSessionExpired, modifier = Modifier.fillMaxWidth(0.6f))
                } else {
                    TextButton(onClick = viewModel::load) { Text("Retry", color = EvColors.Primary) }
                }
            }

            state.news.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No news yet — create the first article.", color = EvColors.OnSurfaceVar)
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.news, key = { it.id }) { news ->
                    AdminNewsRow(
                        news = news,
                        onEdit = {
                            editing = news
                            showForm = true
                        },
                        onTogglePublish = { viewModel.setPublished(news.id, !news.isPublished) },
                        onDelete = { deleting = news }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (showForm) {
        NewsFormDialog(
            editing = editing,
            saving = state.saving,
            onSave = { title, shortDesc, content, category, source, sourceUrl, imageUrl, publishedAt, isPublished ->
                viewModel.save(
                    editing?.id, title, shortDesc, content, category,
                    source, sourceUrl, imageUrl, publishedAt, isPublished
                ) { error ->
                    if (error == null) showForm = false
                }
            },
            onDismiss = { showForm = false }
        )
    }

    deleting?.let { news ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = EvColors.Surface,
            title = { Text("Delete news?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${news.title}\" will be removed permanently.", color = EvColors.OnSurface) },
            confirmButton = {
                EvPrimaryButton("Delete", onClick = {
                    viewModel.delete(news.id)
                    deleting = null
                })
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = EvColors.OnSurfaceVar) }
            }
        )
    }
}

@Composable
private fun AdminNewsRow(
    news: NewsDto,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    val meta = newsCategoryMeta(news.category)
    EvCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meta.emoji)
                Spacer(Modifier.width(8.dp))
                Text(
                    news.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = EvColors.OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, "Edit", tint = EvColors.OnSurfaceVar, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = EvColors.Error, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${meta.label} · ${relativeNewsTime(news.publishedAt).ifBlank { "No date" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EvColors.OnSurfaceVar,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (news.isPublished) "Published" else "Draft",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (news.isPublished) EvColors.Primary else EvColors.Warning
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = news.isPublished,
                    onCheckedChange = { onTogglePublish() },
                    modifier = Modifier.height(26.dp)
                )
            }
        }
    }
}

/** "2026-09-25 10:00" or "2026-09-25T10:00" -> ISO "yyyy-MM-dd'T'HH:mm", or null. */
private fun parseAdminDateInput(input: String): String? {
    val normalized = input.trim().replace(' ', 'T')
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(normalized) ?: return null
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date(parsed.time))
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun NewsFormDialog(
    editing: NewsDto?,
    saving: Boolean,
    onSave: (title: String, shortDescription: String, content: String, category: String,
             source: String, sourceUrl: String, imageUrl: String,
             publishedAt: String, isPublished: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var shortDesc by remember { mutableStateOf(editing?.shortDescription ?: "") }
    var content by remember { mutableStateOf(editing?.content ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: "GAS_CNG") }
    var source by remember { mutableStateOf(editing?.source ?: "") }
    var sourceUrl by remember { mutableStateOf(editing?.sourceUrl ?: "") }
    var imageUrl by remember { mutableStateOf(editing?.imageUrl ?: "") }
    var publishedAt by remember {
        mutableStateOf(
            editing?.publishedAt?.takeIf { it.isNotBlank() }
                ?.let { it.replace('T', ' ').take(16) }
                ?: nowIsoForNews().replace('T', ' ')
        )
    }
    var isPublished by remember { mutableStateOf(editing?.isPublished ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = EvColors.Surface,
        title = { Text(if (editing == null) "New News" else "Edit News", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                EvTextField(title, { title = it }, "Title *")
                Spacer(Modifier.height(8.dp))
                EvTextField(shortDesc, { shortDesc = it }, "Short description *")
                Spacer(Modifier.height(8.dp))
                EvTextField(content, { content = it }, "Full content *", singleLine = false)
                Spacer(Modifier.height(12.dp))

                Text("Category", style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allNewsCategories.chunked(2).forEach { rowCategories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowCategories.forEach { key ->
                                NewsTypePill(newsCategoryMeta(key).label, category == key, Modifier.weight(1f)) {
                                    category = key
                                }
                            }
                            if (rowCategories.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                EvTextField(source, { source = it }, "Source name (e.g. The Daily Star)")
                Spacer(Modifier.height(8.dp))
                EvTextField(sourceUrl, { sourceUrl = it }, "Source URL (opens in browser)")
                Spacer(Modifier.height(8.dp))
                EvTextField(imageUrl, { imageUrl = it }, "Image URL (optional)")
                Spacer(Modifier.height(8.dp))
                EvTextField(publishedAt, { publishedAt = it }, "Publication date (yyyy-MM-dd HH:mm) *")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isPublished = !isPublished }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published", style = MaterialTheme.typography.bodyMedium,
                        color = EvColors.OnSurface, modifier = Modifier.weight(1f))
                    Switch(checked = isPublished, onCheckedChange = { isPublished = it })
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            EvPrimaryButton(
                text = if (saving) "Saving…" else "Save",
                onClick = {
                    val isoDate = parseAdminDateInput(publishedAt)
                    when {
                        title.isBlank() -> error = "Title is required"
                        shortDesc.isBlank() -> error = "Short description is required"
                        content.isBlank() -> error = "Content is required"
                        isoDate == null -> error = "Date must look like 2026-09-25 10:00"
                        else -> {
                            error = null
                            onSave(title, shortDesc, content, category, source, sourceUrl, imageUrl, isoDate, isPublished)
                        }
                    }
                },
                enabled = !saving
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancel", color = EvColors.OnSurfaceVar)
            }
        }
    )
}

@Composable
private fun NewsTypePill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EvColors.PrimaryDim else EvColors.SurfaceHigh)
            .border(1.dp, if (selected) EvColors.Primary else EvColors.SurfaceBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) EvColors.Primary else EvColors.OnSurface,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
