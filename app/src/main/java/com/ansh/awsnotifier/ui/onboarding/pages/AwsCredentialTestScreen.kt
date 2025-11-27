package com.ansh.awsnotifier.ui.onboarding.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.sdk.kotlin.services.sns.model.ListTopicsRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwsCredentialTestScreen() {

    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    val regionList = listOf(
        "us-east-1",
        "us-east-2",
        "us-west-1",
        "us-west-2",
        "ap-south-1",
        "ap-southeast-1",
        "ap-southeast-2",
        "ap-northeast-1",
        "eu-central-1",
        "eu-west-1",
        "sa-east-1"
    )

    var selectedRegion by remember { mutableStateOf("us-east-1") }
    var regionMenuExpanded by remember { mutableStateOf(false) }

    var output by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("AWS Credential Tester", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = accessKey,
            onValueChange = { accessKey = it },
            label = { Text("Access Key ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = secretKey,
            onValueChange = { secretKey = it },
            label = { Text("Secret Access Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // REGION DROPDOWN
        ExposedDropdownMenuBox(
            expanded = regionMenuExpanded,
            onExpandedChange = { regionMenuExpanded = !regionMenuExpanded }
        ) {
            OutlinedTextField(
                value = selectedRegion,
                onValueChange = {},
                label = { Text("Select Region") },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionMenuExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = regionMenuExpanded,
                onDismissRequest = { regionMenuExpanded = false }
            ) {
                regionList.forEach { region ->
                    DropdownMenuItem(
                        text = { Text(region) },
                        onClick = {
                            selectedRegion = region
                            regionMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                loading = true
                output = "Running AWS test…"

                scope.launch {
                    output = withContext(Dispatchers.IO) {
                        try {
                            val provider = StaticCredentialsProvider(
                                Credentials(
                                    accessKeyId = accessKey.trim(),
                                    secretAccessKey = secretKey.trim()
                                )
                            )

                            val sb = StringBuilder()
                            sb.appendLine("=== AWS TEST START ===")

                            // -------- STS TEST ----------
                            try {
                                sb.appendLine("\n1️⃣ STS GetCallerIdentity…")

                                StsClient {
                                    region = "us-east-1"
                                    credentialsProvider = provider
                                }.use { sts ->
                                    val resp = sts.getCallerIdentity(GetCallerIdentityRequest {})
                                    sb.appendLine("✔ STS OK")
                                    sb.appendLine("Account: ${resp.account}")
                                    sb.appendLine("ARN: ${resp.arn}")
                                    sb.appendLine("UserId: ${resp.userId}")
                                }
                            } catch (e: Exception) {
                                sb.appendLine("❌ STS FAILED:")
                                sb.appendLine(e.stackTraceToString())
                                return@withContext sb.toString()
                            }

                            // -------- SNS TEST ----------
                            try {
                                sb.appendLine("\n2️⃣ SNS ListTopics in $selectedRegion…")

                                SnsClient {
                                    region = selectedRegion
                                    credentialsProvider = provider
                                }.use { sns ->
                                    sns.listTopics(ListTopicsRequest {})
                                }

                                sb.appendLine("✔ SNS OK")
                            } catch (e: Exception) {
                                sb.appendLine("❌ SNS FAILED:")
                                sb.appendLine(e.stackTraceToString())
                                return@withContext sb.toString()
                            }

                            sb.appendLine("\n=== ALL TESTS PASSED ===")
                            sb.toString()

                        } catch (e: Exception) {
                            "❌ Unexpected error:\n${e.stackTraceToString()}"
                        }
                    }

                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            Text(if (loading) "Testing…" else "Run AWS Test")
        }

        Spacer(Modifier.height(16.dp))

        // COPY BUTTON
        Button(
            onClick = {
                val clipboard = ctx.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("aws_test_output", output)
                clipboard.setPrimaryClip(clip)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy Output")
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = output,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
