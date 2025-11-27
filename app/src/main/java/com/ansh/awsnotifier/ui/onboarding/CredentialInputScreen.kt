package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CredentialInputScreen(
    accessKey: String,
    secretKey: String,
    selectedRegion: String,
    platformArn: String,
    regions: List<String>,
    onAccessChange: (String) -> Unit,
    onSecretChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onPlatformArnChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("AWS Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // --- ACCESS KEY ---
        OutlinedTextField(
            value = accessKey,
            onValueChange = onAccessChange,
            label = { Text("AWS Access Key ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // --- SECRET KEY ---
        OutlinedTextField(
            value = secretKey,
            onValueChange = onSecretChange,
            label = { Text("AWS Secret Access Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // --- REGION DROPDOWN ---
        Text("Select Region", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }

        Box {
            OutlinedTextField(
                value = selectedRegion,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text("Region") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                regions.forEach { region ->
                    DropdownMenuItem(
                        text = { Text(region) },
                        onClick = {
                            onRegionChange(region)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- PLATFORM ARN ---
        OutlinedTextField(
            value = platformArn,
            onValueChange = onPlatformArnChange,
            label = { Text("Platform Application ARN for $selectedRegion") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinue
        ) {
            Text("Continue")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Warning: Use IAM credentials with restricted permissions.",
            color = MaterialTheme.colorScheme.error
        )
    }
}
