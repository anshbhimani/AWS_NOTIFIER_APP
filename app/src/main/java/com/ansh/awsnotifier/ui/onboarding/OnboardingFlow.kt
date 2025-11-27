package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onFinish: (accessKey: String, secretKey: String, region: String, platformArn: String) -> Unit
) {
    val pages = remember {
        listOf(
            "welcome",
            "iam_setup",
            "policy",
            "credentials"
        )
    }

    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    // NEW STATE FOR REGION + ARN
    val regions = listOf(
        "us-east-1", "us-east-2",
        "us-west-1", "us-west-2",
        "ap-south-1", "ap-southeast-1",
        "ap-southeast-2", "ap-northeast-1",
        "eu-west-1", "eu-central-1",
        "sa-east-1"
    )

    var selectedRegion by remember { mutableStateOf(regions.first()) }
    var platformArn by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            OnboardingNavigationBar(
                currentPage = pager.currentPage,
                totalPages = pages.size,
                onNext = {
                    if (pager.currentPage < pages.lastIndex) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else {
                        onFinish(accessKey, secretKey, selectedRegion, platformArn)
                    }
                },
                onBack = {
                    if (pager.currentPage > 0) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                    }
                }
            )
        }
    ) { innerPadding ->

        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = pager
        ) { page ->

            when (pages[page]) {
                "welcome" -> WelcomeScreen()
                "iam_setup" -> IAMSetupGuideScreen()
                "policy" -> IAMPolicyScreen()

                "credentials" -> CredentialInputScreen(
                    accessKey = accessKey,
                    secretKey = secretKey,

                    selectedRegion = selectedRegion,
                    platformArn = platformArn,
                    regions = regions,

                    onAccessChange = { accessKey = it },
                    onSecretChange = { secretKey = it },

                    onRegionChange = { selectedRegion = it },
                    onPlatformArnChange = { platformArn = it },
                    onContinue = {
                        onFinish(accessKey, secretKey, selectedRegion, platformArn)
                    }

                )
            }
        }
    }
}
