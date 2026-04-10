package com.fluid.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.appendInlineContent
import org.commonmark.node.*

/**
 * Walks a CommonMark AST and produces a List<MarkdownNode>.
 * Text runs accumulate into AnnotatedString; block elements flush the
 * current text and emit their own MarkdownNode.
 */
class ComposeMarkdownVisitor(private val theme: MarkdownTheme) : AbstractVisitor() {

    private val nodes = mutableListOf<MarkdownNode>()
    private var builder = AnnotatedString.Builder()
    private val inlineLatex = mutableMapOf<String, String>() // id -> formula
    private var latexCounter = 0
    private var listDepth = 0
    private var orderedIndex = 0

    fun result(): List<MarkdownNode> {
        flushText()
        return nodes.toList()
    }

    // ── helpers ──────────────────────────────────────────────

    private fun flushText() {
        val str = builder.toAnnotatedString()
        if (str.text.isNotEmpty()) {
            nodes += MarkdownNode.TextBlock(str, inlineLatex.toMap())
            builder = AnnotatedString.Builder()
            inlineLatex.clear()
        }
    }

    private inline fun withStyle(style: SpanStyle, block: () -> Unit) {
        builder.pushStyle(style)
        block()
        builder.pop()
    }

    private fun visitChildrenInline(node: Node) {
        var child = node.firstChild
        while (child != null) {
            child.accept(this)
            child = child.next
        }
    }

    // ── block nodes ─────────────────────────────────────────

    override fun visit(document: Document) = visitChildren(document)

    override fun visit(paragraph: Paragraph) {
        // Don't add leading newline for first content
        if (builder.length > 0) builder.append("\n")
        visitChildrenInline(paragraph)
    }

    override fun visit(heading: Heading) {
        if (builder.length > 0) builder.append("\n")
        val level = (heading.level - 1).coerceIn(0, theme.headingSizes.lastIndex)
        withStyle(SpanStyle(
            fontSize = theme.headingSizes[level].sp,
            fontWeight = theme.headingWeight,
            color = theme.headingColor
        )) {
            visitChildrenInline(heading)
        }
        builder.append("\n")
    }

    private var blockQuoteDepth = 0
    private var strikethroughDepth = 0

    override fun visit(blockQuote: BlockQuote) {
        if (builder.length > 0) builder.append("\n")
        blockQuoteDepth++
        val prefix = "\u2502 ".repeat(blockQuoteDepth)
        withStyle(SpanStyle(color = theme.blockQuoteTextColor, background = theme.blockQuoteBgColor)) {
            var child = blockQuote.firstChild
            while (child != null) {
                if (child is BlockQuote) {
                    child.accept(this)
                } else if (child is Paragraph) {
                    builder.append(prefix)
                    visitChildrenInline(child)
                } else {
                    builder.append(prefix)
                    child.accept(this)
                }
                child = child.next
            }
        }
        blockQuoteDepth--
    }

    override fun visit(bulletList: BulletList) {
        listDepth++
        val savedOrderedIndex = orderedIndex
        orderedIndex = 0
        visitChildren(bulletList)
        orderedIndex = savedOrderedIndex
        listDepth--
    }

    override fun visit(orderedList: OrderedList) {
        listDepth++
        val savedOrderedIndex = orderedIndex
        orderedIndex = orderedList.startNumber
        visitChildren(orderedList)
        orderedIndex = savedOrderedIndex
        listDepth--
    }

    override fun visit(listItem: ListItem) {
        if (builder.length > 0 && !builder.toAnnotatedString().text.endsWith("\n"))
            builder.append("\n")
        val indent = "  ".repeat((listDepth - 1).coerceAtLeast(0))
        val bullet = if (orderedIndex > 0) {
            val b = "${orderedIndex}. "
            orderedIndex++
            b
        } else "• "
        withStyle(SpanStyle(color = theme.listBulletColor)) {
            builder.append("$indent$bullet")
        }
        // Visit children but suppress leading newline from inner Paragraph
        var child = listItem.firstChild
        while (child != null) {
            if (child is Paragraph) {
                // Inline the paragraph content without the leading newline
                visitChildrenInline(child)
            } else {
                child.accept(this)
            }
            child = child.next
        }
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        flushText()
        // Strip trailing info string (e.g. "kotlin {.highlight}" -> "kotlin")
        val lang = fencedCodeBlock.info?.trim()?.split("\\s+".toRegex())?.firstOrNull()?.takeIf { it.isNotBlank() }
        nodes += MarkdownNode.CodeBlock(
            code = fencedCodeBlock.literal.trimEnd(),
            language = lang,
            highlighted = AnnotatedString("")
        )
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        flushText()
        nodes += MarkdownNode.CodeBlock(
            code = indentedCodeBlock.literal.trimEnd(),
            language = null,
            highlighted = AnnotatedString("")
        )
    }

