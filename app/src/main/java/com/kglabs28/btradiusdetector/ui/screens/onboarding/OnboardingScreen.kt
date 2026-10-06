package com.kglabs28.btradiusdetector.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kglabs28.btradiusdetector.ui.components.HowToFindCard
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.ScrimBlack
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * First-launch overlay. Stateless, no ViewModel — there is no state to own,
 * just a one-shot callback. Reuses [HowToFindCard] so onboarding and in-app
 * help stay identical.
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimBlack.copy(alpha = Dimens.alphaScrim)),
        contentAlignment = Alignment.Center
    ) {
        HowToFindCard(
            steps = Strings.howToSteps,
            onGotIt = onComplete,
            showClose = false,
            modifier = Modifier
                .fillMaxWidth(Dimens.cardWidthFraction)
                .padding(Dimens.spacingMd.scaled())
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun OnboardingPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        OnboardingScreen(onComplete = {})
    }
}
