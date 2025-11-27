package com.ansh.awsnotifier.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ansh.awsnotifier.ui.onboarding.pages.IAMPolicyPage
import com.ansh.awsnotifier.ui.onboarding.pages.IAMSetupPage
import com.ansh.awsnotifier.ui.onboarding.pages.IamCredentialsInputScreen
import com.ansh.awsnotifier.ui.onboarding.pages.PlatformArnPage
import com.ansh.awsnotifier.ui.onboarding.pages.WelcomePage
import kotlinx.coroutines.launch

enum class OnboardingPage {
    Welcome,
    IAMSetup,
    IAMPolicy,
    IAMCredentials,
    PlatformConfig
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.IAMSetup,
        OnboardingPage.IAMPolicy,
        OnboardingPage.IAMCredentials,
        OnboardingPage.PlatformConfig
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            OneUiOnboardingBottomBar(
                currentPage = pagerState.currentPage,
                totalPages = pages.size,
                onNext = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                onBack = {
                    if (pagerState.currentPage > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onSkip = {
                    // You *could* jump directly to the last page here, but right now
                    // skip = "finish onboarding now"
                    onFinish()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->

                val pageOffset =
                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                val alpha = 1f - kotlin.math.abs(pageOffset * 0.35f)
                val scale = 1f - kotlin.math.abs(pageOffset * 0.12f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                ) {
                    when (pages[pageIndex]) {
                        OnboardingPage.Welcome -> WelcomePage()
                        OnboardingPage.IAMSetup -> IAMSetupPage()
                        OnboardingPage.IAMPolicy -> IAMPolicyPage()

                        OnboardingPage.IAMCredentials -> IamCredentialsInputScreen(
                            // when creds are validated, move to PlatformConfig page
                            onSuccess = {
                                val nextIndex = pages.indexOf(OnboardingPage.PlatformConfig)
                                scope.launch {
                                    pagerState.animateScrollToPage(nextIndex)
                                }
                            }
                        )

                        OnboardingPage.PlatformConfig -> PlatformArnPage(
                            onDone = onFinish
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OneUiOnboardingBottomBar(
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    val isSelected = index == currentPage
                    val width by animateDpAsState(if (isSelected) 16.dp else 6.dp)
                    val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f)

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                            )
                    )
                }
            }

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(onClick = onBack) { Text("Back") }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                if (currentPage < totalPages - 1) {
                    TextButton(onClick = onSkip) { Text("Skip") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNext,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.widthIn(min = 120.dp)
                    ) { Text("Next") }
                } else {
                    Button(
                        onClick = onNext,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) { Text("Finish") }
                }
            }
        }
    }
}