    override fun visit(thematicBreak: ThematicBreak) {
        flushText()
        nodes += MarkdownNode.HorizontalRule
    }

    override fun visit(image: Image) {
        flushText()
        // Alt text is in child Text nodes, not image.title
        val alt = buildString {
            var child = image.firstChild
            while (child != null) {
                if (child is Text) append(child.literal)
                child = child.next
            }
        }.ifEmpty { image.title ?: "" }
        nodes += MarkdownNode.ImageBlock(url = image.destination, alt = alt)
    }

    override fun visit(htmlBlock: HtmlBlock) {
        // Render raw HTML as plain text
        if (builder.length > 0) builder.append("\n")
        builder.append(htmlBlock.literal.trim())
    }

    // ── inline nodes ────────────────────────────────────────

    override fun visit(text: Text) {
        val clean = text.literal
            .replace("\uFEFF", "") // BOM
            .replace("\u00AD", "") // soft hyphen
        if (strikethroughDepth > 0) {
            // Embed U+0336 per grapheme cluster so strikethrough survives copy/paste
            // Use code point iteration to handle surrogate pairs (emoji, ancient scripts)
            val sb = StringBuilder()
            var i = 0
            while (i < clean.length) {
                val cp = Character.codePointAt(clean, i)
                val charCount = Character.charCount(cp)
                // Append the full code point (1 or 2 chars)
                sb.append(clean, i, i + charCount)
                // Skip combining marks — don't insert U+0336 between base + combining
                val next = i + charCount
                if (next < clean.length) {
                    val nextCp = Character.codePointAt(clean, next)
                    val type = Character.getType(nextCp)
                    if (type == Character.NON_SPACING_MARK.toInt() ||
                        type == Character.COMBINING_SPACING_MARK.toInt() ||
                        type == Character.ENCLOSING_MARK.toInt() ||
                        nextCp == 0x200D) { // ZWJ — part of emoji sequence
                        i += charCount
                        continue
                    }
                }
                if (cp != '\n'.code && cp != '\r'.code && cp != 0x200D) {
                    sb.append('\u0336')
                }
                i += charCount
            }
            builder.append(sb.toString())
        } else {
            builder.append(clean)
        }
    }

