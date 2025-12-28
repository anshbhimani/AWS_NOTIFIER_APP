package com.ansh.awsnotifier.ui.onboarding.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansh.awsnotifier.R
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
            text = stringResource(id = R.string.plat_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.plat_desc),
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
            label = { Text(stringResource(id = R.string.plat_label_arn)) },
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
                    error = context.getString(R.string.plat_err_required)
                    return@Button
                }
                if (!arn.startsWith("arn:aws:sns:")) {
                    error = context.getString(R.string.plat_err_invalid)
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
            Text(if (isSaving) stringResource(id = R.string.btn_saving) else stringResource(id = R.string.btn_save_finish))
        }
    }
}
