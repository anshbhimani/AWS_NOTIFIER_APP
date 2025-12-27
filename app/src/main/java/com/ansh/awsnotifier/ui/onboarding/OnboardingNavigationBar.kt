package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingNavigationBar(
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // 👈 reduced padding
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            if (currentPage > 0) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Back")
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }

            Button(
                onClick = onNext,
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(if (currentPage == totalPages - 1) "Finish" else "Next")
            }
        }
    }
}
