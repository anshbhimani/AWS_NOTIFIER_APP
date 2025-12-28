package com.ansh.awsnotifier.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ansh.awsnotifier.R
import com.ansh.awsnotifier.security.BiometricHelper

class BiometricAuthActivity : AppCompatActivity() {

    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Reuse splash layout

        biometricHelper = BiometricHelper(this)

        startAuthentication()
    }

    private fun startAuthentication() {
        biometricHelper.authenticate(
            onSuccess = {
                navigateToMain()
            },
            onError = { errorMsg ->
                Toast.makeText(this, "Authentication required: $errorMsg", Toast.LENGTH_SHORT)
                    .show()
                // If the user cancels the prompt or too many attempts, we close the app.
                finishAffinity()
            },
            onFailed = {
                // Biometric recognized but rejected (wrong finger). The prompt usually stays open,
                // so we might not need to do anything here unless we want to count attempts.
            }
        )
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear flags so user can't go back to Auth screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
