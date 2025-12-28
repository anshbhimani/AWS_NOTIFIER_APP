package com.ansh.awsnotifier.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ansh.awsnotifier.BuildConfig
import com.ansh.awsnotifier.aws.AwsIdentityManager
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.SplashActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    app: App
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var currentScreen by remember { mutableStateOf("settings") }

    if (currentScreen == "history") {
        NotificationHistoryScreen(
            onBack = { currentScreen = "settings" },
            app = app
        )
        return
    }

    var accountId by remember { mutableStateOf("Loading...") }
    var region by remember { mutableStateOf("Unknown") }
    var accessKey by remember { mutableStateOf("Unknown") }
    var fcmToken by remember { mutableStateOf("Unknown") }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var retentionDays by remember { mutableStateOf(UserSession.getRetentionDays(context)) }
    var showRetentionDialog by remember { mutableStateOf(false) }

    var biometricEnabled by remember { mutableStateOf(UserSession.isBiometricEnabled(context)) }

    LaunchedEffect(Unit) {
        // Load basic info
        region = UserSession.getCurrentRegion(context) ?: "Not Selected"
        val creds = UserSession.getCredentials(context)
        accessKey = if (creds != null) {
            val key = creds.first
            if (key.length > 4) "•••• " + key.takeLast(4) else key
        } else {
            "Not Set"
        }

        fcmToken = UserSession.getFcmToken(context)?.take(10)?.plus("...") ?: "Not Registered"

        // Load AWS Account ID if creds exist
        if (app.awsCredentialsProvider != null) {
            val identity = AwsIdentityManager(app.awsCredentialsProvider!!).getAccountDetails()
            accountId = identity ?: "Unavailable"
        } else {
            accountId = "Not Logged In"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Account Section
            SettingsSectionTitle("AWS Account")

            SettingsItem(
                icon = Icons.Default.Person,
                title = "Account ID",
                value = accountId
            )
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Access Key ID",
                value = accessKey
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Current Region",
                value = region
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Data Management
            SettingsSectionTitle("Data Management")

            Button(
                onClick = { currentScreen = "history" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Notification History")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showRetentionDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retention: $retentionDays Days")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security
            SettingsSectionTitle("Security")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newState = !biometricEnabled
                        biometricEnabled = newState
                        UserSession.setBiometricEnabled(context, newState)
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Lock",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Require authentication on startup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = {
                        biometricEnabled = it
                        UserSession.setBiometricEnabled(context, it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Credentials & Logout")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Retention Dialog
    if (showRetentionDialog) {
        val options = listOf(7, 15, 30, 60, 90)
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text("Auto-Delete History") },
            text = {
                Column {
                    Text("Delete notifications older than:")
                    Spacer(modifier = Modifier.height(8.dp))
                    options.forEach { days ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    retentionDays = days
                                    UserSession.saveRetentionDays(context, days)
                                    showRetentionDialog = false
                                }
                        ) {
                            RadioButton(
                                selected = (days == retentionDays),
                                onClick = {
                                    retentionDays = days
                                    UserSession.saveRetentionDays(context, days)
                                    showRetentionDialog = false
                                }
                            )
                            Text("$days Days")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("This will remove your AWS credentials and subscription data from this device. You will need to re-enter them to receive notifications.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        performLogout(context, app)
                    }
                ) { Text("Logout", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun performLogout(context: Context, app: App) {
    UserSession.clearAllData(context)
    app.clearCredentials()

    val intent = Intent(context, SplashActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    context.startActivity(intent)
}
