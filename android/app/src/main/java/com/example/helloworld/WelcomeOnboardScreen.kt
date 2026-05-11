package com.example.helloworld

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview

const val WELCOME_TEXT_TAG = "welcome_text"

/**
 * Compose UI for [WelcomeOnboard]. Intentionally shipped without a Compose
 * UI test so the AI QA agent has something concrete to flag — pair it with
 * [WelcomeOnboard] and you have both a unit-test gap and a UI-test gap in
 * the same PR.
 */
@Composable
fun WelcomeOnboardScreen(name: String = Greeting.DEFAULT_NAME) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = WelcomeOnboard.welcome(name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(WELCOME_TEXT_TAG)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeOnboardScreenPreview() {
    WelcomeOnboardScreen()
}
