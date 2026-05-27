package com.example.helloworld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val SEARCH_INPUT_LABEL_TAG = "search_input_label"
const val SEARCH_SANITIZED_TAG = "search_sanitized_text"
const val SEARCH_TRUNCATED_HELPER_TAG = "search_truncated_helper"

@Composable
fun SearchScreen(
    rawQuery: String = "",
    maxLen: Int = SearchQuerySanitizer.DEFAULT_MAX_LEN,
) {
    val sanitized = SearchQuerySanitizer.sanitizeWithMetadata(rawQuery, maxLen)
    val display = if (sanitized.text.isEmpty()) "Start typing to search…" else sanitized.text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.testTag(SEARCH_INPUT_LABEL_TAG),
        )
        Text(
            text = display,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(SEARCH_SANITIZED_TAG),
        )

        if (sanitized.wasTruncated && sanitized.text.isNotEmpty()) {
            Text(
                text = "Showing first ${sanitized.text.length} characters (max $maxLen).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(SEARCH_TRUNCATED_HELPER_TAG),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    SearchScreen(rawQuery = "  Breaking!! NEWS,, today??  ")
}
