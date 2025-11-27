package com.ansh.awsnotifier.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.onboarding.pages.RegionArnSetupScreen

class RegionArnSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RegionArnSetupScreen(
                onSave = { region, arn ->
                    UserSession.savePlatformArnForRegion(this, region, arn)
                    finish()
                },
                onCancel = { finish() }
            )
        }
    }
}
