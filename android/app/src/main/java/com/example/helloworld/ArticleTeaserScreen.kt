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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val ARTICLE_TEASER_TAG = "article_teaser_text"
const val ARTICLE_TEASER_TRUNCATED_TAG = "article_teaser_truncated_helper"

@Composable
fun ArticleTeaserScreen(
    teaser: String = "",
    maxHeadlineLength: Int = 80,
) {
    val headline = ArticleTeaserFormatter.headlineWithMetadata(teaser, maxHeadlineLength)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline.text,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ARTICLE_TEASER_TAG),
        )
        if (headline.wasTruncated) {
            Text(
                text = "Headline shortened to $maxHeadlineLength characters.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(ARTICLE_TEASER_TRUNCATED_TAG),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleTeaserScreenPreview() {
    ArticleTeaserScreen(
        teaser = "Cabinet reshuffle: key ministers moved in late-night announcement",
    )
}
