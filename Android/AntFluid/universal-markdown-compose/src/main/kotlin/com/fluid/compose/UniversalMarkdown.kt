package com.fluid.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import io.noties.markwon.ext.latex.JLatexMathBlockParser
import io.noties.markwon.ext.latex.LatexDocumentTranspiler
import io.noties.markwon.ext.tasklist.TaskListPostProcessor

/**
 * Native Compose markdown renderer.
 * Parses markdown to CommonMark AST, walks it to produce AnnotatedString nodes,
 * renders everything inside SelectionContainer for text selection support.
 *
 * @param content Raw markdown string
 * @param modifier Modifier for the outer column
 * @param theme Styling configuration
 * @param onLinkClick Optional link click handler; defaults to opening URI
 * @param onImageContent Optional composable for rendering images
 */
@Composable
fun UniversalMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = remember { MarkdownTheme() },
    animateStreaming: Boolean = false,
    onLinkClick: ((String) -> Unit)? = null,
    onImageContent: (@Composable (url: String, alt: String) -> Unit)? = null,
    onExportPdf: ((String) -> Unit)? = null,
) {
    val nodes = remember(content, theme) {
        parseMarkdown(content, theme)
    }
    val prevCount = remember { mutableIntStateOf(0) }

    val uriHandler = LocalUriHandler.current
    val linkHandler = onLinkClick ?: { url ->
        try { uriHandler.openUri(url) } catch (_: Exception) {}
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) { ComposeLatexRenderer.init(context) }

    SelectionContainer {
        Column(modifier = modifier) {
            for ((index, node) in nodes.withIndex()) {
                val isNew = animateStreaming && index >= prevCount.intValue
                val wrapper: @Composable (@Composable () -> Unit) -> Unit = if (isNew) {
                    { content -> AnimatedVisibility(visible = true, enter = fadeIn()) { content() } }
                } else {
                    { content -> content() }
                }
                wrapper {
                when (node) {
                    is MarkdownNode.TextBlock -> MarkdownText(node, linkHandler, theme)
                    is MarkdownNode.CodeBlock -> CodeBlockView(node, theme, onExportPdf)
                    is MarkdownNode.TableBlock -> TableBlockView(node, theme, onImageContent)
                    is MarkdownNode.ImageBlock -> {
                        if (onImageContent != null) {
                            onImageContent(node.url, node.alt)
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.codeBgColor)
                                    .border(1.dp, theme.codeBorderColor, RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = node.alt.ifEmpty { node.url },
                                    color = theme.textColor.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is MarkdownNode.LatexBlock -> {
                        LatexView(node, theme)
                    }
                    is MarkdownNode.HorizontalRule -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = theme.hrColor, thickness = 1.dp
                        )
                    }
                }
                }
            }
        }
    }
    LaunchedEffect(nodes.size) { prevCount.intValue = nodes.size }
}

@Composable
private fun MarkdownText(node: MarkdownNode.TextBlock, onLinkClick: (String) -> Unit, theme: MarkdownTheme) {
    val text = node.text
    val clipboardManager = LocalClipboardManager.current
    val layoutResult = remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    val linked = remember(text) {
        val builder = AnnotatedString.Builder(text)
        text.getStringAnnotations("URL", 0, text.length).forEach { range ->
            builder.addLink(
                androidx.compose.ui.text.LinkAnnotation.Url(range.item),
                range.start, range.end
            )
        }
        builder.toAnnotatedString()
    }

    val gestureModifier = Modifier.pointerInput(text) {
        detectTapGestures(
            onTap = { offset ->
                layoutResult.value?.let { layout ->
                    val pos = layout.getOffsetForPosition(offset)
                    text.getStringAnnotations("URL", pos, pos)
                        .firstOrNull()?.let { onLinkClick(it.item) }
                }
            },
            onLongPress = { offset ->
                layoutResult.value?.let { layout ->
                    val pos = layout.getOffsetForPosition(offset)
                    text.getStringAnnotations("URL", pos, pos)
                        .firstOrNull()?.let {
                            clipboardManager.setText(AnnotatedString(it.item))
                        }
                }
            }
        )
    }

    if (node.inlineLatex.isEmpty()) {
        BasicText(
            text = linked,
            style = theme.bodyStyle,
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier.padding(vertical = 2.dp).then(gestureModifier)
        )
    } else {
        val inlineContent = node.inlineLatex.mapValues { (_, formula) ->
            val bmp = ComposeLatexRenderer.render(formula, textSize = 36f, textColor = theme.textColor)
            InlineTextContent(
                placeholder = androidx.compose.ui.text.Placeholder(
                    width = ((bmp?.width ?: 20) / 2).sp,
                    height = ((bmp?.height ?: 16) / 2).sp,
                    placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter
                )
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = formula,
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(formula, style = theme.codeStyle, fontSize = 12.sp)
                }
            }
        }
        BasicText(
            text = linked,
            style = theme.bodyStyle,
            inlineContent = inlineContent,
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier.padding(vertical = 2.dp).then(gestureModifier)
        )
    }
}

