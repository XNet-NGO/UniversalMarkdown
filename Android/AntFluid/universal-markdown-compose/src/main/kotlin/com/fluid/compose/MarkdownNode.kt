package com.fluid.compose

import androidx.compose.ui.text.AnnotatedString

sealed interface MarkdownNode {
    data class TextBlock(
        val text: AnnotatedString,
        val inlineLatex: Map<String, String> = emptyMap() // id -> formula
    ) : MarkdownNode
    data class CodeBlock(
        val code: String,
        val language: String?,
        val highlighted: AnnotatedString
    ) : MarkdownNode
    data class TableBlock(
        val headers: List<TableCell>,
        val rows: List<List<TableCell>>,
        val alignments: List<Alignment>
    ) : MarkdownNode {
        enum class Alignment { LEFT, CENTER, RIGHT }
    }
    data class TableCell(val text: AnnotatedString, val imageUrl: String? = null, val imageAlt: String? = null)
    data class ImageBlock(val url: String, val alt: String) : MarkdownNode
    data class LatexBlock(val formula: String) : MarkdownNode
    data object HorizontalRule : MarkdownNode
}
