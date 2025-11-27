package com.ansh.awsnotifier.ui.onboarding.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IAMSetupPage() {
    Column(
        Modifier.fillMaxSize().padding(22.dp)
    ) {
        Text("IAM Setup Guide", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        Text("Create an IAM user with programmatic access…")
    }
}
