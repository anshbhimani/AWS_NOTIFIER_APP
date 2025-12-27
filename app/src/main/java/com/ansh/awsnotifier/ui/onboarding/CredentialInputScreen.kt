package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CredentialInputScreen(
    accessKey: String,
    secretKey: String,
    onAccessChange: (String) -> Unit,
    onSecretChange: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("AWS Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = accessKey,
            onValueChange = onAccessChange,
            label = { Text("AWS Access Key ID") },
            singleLine = true,
            isError = accessKey.isNotEmpty() && !accessKey.matches(Regex("^AKIA[0-9A-Z]{16}$")),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = secretKey,
            onValueChange = onSecretChange,
            label = { Text("AWS Secret Access Key") },
            singleLine = true,
            isError = secretKey.isNotEmpty() && secretKey.length < 40,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Credentials are stored securely on this device only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Warning: Use IAM credentials with restricted permissions.",
            color = MaterialTheme.colorScheme.error
        )
    }
}