    override fun visit(emphasis: Emphasis) {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            visitChildrenInline(emphasis)
        }
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            visitChildrenInline(strongEmphasis)
        }
    }

    override fun visit(code: Code) {
        withStyle(SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = theme.inlineCodeTextColor,
            background = theme.inlineCodeBgColor
        )) {
            builder.append(code.literal)
        }
    }

    override fun visit(link: Link) {
        builder.pushStyle(SpanStyle(
            color = theme.linkColor,
            textDecoration = TextDecoration.Underline
        ))
        builder.pushStringAnnotation("URL", link.destination)
        visitChildrenInline(link)
        builder.pop() // annotation
        builder.pop() // style
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        builder.append(" ")
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        builder.append("\n")
    }

    override fun visit(htmlInline: HtmlInline) {
        val tag = htmlInline.literal.lowercase().trim()
        when {
            tag == "<s>" || tag == "<del>" || tag == "<strike>" -> {
                strikethroughDepth++
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            }
            tag == "</s>" || tag == "</del>" || tag == "</strike>" -> {
                tryPop()
                strikethroughDepth--
            }
            tag == "<u>" ->
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            tag == "</u>" ->
                tryPop()
            tag == "<mark>" ->
                builder.pushStyle(SpanStyle(background = Color(0x66FFFF00)))
            tag == "</mark>" ->
                tryPop()
            tag == "<sup>" ->
                builder.pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp))
            tag == "</sup>" ->
                tryPop()
            tag == "<sub>" ->
                builder.pushStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 10.sp))
            tag == "</sub>" ->
                tryPop()
            tag == "<br>" || tag == "<br/>" || tag == "<br />" ->
                builder.append("\n")
            tag == "<dt>" ->
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            tag == "</dt>" ->
                tryPop()
            tag == "<dd>" ->
                builder.append("  ")
            tag == "</dd>" || tag == "<dl>" || tag == "</dl>" -> {} // structural, no-op
            else -> builder.append(htmlInline.literal)
        }
    }

    private fun tryPop() {
        try { builder.pop() } catch (_: Exception) {}
    }

    // ── custom nodes (tables, strikethrough, etc.) ──────────
    // These are handled by extensions registered with the parser.
    // We override customNode/customBlock to catch them.

    override fun visit(customNode: CustomNode) {
        // Strikethrough from commonmark-ext-gfm-strikethrough
        if (customNode.javaClass.simpleName == "Strikethrough") {
            strikethroughDepth++
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                visitChildrenInline(customNode)
            }
            strikethroughDepth--
            return
        }
        // Inline LaTeX ($...$)
        if (customNode.javaClass.simpleName == "JLatexMathNode") {
            val latex = try {
                customNode.javaClass.getMethod("latex").invoke(customNode) as? String
            } catch (_: Exception) { null }
            if (!latex.isNullOrBlank()) {
                val id = "latex_${latexCounter++}"
                inlineLatex[id] = latex
                builder.appendInlineContent(id, latex)
                return
            }
        }
        // Fallback: visit children
        visitChildrenInline(customNode)
    }

    override fun visit(customBlock: CustomBlock) {
        val className = customBlock.javaClass.simpleName
        // Block LaTeX ($$...$$)
        if (className == "JLatexMathBlock") {
            val latex = try {
                customBlock.javaClass.getMethod("latex").invoke(customBlock) as? String
            } catch (_: Exception) { null }
            if (!latex.isNullOrBlank()) {
                flushText()
                nodes += MarkdownNode.LatexBlock(formula = latex.trim())
                return
            }
        }
        // Task list item (- [ ] or - [x])
        if (className == "TaskListItem") {
            val isDone = try {
                customBlock.javaClass.getMethod("isDone").invoke(customBlock) as? Boolean ?: false
            } catch (_: Exception) { false }
            if (builder.length > 0 && !builder.toAnnotatedString().text.endsWith("\n"))
                builder.append("\n")
            val indent = "  ".repeat((listDepth - 1).coerceAtLeast(0))
            val checkbox = if (isDone) "$indent\u2611 " else "$indent\u2610 "
            builder.append(checkbox)
            visitChildrenInline(customBlock)
            return
        }
        // Table from commonmark-ext-gfm-tables
        if (className == "TableBlock") {
            flushText()
            visitTableBlock(customBlock)
            return
        }
        visitChildren(customBlock)
    }

    private fun visitTableBlock(tableBlock: CustomBlock) {
        val headers = mutableListOf<MarkdownNode.TableCell>()
        val rows = mutableListOf<List<MarkdownNode.TableCell>>()
        val alignments = mutableListOf<MarkdownNode.TableBlock.Alignment>()

        var child = tableBlock.firstChild
        while (child != null) {
            val rowClassName = child.javaClass.simpleName
            if (rowClassName == "TableHead") {
                var headRow = child.firstChild // TableRow
                if (headRow != null) {
                    var cell = headRow.firstChild
                    while (cell != null) {
                        headers += MarkdownNode.TableCell(buildCellText(cell))
                        alignments += getCellAlignment(cell)
                        cell = cell.next
                    }
                }
            } else if (rowClassName == "TableBody") {
                var row = child.firstChild
                while (row != null) {
                    val cells = mutableListOf<MarkdownNode.TableCell>()
                    var cell = row.firstChild
                    while (cell != null) {
                        cells += MarkdownNode.TableCell(buildCellText(cell))
                        cell = cell.next
                    }
                    rows += cells
                    row = row.next
                }
            }
            child = child.next
        }
        nodes += MarkdownNode.TableBlock(headers, rows, alignments)
    }

    private fun buildCellText(cell: Node): AnnotatedString {
        val saved = builder
        builder = AnnotatedString.Builder()
        return try {
            visitChildrenInline(cell)
            builder.toAnnotatedString()
        } finally {
            builder = saved
        }
    }

    private fun getCellAlignment(cell: Node): MarkdownNode.TableBlock.Alignment {
        // Use reflection to get alignment from TableCell node
        return try {
            val m = cell.javaClass.getMethod("getAlignment")
            when (m.invoke(cell)?.toString()) {
                "CENTER" -> MarkdownNode.TableBlock.Alignment.CENTER
                "RIGHT" -> MarkdownNode.TableBlock.Alignment.RIGHT
                else -> MarkdownNode.TableBlock.Alignment.LEFT
            }
        } catch (_: Exception) {
            MarkdownNode.TableBlock.Alignment.LEFT
        }
    }
}
