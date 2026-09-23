package com.example.evfinder.feature.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evfinder.core.model.IssueCategories
import com.example.evfinder.core.model.IssueDto
import com.example.evfinder.ui.components.*
import com.example.evfinder.ui.theme.EvColors

/**
 * Report a bug or station problem, and follow the reports you already filed.
 * Reports land in the admin inbox, and the admin's reply shows up here and in
 * the notification feed.
 */
@Composable
fun ReportIssueScreen(
    onBack: () -> Unit,
    /** Optional station to pre-attach (when opened from a station screen). */
    stationId: String? = null,
    stationName: String? = null,
    viewModel: ReportIssueViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var category by remember { mutableStateOf(IssueCategories.ALL.first()) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().background(EvColors.Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── App bar ───────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvColors.SurfaceHigh)
                        .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = EvColors.OnSurface, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Report an issue", style = MaterialTheme.typography.titleLarge,
                        color = EvColors.OnBackground, fontWeight = FontWeight.Bold)
                    Text("Goes straight to the admin team", style = MaterialTheme.typography.bodySmall,
                        color = EvColors.OnSurfaceVar)
                }
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(EvColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SupportAgent, null, tint = EvColors.Primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Success banner ────────────────────────────────────────────────
        if (state.submitted) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EvColors.PrimaryDim)
                        .border(1.dp, EvColors.Primary.copy(0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = EvColors.Primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Report sent", style = MaterialTheme.typography.titleSmall,
                            color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reference #${state.submittedRef ?: "—"} · the admin team has been notified. " +
                            "You'll get a notification when it's resolved.",
                        style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurfaceVar
                    )
                    Spacer(Modifier.height(10.dp))
                    EvOutlinedButton(
                        text = "Report something else",
                        onClick = {
                            viewModel.acknowledgeSubmit()
                            subject = ""; description = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Form ──────────────────────────────────────────────────────────
        item {
            EvSectionHeader("What's wrong?", Modifier.padding(horizontal = 16.dp),
                icon = Icons.Filled.ReportProblem)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(IssueCategories.ALL) { option ->
                    EvFilterChip(option, category == option, { category = option })
                }
            }
            Spacer(Modifier.height(14.dp))

            Column(Modifier.padding(horizontal = 16.dp)) {
                stationName?.let {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EvColors.Surface)
                            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.EvStation, null, tint = EvColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ABOUT THIS STATION", style = MaterialTheme.typography.labelSmall,
                                color = EvColors.OnSurfaceVar, letterSpacing = 0.8.sp)
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = EvColors.OnSurface)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                EvTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = "Short title, e.g. \"Charger 2 not starting\"",
                    label = "Subject",
                    leadingIcon = Icons.Filled.Title
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text("What happened? Include the time and any error you saw.",
                            color = EvColors.OnSurfaceVar.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium)
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EvColors.Primary,
                        unfocusedBorderColor = EvColors.OutlineVariant,
                        focusedContainerColor = EvColors.InputBackground,
                        unfocusedContainerColor = EvColors.InputBackground,
                        cursorColor = EvColors.Primary,
                        focusedTextColor = EvColors.OnSurface,
                        unfocusedTextColor = EvColors.OnSurface
                    )
                )

                state.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = EvColors.Error, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = EvColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(16.dp))
                EvPrimaryButton(
                    text = if (state.submitting) "Sending…" else "Send report to admin",
                    onClick = { viewModel.submit(category, subject, description, stationId) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.submitting && subject.isNotBlank() && description.isNotBlank(),
                    icon = Icons.Filled.Send
                )
            }
        }

        // ── My reports ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(24.dp))
            EvSectionHeader("Your reports", Modifier.padding(horizontal = 16.dp),
                icon = Icons.Filled.History)
            Spacer(Modifier.height(8.dp))
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = EvColors.Primary)
                }
            } else if (state.myIssues.isEmpty()) {
                Text(
                    "Nothing reported yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EvColors.OnSurfaceVar,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        items(state.myIssues, key = { it.id }) { issue ->
            MyIssueCard(issue, Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun MyIssueCard(issue: IssueDto, modifier: Modifier = Modifier) {
    val (accent, label) = when (issue.status) {
        "OPEN"        -> EvColors.Warning to "Open"
        "IN_PROGRESS" -> EvColors.Secondary to "In progress"
        "RESOLVED"    -> EvColors.Primary to "Resolved"
        else          -> EvColors.OnSurfaceVar to "Rejected"
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EvColors.Surface)
            .border(1.dp, EvColors.SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(issue.subject, style = MaterialTheme.typography.titleSmall,
                    color = EvColors.OnBackground, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    listOfNotNull(issue.category, issue.stationName, issue.createdAt?.take(10))
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = EvColors.OnSurfaceVar, maxLines = 1
                )
            }
            StatusPill(label, isActive = issue.status == "RESOLVED", showDot = false)
        }
        Spacer(Modifier.height(6.dp))
        Text(issue.description, style = MaterialTheme.typography.bodySmall,
            color = EvColors.OnSurface, maxLines = 3)
        issue.resolutionNote?.let {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.AdminPanelSettings, null, tint = accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("ADMIN REPLY", style = MaterialTheme.typography.labelSmall,
                        color = accent, letterSpacing = 0.7.sp)
                    Text(it, style = MaterialTheme.typography.bodySmall, color = EvColors.OnSurface)
                }
            }
        }
    }
}