@Composable
private fun CodeBlockView(node: MarkdownNode.CodeBlock, theme: MarkdownTheme, onExportPdf: ((String) -> Unit)? = null) {
    val shape = RoundedCornerShape(theme.codeCornerRadius)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val isLatex = node.language?.lowercase()?.let { it == "latex" || it == "tex" } == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(theme.codeBgColor, shape)
            .border(1.dp, theme.codeBorderColor, shape)
    ) {
        // Header: language label + export/copy buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.language ?: "",
                color = theme.codeLabelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLatex && onExportPdf != null) {
                    Text(
                        text = "Export PDF",
                        color = theme.codeLabelColor.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onExportPdf(node.code) }
                    )
                }
                Text(
                    text = if (copied) "Copied" else "Copy",
                    color = theme.codeLabelColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(node.code))
                        copied = true
                    }
                )
            }
        }
        if (!node.language.isNullOrBlank() || node.code.isNotEmpty()) {
            HorizontalDivider(color = theme.codeBorderColor, thickness = 0.5.dp)
        }
        // Code content with horizontal scroll
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            val highlighted = node.highlighted.takeIf { it.text.isNotEmpty() } ?: AnnotatedString(node.code)
            Text(text = highlighted, style = theme.codeStyle)
        }
    }
}

