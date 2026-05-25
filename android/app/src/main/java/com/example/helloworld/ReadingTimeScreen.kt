package com.example.helloworld

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val READING_TIME_TAG = "reading_time_text"

@Composable
fun ReadingTimeScreen(
    articleText: String = "",
    wordsPerMinute: Int = ReadingTimeEstimator.DEFAULT_WORDS_PER_MINUTE,
    longLabel: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ReadingTimeEstimator.format(articleText, wordsPerMinute, longLabel),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(READING_TIME_TAG),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadingTimeScreenPreview() {
    ReadingTimeScreen(
        articleText = "Cabinet reshuffle: key ministers moved in late-night announcement. " +
            "Analysts expect markets to react when trading opens.",
    )
}
