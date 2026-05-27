package com.example.helloworld

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val HELLO_TEXT_TAG = "hello_text"
const val HELLO_TOAST_BUTTON_TAG = "hello_toast_button"

@Composable
fun HelloScreen(
    name: String = Greeting.DEFAULT_NAME,
    greeting: String = Greeting.DEFAULT_GREETING,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = Greeting.greet(name, greeting),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag(HELLO_TEXT_TAG)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Toast.makeText(context, "Hi from Compose!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag(HELLO_TOAST_BUTTON_TAG)
            ) {
                Text("Show message")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelloScreenPreview() {
    HelloScreen()
}