@Composable
private fun LatexView(node: MarkdownNode.LatexBlock, theme: MarkdownTheme) {
    val bmp = remember(node.formula) {
        ComposeLatexRenderer.render(formula = node.formula, textSize = 44f, textColor = theme.textColor)
    }
    if (bmp != null) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = node.formula,
                contentScale = ContentScale.Fit,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    } else {
        Text(node.formula, style = theme.codeStyle, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun TableBlockView(node: MarkdownNode.TableBlock, theme: MarkdownTheme, onImageContent: (@Composable (url: String, alt: String) -> Unit)? = null) {
    val shape = RoundedCornerShape(theme.tableCornerRadius)
    val colCount = node.headers.size.coerceAtLeast(node.rows.firstOrNull()?.size ?: 1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        // Render as columns so each column is as wide as its widest cell
        Row(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, theme.tableBorderColor, shape)
        ) {
            for (col in 0 until colCount) {
                val align = node.alignments.getOrElse(col) { MarkdownNode.TableBlock.Alignment.LEFT }
                Column(Modifier.width(IntrinsicSize.Max)) {
                    // Header cell
                    if (node.headers.isNotEmpty()) {
                        val hCell = node.headers.getOrNull(col)
                        Box(Modifier.fillMaxWidth().background(theme.tableHeaderBgColor).padding(8.dp)) {
                            if (hCell != null) {
                                TableCellView(hCell.text, theme.tableHeaderTextColor, FontWeight.Bold, align, Modifier)
                            }
                        }
                        HorizontalDivider(color = theme.tableBorderColor, thickness = 1.dp)
                    }
                    // Body cells
                    node.rows.forEachIndexed { rowIdx, row ->
                        val cell = row.getOrNull(col)
                        Box(Modifier.fillMaxWidth().background(theme.tableBodyBgColor).padding(8.dp)) {
                            if (cell != null) {
                                if (cell.imageUrl != null && onImageContent != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        onImageContent(cell.imageUrl, cell.imageAlt ?: "")
                                        if (cell.text.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            TableCellView(cell.text, theme.tableBodyTextColor, FontWeight.Normal, align, Modifier)
                                        }
                                    }
                                } else {
                                    TableCellView(cell.text, theme.tableBodyTextColor, FontWeight.Normal, align, Modifier)
                                }
                            }
                        }
                        if (rowIdx < node.rows.lastIndex) {
                            HorizontalDivider(color = theme.tableBorderColor, thickness = 0.5.dp)
                        }
                    }
                }
                // Column divider
                if (col < colCount - 1) {
                    Box(Modifier.width(0.5.dp).background(theme.tableBorderColor).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun TableCellView(
    text: AnnotatedString,
    color: androidx.compose.ui.graphics.Color,
    weight: FontWeight,
    alignment: MarkdownNode.TableBlock.Alignment,
    modifier: Modifier
) {
    val textAlign = when (alignment) {
        MarkdownNode.TableBlock.Alignment.CENTER -> TextAlign.Center
        MarkdownNode.TableBlock.Alignment.RIGHT -> TextAlign.End
        MarkdownNode.TableBlock.Alignment.LEFT -> TextAlign.Start
    }
    Text(
        text = text,
        color = color,
        fontWeight = weight,
        fontSize = 12.sp,
        textAlign = textAlign,
        modifier = modifier
    )
}

// ── Parsing ─────────────────────────────────────────────────

private val parser: Parser by lazy {
    Parser.builder()
        .extensions(listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
        ))
        .customBlockParserFactory(JLatexMathBlockParser.Factory())
        .postProcessor(TaskListPostProcessor())
        .postProcessor(InlineLatexPostProcessor())
        .build()
}

private val parserLock = Any()

private fun parseMarkdown(content: String, theme: MarkdownTheme): List<MarkdownNode> {
    if (content.isBlank()) return emptyList()
    val preprocessed = preprocessMarkdown(content)
    val document = synchronized(parserLock) { parser.parse(preprocessed) }
    val visitor = ComposeMarkdownVisitor(theme)
    document.accept(visitor)
    val nodes = visitor.result()
    // Post-process: apply syntax highlighting to code blocks
    return nodes.map { node ->
        if (node is MarkdownNode.CodeBlock) {
            node.copy(highlighted = ComposeSyntaxHighlighter.highlight(node.code, node.language))
        } else node
    }
}

/**
 * Reuses the same preprocessing pipeline from UniversalMarkdown's MarkdownParser.
 */
private fun preprocessMarkdown(input: String): String {
    var s = input
    // Transpile full LaTeX documents
    if (LatexDocumentTranspiler.isLatexDocument(s)) {
        s = LatexDocumentTranspiler.transpile(s)
    }
    // Wrap inline LaTeX documents in code fences
    s = wrapInlineLatexDocs(s)
    // Escape HTML outside code fences
    s = escapeHtmlOutsideCode(s)
    // Escape lone asterisks
    s = escapeLoneAsterisks(s)
    // Convert definition lists to HTML <dl> (matches DefinitionListPlugin)
    s = processDefinitionLists(s)
    // Convert \bm{X} to bold-italic (matches original processMarkdown)
    s = s.replace(Regex("\\$\\$?\\s*\\\\bm\\{([A-Za-z]{1,9})\\}\\s*\\$\\$?"), "***$1***")
    // Convert common LaTeX symbols to unicode
    s = replaceLatexSymbols(s)
    return s
}

private val latexSymbols = mapOf(
    "\\rightarrow" to "→", "\\leftarrow" to "←", "\\leftrightarrow" to "↔",
    "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐", "\\Leftrightarrow" to "⇔",
    "\\uparrow" to "↑", "\\downarrow" to "↓",
    "\\times" to "×", "\\div" to "÷", "\\pm" to "±", "\\mp" to "∓",
    "\\leq" to "≤", "\\geq" to "≥", "\\neq" to "≠", "\\approx" to "≈",
    "\\equiv" to "≡", "\\sim" to "∼", "\\propto" to "∝",
    "\\infty" to "∞", "\\partial" to "∂", "\\nabla" to "∇",
    "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫",
    "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
    "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
    "\\lambda" to "λ", "\\mu" to "μ", "\\pi" to "π", "\\sigma" to "σ",
    "\\tau" to "τ", "\\phi" to "φ", "\\omega" to "ω",
    "\\Delta" to "Δ", "\\Sigma" to "Σ", "\\Omega" to "Ω", "\\Pi" to "Π",
    "\\in" to "∈", "\\notin" to "∉", "\\subset" to "⊂", "\\supset" to "⊃",
    "\\cup" to "∪", "\\cap" to "∩", "\\emptyset" to "∅",
    "\\forall" to "∀", "\\exists" to "∃",
    "\\land" to "∧", "\\lor" to "∨", "\\neg" to "¬",
    "\\cdot" to "·", "\\ldots" to "…", "\\cdots" to "⋯",
    "\\langle" to "⟨", "\\rangle" to "⟩",
    "\\oplus" to "⊕", "\\otimes" to "⊗",
    "\\checkmark" to "✓", "\\dagger" to "†",
)

private fun replaceLatexSymbols(input: String): String {
    var s = input
    for ((tex, uni) in latexSymbols) {
        s = s.replace(tex, uni)
    }
    return s
}

private fun wrapInlineLatexDocs(input: String): String {
    val fences = mutableListOf<String>()
    val placeholder = "\u0000FENCE%d\u0000"
    val fm = Regex("```[\\s\\S]*?```").findAll(input)
    var stripped = input
    var idx = 0
    for (match in fm) {
        fences += match.value
        stripped = stripped.replaceFirst(match.value, placeholder.format(idx++))
    }
    stripped = stripped.replace(
        Regex("(\\\\documentclass[\\s\\S]*?\\\\end\\{document\\})"),
        "\n```latex\n$1\n```\n"
    )
    for (i in fences.indices) {
        stripped = stripped.replace(placeholder.format(i), fences[i])
    }
    return stripped
}

private fun escapeHtmlOutsideCode(input: String): String {
    val sb = StringBuilder()
    var inFence = false
    for (line in input.split("\n")) {
        if (line.trim().startsWith("```")) inFence = !inFence
        if (inFence) {
            sb.append(line)
        } else {
            sb.append(line.replace(
                Regex("<(?!/?(?:br|sup|sub|mark|del|ins|a |a>|em|strong|code|pre|ul|ol|li|p|h[1-6]|table|tr|td|th|thead|tbody|hr|blockquote|img ))"),
                "&lt;"
            ))
        }
        sb.append("\n")
    }
    return sb.toString()
}

private fun escapeLoneAsterisks(input: String): String {
    val sb = StringBuilder()
    var inFence = false
    for (line in input.split("\n")) {
        if (line.trim().startsWith("```")) inFence = !inFence
        if (inFence) sb.append(line)
        else sb.append(line.replace(Regex("(?<= )\\*(?= )"), "\\*"))
        sb.append("\n")
    }
    return sb.toString()
}

private fun processDefinitionLists(input: String): String {
    if (!input.contains(":")) return input
    // Skip if there's a markdown table (avoid false positives)
    if (Regex("\\|[ \\t]*:?-+:?[ \\t]*(\\|[ \\t]*:?-+:?[ \\t]*)*(\\|$)?", RegexOption.MULTILINE).containsMatchIn(input))
        return input
    val lines = input.split("\n")
    val result = StringBuilder(input.length + 100)
    var i = 0
    while (i < lines.size) {
        // Look for term followed by : definition
        if (i + 1 < lines.size && lines[i].trim().isNotEmpty() && lines[i + 1].trim().startsWith(":")
            && !lines[i].trim().startsWith("#") && !lines[i].trim().startsWith("```")) {
            result.append("<dl>\n")
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) { i++; continue }
                if (line.startsWith(":")) {
                    result.append("  <dd>").append(line.substring(1).trim()).append("</dd>\n")
                    i++
                } else if (i + 1 < lines.size && lines[i + 1].trim().startsWith(":")) {
                    result.append("  <dt>").append(line).append("</dt>\n")
                    i++
                } else break
            }
            result.append("</dl>\n")
        } else {
            result.append(lines[i]).append("\n")
            i++
        }
    }
    return result.toString()
}
