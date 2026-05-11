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

const val HELLO_TEXT_TAG = "hello_text"

@Composable
fun HelloScreen(name: String = Greeting.DEFAULT_NAME) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = Greeting.greet(name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(HELLO_TEXT_TAG)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HelloScreenPreview() {
    HelloScreen()
}
