package com.ansh.awsnotifier.ui.onboarding.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansh.awsnotifier.session.UserSession

@Composable
fun PlatformArnPage(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var arn by remember {
        mutableStateOf(UserSession.getPlatformApplicationArn(context) ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "SNS Platform Application",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Paste the SNS Platform Application ARN for your Firebase/FCM app.\n" +
                    "You can find this in the AWS SNS console under Mobile push → Platform applications.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = arn,
            onValueChange = {
                arn = it
                error = null
            },
            label = { Text("Platform Application ARN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (arn.isBlank()) {
                    error = "Platform Application ARN is required"
                    return@Button
                }
                if (!arn.startsWith("arn:aws:sns:")) {
                    error = "This doesn't look like a valid SNS ARN"
                    return@Button
                }

                isSaving = true
                // store locally
                UserSession.savePlatformApplicationArn(context, arn)
                isSaving = false
                onDone()
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Saving..." else "Save & Finish")
        }
    }
}
