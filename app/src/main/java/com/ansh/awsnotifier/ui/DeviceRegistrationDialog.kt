package com.ansh.awsnotifier.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.ansh.awsnotifier.session.UserSession
import kotlinx.coroutines.launch

/**
 * Dialog to register device for push notifications in a region.
 * Currently uses Platform Application ARNs from BuildConfig via MultiRegionSnsManager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceRegistrationDialog(
    region: String,
    onDismiss: () -> Unit,
    onSuccess: (endpointArn: String) -> Unit,
    app: App
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var platformAppArn by remember { mutableStateOf("") } // kept for UI; not used in API call
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // ✅ FIX: no region argument here
    val existingEndpoint = UserSession.getDeviceEndpointArn(ctx)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register Device")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                if (existingEndpoint != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Already Registered",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Device is registered (endpoint ARN stored).",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You can subscribe to topics now, or re-register to update the endpoint.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "To receive notifications, you need to register this device with your SNS Platform Application.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "How this works:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "The app uses the Platform Application ARN you configured in local.properties for region $region.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // You can keep or remove this field; it is no longer used in logic.
                    OutlinedTextField(
                        value = platformAppArn,
                        onValueChange = { platformAppArn = it; error = null },
                        label = { Text("Platform Application ARN (optional)") },
                        placeholder = { Text("arn:aws:sns:$region:...") },
                        singleLine = false,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        supportingText = if (error != null) {
                            { Text(error!!, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }
            }
        },
        confirmButton = {
            if (existingEndpoint != null) {
                TextButton(onClick = { onSuccess(existingEndpoint) }) {
                    Text("Continue")
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            try {
                                val fcmToken = UserSession.getFcmToken(ctx)
                                if (fcmToken == null) {
                                    error = "FCM token not available yet. Please try again in a moment."
                                    isLoading = false
                                    return@launch
                                }

                                // Use DeviceRegistrar for consistent logic
                                com.ansh.awsnotifier.aws.DeviceRegistrar.registerForRegion(
                                    ctx,
                                    region
                                )

                                val endpointArn = UserSession.getDeviceEndpointArn(ctx)
                                if (endpointArn.isNullOrEmpty()) {
                                    error =
                                        "Registration failed. Ensure Platform ARN is set for $region."
                                    isLoading = false
                                    return@launch
                                }

                                onSuccess(endpointArn)
                            } catch (e: IllegalArgumentException) {
                                error = e.message ?: "Invalid configuration"
                            } catch (e: Exception) {
                                error = e.message ?: "Registration failed"
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Register")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Wrapper to require registration before showing content.
 */
@Composable
fun RequireDeviceRegistration(
    region: String,
    app: App,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    var showRegistrationDialog by remember { mutableStateOf(false) }

    // ✅ FIX: no region argument here
    var isRegistered by remember {
        mutableStateOf(UserSession.getDeviceEndpointArn(ctx) != null)
    }

    if (!isRegistered) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Device Not Registered",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Register your device in $region to subscribe to topics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showRegistrationDialog = true }) {
                    Text("Register Device")
                }
            }
        }

        if (showRegistrationDialog) {
            DeviceRegistrationDialog(
                region = region,
                onDismiss = { showRegistrationDialog = false },
                onSuccess = {
                    isRegistered = true
                    showRegistrationDialog = false
                },
                app = app
            )
        }
    } else {
        content()
    }
}
