package com.ansh.awsnotifier.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EnterArnDialog(
    region: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun validate(input: String) {
        if (input.isBlank()) {
            isError = false
            errorMessage = ""
            return
        }
        if (!input.startsWith("arn:aws:sns:")) {
            isError = true
            errorMessage = "Must start with arn:aws:sns:"
        } else if (!input.contains(":$region:")) {
            isError = true
            errorMessage = "Must match region $region"
        } else {
            isError = false
            errorMessage = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Platform ARN for $region") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it.trim()
                        validate(text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Platform Application ARN") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(errorMessage) }
                    } else null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Example: arn:aws:sns:$region:123456789:app/GCM/MyApp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank() && !isError,
                onClick = { onConfirm(text) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
