package com.ansh.awsnotifier.ui.onboarding.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun IAMPolicyPage() {
    val policyJson = """
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SNSFullTopicManagement",
      "Effect": "Allow",
      "Action": [
        "sns:CreateTopic",
        "sns:DeleteTopic",
        "sns:SetTopicAttributes",
        "sns:GetTopicAttributes",
        "sns:ListTopics"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SNSSubscriptionManagement",
      "Effect": "Allow",
      "Action": [
        "sns:Subscribe",
        "sns:Unsubscribe",
        "sns:GetSubscriptionAttributes",
        "sns:ListSubscriptions",
        "sns:ListSubscriptionsByTopic"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SNSPublishing",
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SNSMobilePushEndpoints",
      "Effect": "Allow",
      "Action": [
        "sns:CreatePlatformEndpoint",
        "sns:SetEndpointAttributes",
        "sns:GetEndpointAttributes",
        "sns:DeleteEndpoint"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SNSPublicTopicAccessControls",
      "Effect": "Allow",
      "Action": [
        "sns:AddPermission",
        "sns:RemovePermission"
      ],
      "Resource": "*"
    },
    {
      "Sid": "StsIdentity",
      "Effect": "Allow",
      "Action": [
        "sts:GetCallerIdentity"
      ],
      "Resource": "*"
    }
  ]
}
    """.trimIndent()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("IAM Policy", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Text(
            "To grant the necessary permissions, create a new IAM policy in your AWS account. Go to IAM > Policies > Create policy, switch to the JSON tab, and paste the content below.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        SelectionContainer {
            Text(
                text = policyJson,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}
