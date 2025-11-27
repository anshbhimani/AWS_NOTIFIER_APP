package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ansh.awsnotifier.aws.CredentialValidator
import com.ansh.awsnotifier.session.UserSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsInputScreen(onSuccess: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("us-east-1") }
    var showSecret by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var regionExpanded by remember { mutableStateOf(false) }

    val regions = listOf(
        "us-east-1" to "US East (N. Virginia)",
        "us-east-2" to "US East (Ohio)",
        "us-west-1" to "US West (N. California)",
        "us-west-2" to "US West (Oregon)",
        "eu-west-1" to "Europe (Ireland)",
        "eu-west-2" to "Europe (London)",
        "eu-central-1" to "Europe (Frankfurt)",
        "ap-south-1" to "Asia Pacific (Mumbai)",
        "ap-southeast-1" to "Asia Pacific (Singapore)",
        "ap-southeast-2" to "Asia Pacific (Sydney)",
        "ap-northeast-1" to "Asia Pacific (Tokyo)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Enter Credentials",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the access keys from your IAM user",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Security notice
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your credentials are encrypted and stored securely on this device only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Access Key Input
        OutlinedTextField(
            value = accessKey,
            onValueChange = { accessKey = it.trim(); errorMessage = null },
            label = { Text("Access Key ID") },
            placeholder = { Text("AKIA...") },
            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Secret Key Input
        OutlinedTextField(
            value = secretKey,
            onValueChange = { secretKey = it.trim(); errorMessage = null },
            label = { Text("Secret Access Key") },
            placeholder = { Text("Your secret key") },
            leadingIcon = { Icon(Icons.Outlined.Password, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showSecret = !showSecret }) {
                    Icon(
                        if (showSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showSecret) "Hide" else "Show"
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Region Selector
        ExposedDropdownMenuBox(
            expanded = regionExpanded,
            onExpandedChange = { regionExpanded = it }
        ) {
            OutlinedTextField(
                value = regions.find { it.first == selectedRegion }?.second ?: selectedRegion,
                onValueChange = {},
                readOnly = true,
                label = { Text("Default Region") },
                leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = regionExpanded,
                onDismissRequest = { regionExpanded = false }
            ) {
                regions.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                Text(code, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            selectedRegion = code
                            regionExpanded = false
                        }
                    )
                }
            }
        }

        // Error message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Validate and Save button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null

                    val result = CredentialValidator.validateAndSave(
                        context = ctx,
                        accessKey = accessKey,
                        secretKey = secretKey,
                        region = selectedRegion
                    )

                    isLoading = false

                    result.fold(
                        onSuccess = { onSuccess() },
                        onFailure = { e -> errorMessage = e.message ?: "Validation failed" }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = accessKey.isNotBlank() && secretKey.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Validating...")
            } else {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validate & Connect")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help text
        Text(
            text = "We'll verify your credentials by making a test API call to AWS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}