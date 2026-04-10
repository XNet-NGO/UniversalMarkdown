package com.fluid.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.noties.markwon.syntax.BaseCodeGrammar
import io.noties.markwon.syntax.FixPrism4j
import io.noties.prism4j.Prism4j

/**
 * Tokenizes code with Prism4j and produces an AnnotatedString with syntax colors.
 * Reuses the existing Prism4j grammars from markwon-syntax-highlight.
 */
object ComposeSyntaxHighlighter {

    private val prism4j = FixPrism4j(BaseCodeGrammar())

    // Darkula-inspired color map
    private val colorMap = mapOf(
        "comment" to 0xFF808080, "prolog" to 0xFF808080, "cdata" to 0xFF808080,
        "delimiter" to 0xFFcc7832, "boolean" to 0xFFcc7832, "keyword" to 0xFFcc7832,
        "selector" to 0xFFcc7832, "important" to 0xFFcc7832, "atrule" to 0xFFcc7832,
        "operator" to 0xFFa9b7c6, "punctuation" to 0xFFa9b7c6, "attr-name" to 0xFFa9b7c6,
        "tag" to 0xFFe8bf6a, "doctype" to 0xFFe8bf6a, "builtin" to 0xFFe8bf6a,
        "entity" to 0xFF6897bb, "number" to 0xFF6897bb, "symbol" to 0xFF6897bb,
        "property" to 0xFF9876aa, "constant" to 0xFF9876aa, "variable" to 0xFF9876aa,
        "string" to 0xFF6a8759, "char" to 0xFF6a8759,
        "annotation" to 0xFFbbb438,
        "attr-value" to 0xFFa5c261,
        "url" to 0xFF287bde,
        "function" to 0xFFffc66d, "class-name" to 0xFFffc66d,
        "regex" to 0xFF364135,
        "inserted" to 0xFF294436,
        "deleted" to 0xFF484a4a,
    )

    fun highlight(code: String, language: String?): AnnotatedString {
        if (language == null) return AnnotatedString(code)
        val grammar = prism4j.grammar(language.lowercase()) ?: return AnnotatedString(code)
        val tokens = prism4j.tokenize(code, grammar)
        return buildHighlighted(tokens)
    }

    private fun buildHighlighted(tokens: List<Prism4j.Node>): AnnotatedString {
        val builder = AnnotatedString.Builder()
        for (node in tokens) {
            appendNode(builder, node)
        }
        return builder.toAnnotatedString()
    }

    private fun appendNode(builder: AnnotatedString.Builder, node: Prism4j.Node) {
        when (node) {
            is Prism4j.Text -> builder.append(node.literal())
            is Prism4j.Syntax -> {
                val color = resolveColor(node.type(), node.alias())
                if (color != null) {
                    val style = SpanStyle(color = Color(color.toInt()))
                    builder.pushStyle(style)
                    for (child in node.children()) appendNode(builder, child)
                    builder.pop()
                } else {
                    for (child in node.children()) appendNode(builder, child)
                }
                // Bold for "important"/"bold" types
                if (node.type() == "important" || node.type() == "bold" || node.alias() == "bold") {
                    // Already rendered children above, just noting for future
                }
            }
        }
    }

    private fun resolveColor(type: String, alias: String?): Long? {
        return colorMap[type] ?: alias?.let { colorMap[it] }
    }
}
