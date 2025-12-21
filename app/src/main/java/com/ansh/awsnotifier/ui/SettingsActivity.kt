package com.ansh.awsnotifier.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.ui.settings.SettingsScreen
import com.ansh.awsnotifier.ui.theme.AppTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            AppTheme {
                SettingsScreen(
                    onBack = { finish() },
                    app = app
                )
            }
        }
    }
}
