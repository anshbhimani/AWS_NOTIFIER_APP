package com.ansh.awsnotifier.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.MainActivity
import com.ansh.awsnotifier.ui.theme.AppTheme

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AppTheme {
                OnboardingFlow(
                    onFinish = { accessKey, secretKey ->
                        UserSession.saveCredentials(
                            this,
                            accessKey,
                            secretKey
                        )
                        UserSession.setOnboardingComplete(this, true)

                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                )
            }
        }
    }
}
