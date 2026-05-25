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

const val ARTICLE_TEASER_TAG = "article_teaser_text"

@Composable
fun ArticleTeaserScreen(
    teaser: String = "",
    maxHeadlineLength: Int = 80,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ArticleTeaserFormatter.headline(teaser, maxHeadlineLength),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(ARTICLE_TEASER_TAG),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleTeaserScreenPreview() {
    ArticleTeaserScreen(
        teaser = "Cabinet reshuffle: key ministers moved in late-night announcement",
    )
}
