package com.ansh.awsnotifier.ui.onboarding.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionArnSetupScreen(
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    val regions = listOf(
        "us-east-1", "us-east-2",
        "us-west-1", "us-west-2",
        "ap-south-1", "ap-southeast-1",
        "ap-southeast-2", "ap-northeast-1",
        "eu-west-1", "eu-central-1",
        "sa-east-1"
    )

    var selectedRegion by remember { mutableStateOf(regions.first()) }
    var arn by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Setup Platform ARN") })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
        ) {
            Text("Select Region", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Box {
                OutlinedTextField(
                    value = selectedRegion,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    regions.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                selectedRegion = it
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = arn,
                onValueChange = { arn = it },
                label = { Text("Platform ARN") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(selectedRegion, arn) }
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCancel
            ) {
                Text("Cancel")
            }
        }
    }
}
