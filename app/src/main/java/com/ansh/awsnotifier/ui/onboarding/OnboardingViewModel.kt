package com.ansh.awsnotifier.ui.onboarding

import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {

    var accessKey: String = ""
        private set

    var secretKey: String = ""
        private set

    var selectedRegion: String = "us-east-1"
        private set

    var platformArn: String = ""
        private set
}
