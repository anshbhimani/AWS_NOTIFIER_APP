package com.ansh.awsnotifier.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.data.NotificationEntity
import com.ansh.awsnotifier.session.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    onBack: () -> Unit,
    app: App
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notifications by remember {
        app.notificationRepository.getAll()
    }.collectAsState(initial = emptyList())

    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTopicFilter by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    val retentionDays = remember { UserSession.getRetentionDays(context) }

    val filteredNotifications = remember(notifications, selectedTopicFilter) {
        selectedTopicFilter?.let { filter ->
            notifications.filter {
                it.topic.substringAfterLast(":")
                    .contains(filter, ignoreCase = true)
            }
        } ?: notifications
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, null)
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Auto-deleting notifications older than $retentionDays days",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notifications found")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications) {
                        NotificationItemCard(it)
                    }
                }
            }
        }
    }

    // 🔍 Filter Bottom Sheet
    if (showFilterSheet) {
        TopicFilterBottomSheet(
            topics = notifications.map { it.topic },
            selected = selectedTopicFilter,
            onSelect = { selectedTopicFilter = it },
            onDismiss = { showFilterSheet = false }
        )
    }

    // ❌ Clear Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Delete all saved notifications? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            app.notificationRepository.clearAll()
                            showClearDialog = false
                        }
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/* -------------------- FILTER BOTTOM SHEET -------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicFilterBottomSheet(
    topics: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    // ⏳ Debounce (300ms)
    LaunchedEffect(query) {
        delay(300)
        debouncedQuery = query
    }

    val filteredTopics = remember(debouncedQuery, topics) {
        topics
            .map { it.substringAfterLast(":") }
            .distinct()
            .filter {
                it.contains(debouncedQuery, ignoreCase = true)
            }
            .sorted()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Filter by Topic",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search topics…") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.FilterList, null)
                }
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                item {
                    FilterRow(
                        text = "All Topics",
                        selected = selected == null
                    ) {
                        onSelect(null)
                        onDismiss()
                    }
                }

                items(filteredTopics) { topic ->
                    FilterRow(
                        text = topic,
                        selected = selected == topic
                    ) {
                        onSelect(topic)
                        onDismiss()
                    }
                }
            }
        }
    }
}

/* -------------------- FILTER ROW -------------------- */

@Composable
fun FilterRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/* -------------------- NOTIFICATION CARD -------------------- */

@Composable
fun NotificationItemCard(notification: NotificationEntity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        notification.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(notification.message)

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Topic: ${notification.topic.substringAfterLast(":")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
