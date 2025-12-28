package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ansh.awsnotifier.aws.CredentialValidator
import com.ansh.awsnotifier.ui.onboarding.pages.IAMPolicyPage
import com.ansh.awsnotifier.ui.onboarding.pages.WelcomePage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onFinish: () -> Unit
) {
    val pages = listOf("welcome", "iam setup guide", "iam user", "credentials")
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("us-east-1") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            OnboardingNavigationBar(
                currentPage = pager.currentPage,
                totalPages = pages.size,
                isLoading = isLoading,
                onNext = {
                    if (pager.currentPage < pages.lastIndex) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else {
                        // Finish / Validate
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = CredentialValidator.validateAndSave(
                                context = context,
                                accessKey = accessKey,
                                secretKey = secretKey,
                                region = selectedRegion
                            )
                            isLoading = false
                            result.fold(
                                onSuccess = { onFinish() },
                                onFailure = { e -> errorMessage = e.message }
                            )
                        }
                    }
                },
                onBack = {
                    if (pager.currentPage > 0) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (pages[page]) {
                    "welcome" -> WelcomePage()
                    "iam setup guide" -> IAMPolicyPage()
                    "iam user" -> IAMSetupGuideScreen()
                    "credentials" -> CredentialInputScreen(
                        accessKey = accessKey,
                        secretKey = secretKey,
                        selectedRegion = selectedRegion,
                        onAccessChange = {
                            accessKey = it
                            errorMessage = null
                        },
                        onSecretChange = {
                            secretKey = it
                            errorMessage = null
                        },
                        onRegionChange = { selectedRegion = it },
                        error = errorMessage
                    )
                }
            }
        }
    }
}
