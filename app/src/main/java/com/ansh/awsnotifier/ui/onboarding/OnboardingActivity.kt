package com.ansh.awsnotifier.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.aws.DeviceRegistrar
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

class OnboardingActivity : ComponentActivity() {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OnboardingFlow { accessKey, secretKey, region, platformArn ->

                // 1️⃣ Save credentials
                UserSession.saveCredentials(this, accessKey, secretKey)

                // 2️⃣ Save region
                UserSession.saveCurrentRegion(this, region)

                // 3️⃣ Save Platform Application ARN for this region
                UserSession.savePlatformApplicationArn(this, platformArn)

                // 4️⃣ Mark onboarding as complete
                UserSession.setOnboardingComplete(this, true)

                // 5️⃣ Initialize AWS in App()
                val app = application as App

                val provider = StaticCredentialsProvider(
                    Credentials(
                        accessKeyId = accessKey,
                        secretAccessKey = secretKey
                    )
                )

                app.applyAwsCredentialsProvider(provider)

                // 6️⃣ Auto-register device in SNS
                ioScope.launch {
                    DeviceRegistrar.autoRegister(this@OnboardingActivity)
                }

                // 7️⃣ Navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
