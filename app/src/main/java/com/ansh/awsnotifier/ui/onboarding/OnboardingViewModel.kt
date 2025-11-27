package com.ansh.awsnotifier.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ansh.awsnotifier.session.UserSession

class OnboardingViewModel : ViewModel() {

    var accessKey: String = ""
        private set

    var secretKey: String = ""
        private set

    var selectedRegion: String = "us-east-1"
        private set

    var platformArn: String = ""
        private set

    val regions = listOf(
        "us-east-1", "us-east-2",
        "us-west-1", "us-west-2",
        "ap-south-1", "ap-southeast-1",
        "ap-southeast-2", "ap-northeast-1",
        "eu-west-1", "eu-central-1",
        "sa-east-1"
    )

    fun updateAccessKey(s: String) { accessKey = s }
    fun updateSecretKey(s: String) { secretKey = s }

    fun updateRegion(context: Context, region: String) {
        selectedRegion = region
        platformArn = UserSession.getPlatformArn(context, region) ?: ""
    }

    fun updatePlatformArn(s: String) { platformArn = s }
}
