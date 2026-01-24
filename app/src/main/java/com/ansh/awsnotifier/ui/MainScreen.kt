package com.ansh.awsnotifier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ansh.awsnotifier.ui.components.TopicDetailSheet
import com.ansh.awsnotifier.ui.components.TopicListItem
import kotlinx.coroutines.launch

enum class TopicFilter {
    All, Subscribed, Unsubscribed
}

data class TopicUiModel(
    val arn: String,
    val name: String,
    val region: String,
    val isSubscribed: Boolean,
    val subscriptionArn: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    topics: List<TopicUiModel>,
    regions: List<String>,
    currentRegion: String,
    isLoading: Boolean,
    onRegionSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddTopic: () -> Unit,
    onCreateTopic: () -> Unit,
    onSettingsClick: () -> Unit,
    onShowFcmToken: () -> Unit,
    onSubscribe: (String) -> Unit,
    onUnsubscribe: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onCopyArn: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(TopicFilter.All) }

    var showRegionSheet by remember { mutableStateOf(false) }
    var selectedTopicForSheet by remember { mutableStateOf<TopicUiModel?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val filteredTopics by remember(topics, searchQuery, activeFilter) {
        derivedStateOf {
            topics.filter { topic ->
                val matchesSearch = searchQuery.isBlank() ||
                        topic.name.contains(searchQuery, ignoreCase = true) ||
                        topic.arn.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (activeFilter) {
                    TopicFilter.All -> true
                    TopicFilter.Subscribed -> topic.isSubscribed
                    TopicFilter.Unsubscribed -> !topic.isSubscribed
                }

                matchesSearch && matchesFilter
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ) {
                // 1. Dashboard Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Region Selector (Pill)
                    Surface(
                        onClick = { showRegionSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentRegion,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Settings Icon
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2. Search Field (Minimal)
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search topics...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // 3. Filter Chips (Horizontal Scroll)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = activeFilter == TopicFilter.All,
                            onClick = { activeFilter = TopicFilter.All },
                            label = { Text("All Topics") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = activeFilter == TopicFilter.Subscribed,
                            onClick = { activeFilter = TopicFilter.Subscribed },
                            label = { Text("Subscribed") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    item {
                        // Action Chip for "Create New" acting as a shortcut
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable(onClick = onCreateTopic)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTopic,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Topic"
                ) // User asked for Create, usually FAB is create
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredTopics.isEmpty()) {
                EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    message = if (searchQuery.isNotEmpty()) "No matches found" else "No topics in this region"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTopics, key = { it.arn }) { topic ->
                        TopicListItem(
                            name = topic.name,
                            region = topic.region,
                            isSubscribed = topic.isSubscribed,
                            onClick = { selectedTopicForSheet = topic },
                            onMenuClick = { selectedTopicForSheet = topic }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp), // Indent divider
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // --- Region Sheet ---
    if (showRegionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRegionSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Select Region",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp)
                )
                LazyColumn {
                    items(regions) { region ->
                        val isSelected = region == currentRegion
                        ListItem(
                            headlineContent = {
                                Text(
                                    region,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            modifier = Modifier.clickable {
                                onRegionSelected(region)
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showRegionSheet = false }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    // --- Topic Details Sheet ---
    selectedTopicForSheet?.let { topic ->
        ModalBottomSheet(
            onDismissRequest = { selectedTopicForSheet = null },
            sheetState = detailSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { WindowInsets.navigationBars } // Changed windowInsets to contentWindowInsets and made it a lambda
        ) {
            TopicDetailSheet(
                topic = topic,
                onCopyArn = {
                    onCopyArn(topic.arn)
                    // Don't close sheet, user might want to do more
                },
                onSubscribe = {
                    onSubscribe(topic.arn)
                    scope.launch { detailSheetState.hide() }
                        .invokeOnCompletion { selectedTopicForSheet = null }
                },
                onUnsubscribe = {
                    onUnsubscribe(topic.subscriptionArn ?: "")
                    scope.launch { detailSheetState.hide() }
                        .invokeOnCompletion { selectedTopicForSheet = null }
                },
                onDelete = {
                    onDelete(topic.arn)
                    scope.launch { detailSheetState.hide() }
                        .invokeOnCompletion { selectedTopicForSheet = null }
                },
                onSendMessage = {
                    onSendMessage(topic.arn)
                    scope.launch { detailSheetState.hide() }
                        .invokeOnCompletion { selectedTopicForSheet = null }
                }
            )
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, message: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📭",
            fontSize = 40.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}