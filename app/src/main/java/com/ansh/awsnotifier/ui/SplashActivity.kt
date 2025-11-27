package com.ansh.awsnotifier.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ansh.awsnotifier.session.UserSession

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = if (!UserSession.isOnboardingComplete(this)) {
            Intent(this, com.ansh.awsnotifier.ui.onboarding.OnboardingActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}