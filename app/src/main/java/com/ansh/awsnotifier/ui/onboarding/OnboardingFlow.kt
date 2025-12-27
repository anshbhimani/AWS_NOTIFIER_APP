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
import com.ansh.awsnotifier.ui.onboarding.pages.WelcomePage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onFinish: (accessKey: String, secretKey: String) -> Unit
) {
    val pages = listOf("welcome", "iam", "credentials")
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    var accessKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            OnboardingNavigationBar(
                currentPage = pager.currentPage,
                totalPages = pages.size,
                onNext = {
                    if (pager.currentPage < pages.lastIndex) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else {
                        onFinish(accessKey.trim(), secretKey.trim())
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (pages[page]) {
                    "welcome" -> WelcomePage()
                    "iam" -> IAMSetupGuideScreen()
                    "credentials" -> CredentialInputScreen(
                        accessKey = accessKey,
                        secretKey = secretKey,
                        onAccessChange = { accessKey = it },
                        onSecretChange = { secretKey = it }
                    )
                }
            }
        }
    }
}
