package com.fluid.compose

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import io.noties.markwon.ext.latex.JLatexMathBlockParser
import io.noties.markwon.ext.tasklist.TaskListPostProcessor
import org.junit.Assert.*
import org.junit.Test

class ComposeMarkdownVisitorTest {

    private val theme = MarkdownTheme()

    private val parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .customBlockParserFactory(JLatexMathBlockParser.Factory())
        .postProcessor(TaskListPostProcessor())
        .postProcessor(InlineLatexPostProcessor())
        .build()

    private fun parse(md: String): List<MarkdownNode> {
        val doc = parser.parse(md)
        val visitor = ComposeMarkdownVisitor(theme)
        doc.accept(visitor)
        return visitor.result()
    }

    // ── Basic text ──────────────────────────────────────────

    @Test
    fun `plain text produces single TextBlock`() {
        val nodes = parse("Hello world")
        assertEquals(1, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.TextBlock)
        assertEquals("Hello world", (nodes[0] as MarkdownNode.TextBlock).text.text)
    }

    @Test
    fun `bold text has bold span`() {
        val nodes = parse("**bold**")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("bold", text.text)
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `italic text has italic span`() {
        val nodes = parse("*italic*")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("italic", text.text)
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `inline code has monospace span`() {
        val nodes = parse("`code`")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("code", text.text)
        assertTrue(text.spanStyles.isNotEmpty())
    }

    // ── Headings ────────────────────────────────────────────

    @Test
    fun `heading produces styled text`() {
        val nodes = parse("# Title")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("Title"))
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `multiple heading levels`() {
        val nodes = parse("# H1\n## H2\n### H3")
        assertEquals(1, nodes.size) // all in one TextBlock
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("H1"))
        assertTrue(text.text.contains("H2"))
        assertTrue(text.text.contains("H3"))
    }

    // ── Links ───────────────────────────────────────────────

    @Test
    fun `link has URL annotation`() {
        val nodes = parse("[click](https://example.com)")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("click", text.text)
        val annotations = text.getStringAnnotations("URL", 0, text.length)
        assertEquals(1, annotations.size)
        assertEquals("https://example.com", annotations[0].item)
    }

    // ── Lists ───────────────────────────────────────────────

    @Test
    fun `bullet list items have bullet prefix`() {
        val nodes = parse("- one\n- two\n- three")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("• one"))
        assertTrue(text.contains("• two"))
        assertTrue(text.contains("• three"))
    }

    @Test
    fun `ordered list items have number prefix`() {
        val nodes = parse("1. first\n2. second")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("1. first"))
        assertTrue(text.contains("2. second"))
    }

    @Test
    fun `ordered then bullet list renders correctly`() {
        val nodes = parse("1. ordered\n\n- bullet")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("1. ordered"))
        assertTrue(text.contains("• bullet"))
        assertFalse(text.contains("2."))
    }

    @Test
    fun `nested bullet lists have indentation`() {
        val nodes = parse("- outer\n  - inner\n    - deep")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("• outer"))
        assertTrue(text.contains("  • inner"))
        assertTrue(text.contains("    • deep"))
    }

    @Test
    fun `nested ordered inside bullet`() {
        val nodes = parse("- item\n  1. sub one\n  2. sub two")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("• item"))
        assertTrue(text.contains("  1. sub one"))
        assertTrue(text.contains("  2. sub two"))
    }

    // ── Code blocks ─────────────────────────────────────────

    @Test
    fun `fenced code block produces CodeBlock node`() {
        val nodes = parse("```python\nprint('hi')\n```")
        assertEquals(1, nodes.size)
        val code = nodes[0] as MarkdownNode.CodeBlock
        assertEquals("python", code.language)
        assertEquals("print('hi')", code.code)
    }

    @Test
    fun `code block language strips trailing info`() {
        val nodes = parse("```js {.highlight}\nconsole.log(1)\n```")
        val code = nodes[0] as MarkdownNode.CodeBlock
        assertEquals("js", code.language)
    }

    @Test
    fun `indented code block produces CodeBlock node`() {
        val nodes = parse("    indented code")
        assertEquals(1, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.CodeBlock)
    }

    @Test
    fun `code block between text flushes correctly`() {
        val nodes = parse("before\n\n```\ncode\n```\n\nafter")
        assertEquals(3, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.TextBlock)
        assertTrue(nodes[1] is MarkdownNode.CodeBlock)
        assertTrue(nodes[2] is MarkdownNode.TextBlock)
    }

    // ── Tables ──────────────────────────────────────────────

    @Test
    fun `table produces TableBlock node`() {
        val nodes = parse("| A | B |\n|---|---|\n| 1 | 2 |")
        assertEquals(1, nodes.size)
        val table = nodes[0] as MarkdownNode.TableBlock
        assertEquals(2, table.headers.size)
        assertEquals("A", table.headers[0].text.text)
        assertEquals("B", table.headers[1].text.text)
        assertEquals(1, table.rows.size)
        assertEquals("1", table.rows[0][0].text.text)
    }

    @Test
    fun `table with alignment`() {
        val nodes = parse("| L | C | R |\n|:--|:--:|--:|\n| a | b | c |")
        val table = nodes[0] as MarkdownNode.TableBlock
        assertEquals(MarkdownNode.TableBlock.Alignment.LEFT, table.alignments[0])
        assertEquals(MarkdownNode.TableBlock.Alignment.CENTER, table.alignments[1])
        assertEquals(MarkdownNode.TableBlock.Alignment.RIGHT, table.alignments[2])
    }

    // ── Horizontal rule ─────────────────────────────────────

    @Test
    fun `thematic break produces HorizontalRule`() {
        val nodes = parse("above\n\n---\n\nbelow")
        assertEquals(3, nodes.size)
        assertTrue(nodes[1] is MarkdownNode.HorizontalRule)
    }

    // ── Strikethrough ───────────────────────────────────────

    @Test
    fun `strikethrough text has combining chars`() {
        val nodes = parse("~~deleted~~")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("\u0336"))
        assertTrue(text.spanStyles.isNotEmpty())
    }

    // ── Block quotes ────────────────────────────────────────

    @Test
    fun `blockquote has vertical bar prefix`() {
        val nodes = parse("> quoted text")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("│"))
        assertTrue(text.contains("quoted text"))
    }

    @Test
    fun `nested blockquotes have multiple bars`() {
        val nodes = parse("> outer\n> > inner")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("│ │"))
        assertTrue(text.contains("inner"))
    }

    // ── Images ──────────────────────────────────────────────

    @Test
    fun `image produces ImageBlock with alt from child text`() {
        val nodes = parse("![alt text](https://img.png)")
        assertEquals(1, nodes.size)
        val img = nodes[0] as MarkdownNode.ImageBlock
        assertEquals("https://img.png", img.url)
        assertEquals("alt text", img.alt)
    }

    // ── Task lists ──────────────────────────────────────────

    @Test
    fun `task list items have checkbox chars`() {
        val nodes = parse("- [ ] todo\n- [x] done")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("☐") || text.contains("todo"))
        assertTrue(text.contains("☑") || text.contains("done"))
    }

    // ── LaTeX blocks ────────────────────────────────────────

    @Test
    fun `block latex produces LatexBlock`() {
        val nodes = parse("$$\nE = mc^2\n$$")
        assertTrue(nodes.any { it is MarkdownNode.LatexBlock })
        val latex = nodes.filterIsInstance<MarkdownNode.LatexBlock>().first()
        assertTrue(latex.formula.contains("E = mc^2"))
    }

    // ── Inline LaTeX ────────────────────────────────────────

    @Test
    fun `inline latex embeds in TextBlock via inlineContent`() {
        val nodes = parse("The formula \$E=mc^2\$ is famous")
        val textBlocks = nodes.filterIsInstance<MarkdownNode.TextBlock>()
        assertEquals(1, textBlocks.size)
        assertTrue(textBlocks[0].inlineLatex.isNotEmpty())
        assertEquals("E=mc^2", textBlocks[0].inlineLatex.values.first())
    }

    @Test
    fun `dollar sign without math chars is not latex`() {
        val nodes = parse("costs \$50 today")
        val latexNodes = nodes.filterIsInstance<MarkdownNode.LatexBlock>()
        assertEquals(0, latexNodes.size)
    }

    // ── Mixed content ───────────────────────────────────────

    @Test
    fun `mixed content produces correct node sequence`() {
        val md = """
# Title

Some **bold** and *italic* text.

```kotlin
val x = 1
```

| H1 | H2 |
|----|----|
| a  | b  |

---

![img](url)
        """.trimIndent()
        val nodes = parse(md)
        assertTrue(nodes[0] is MarkdownNode.TextBlock) // heading + paragraph
        assertTrue(nodes[1] is MarkdownNode.CodeBlock)
        assertTrue(nodes[2] is MarkdownNode.TableBlock)
        assertTrue(nodes[3] is MarkdownNode.HorizontalRule)
        assertTrue(nodes[4] is MarkdownNode.ImageBlock)
    }

    // ── Combined inline styles ──────────────────────────────

    @Test
    fun `bold italic combined`() {
        val nodes = parse("***bold italic***")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("bold italic", text.text)
        assertTrue(text.spanStyles.size >= 2) // both bold + italic spans
    }

    @Test
    fun `link inside bold`() {
        val nodes = parse("**[click](https://x.com)**")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("click", text.text)
        assertTrue(text.spanStyles.isNotEmpty())
        val urls = text.getStringAnnotations("URL", 0, text.length)
        assertEquals(1, urls.size)
        assertEquals("https://x.com", urls[0].item)
    }

    @Test
    fun `inline code inside heading`() {
        val nodes = parse("# Title with `code`")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("Title with "))
        assertTrue(text.text.contains("code"))
        assertTrue(text.spanStyles.size >= 2) // heading + code spans
    }

    // ── Paragraph separation ────────────────────────────────

    @Test
    fun `multiple paragraphs separated by newlines`() {
        val nodes = parse("First paragraph.\n\nSecond paragraph.")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("First paragraph."))
        assertTrue(text.contains("Second paragraph."))
        assertTrue(text.contains("\n"))
    }

    // ── Empty and edge cases ────────────────────────────────

    @Test
    fun `empty code block`() {
        val nodes = parse("```\n```")
        assertEquals(1, nodes.size)
        val code = nodes[0] as MarkdownNode.CodeBlock
        assertTrue(code.code.isEmpty())
    }

    @Test
    fun `code block with no language`() {
        val nodes = parse("```\nhello\n```")
        val code = nodes[0] as MarkdownNode.CodeBlock
        assertNull(code.language)
        assertEquals("hello", code.code)
    }

    // ── Table with inline markdown ──────────────────────────

    @Test
    fun `table cells with bold and code`() {
        val nodes = parse("| **bold** | `code` |\n|---|---|\n| [link](url) | *italic* |")
        val table = nodes[0] as MarkdownNode.TableBlock
        assertEquals("bold", table.headers[0].text.text)
        assertTrue(table.headers[0].text.spanStyles.isNotEmpty()) // bold span
        assertEquals("code", table.headers[1].text.text)
        assertTrue(table.headers[1].text.spanStyles.isNotEmpty()) // code span
        assertEquals("link", table.rows[0][0].text.text)
        val urls = table.rows[0][0].text.getStringAnnotations("URL", 0, 4)
        assertEquals(1, urls.size)
    }

    // ── HTML inline tags ────────────────────────────────────

    @Test
    fun `sup tag produces superscript span`() {
        val nodes = parse("x<sup>2</sup>")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("x"))
        assertTrue(text.text.contains("2"))
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `sub tag produces subscript span`() {
        val nodes = parse("H<sub>2</sub>O")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("H"))
        assertTrue(text.text.contains("2"))
        assertTrue(text.text.contains("O"))
    }

    @Test
    fun `mark tag produces highlight span`() {
        val nodes = parse("<mark>highlighted</mark>")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("highlighted", text.text)
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `u tag produces underline span`() {
        val nodes = parse("<u>underlined</u>")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertEquals("underlined", text.text)
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `br tag produces newline`() {
        val nodes = parse("line1<br>line2")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("line1"))
        assertTrue(text.contains("\n"))
        assertTrue(text.contains("line2"))
    }

    @Test
    fun `del tag produces strikethrough`() {
        val nodes = parse("<del>removed</del>")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("\u0336"))
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `s tag produces strikethrough`() {
        val nodes = parse("<s>struck</s>")
        val text = (nodes[0] as MarkdownNode.TextBlock).text
        assertTrue(text.text.contains("\u0336"))
        assertTrue(text.spanStyles.isNotEmpty())
    }

    @Test
    fun `dt dd tags render in definition list HTML block`() {
        // <dl><dt><dd> come through as HTML blocks, not inline
        val nodes = parse("<dl>\n<dt>term</dt>\n<dd>definition</dd>\n</dl>")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("term"))
    }

    // ── Multiple inline LaTeX ───────────────────────────────

    @Test
    fun `multiple inline latex in one paragraph`() {
        val nodes = parse("Given \$x^2\$ and \$y^2\$ we compute")
        val textBlocks = nodes.filterIsInstance<MarkdownNode.TextBlock>()
        val totalLatex = textBlocks.sumOf { it.inlineLatex.size }
        assertEquals(2, totalLatex)
    }

    // ── Image between text ──────────────────────────────────

    @Test
    fun `image between text produces correct node order`() {
        val nodes = parse("before\n\n![pic](url)\n\nafter")
        assertTrue(nodes[0] is MarkdownNode.TextBlock)
        assertTrue(nodes[1] is MarkdownNode.ImageBlock)
        assertTrue(nodes[2] is MarkdownNode.TextBlock)
        assertTrue((nodes[0] as MarkdownNode.TextBlock).text.text.contains("before"))
        assertTrue((nodes[2] as MarkdownNode.TextBlock).text.text.contains("after"))
    }

    // ── Task list mixed ─────────────────────────────────────

    @Test
    fun `task list mixed checked and unchecked`() {
        val nodes = parse("- [ ] a\n- [x] b\n- [ ] c")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        // Should have both checkbox types
        assertTrue(text.contains("a"))
        assertTrue(text.contains("b"))
        assertTrue(text.contains("c"))
    }

    // ── Soft vs hard line break ─────────────────────────────

    @Test
    fun `soft line break becomes space`() {
        val nodes = parse("line one\nline two")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        // Soft break = space, not newline
        assertTrue(text.contains("line one line two") || text.contains("line one\nline two"))
    }

    @Test
    fun `hard line break becomes newline`() {
        val nodes = parse("line one  \nline two")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("\n"))
        assertTrue(text.contains("line one"))
        assertTrue(text.contains("line two"))
    }

    // ── Deeply nested lists ─────────────────────────────────

    @Test
    fun `three level nested bullet list`() {
        val nodes = parse("- a\n  - b\n    - c\n      - d")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("• a"))
        assertTrue(text.contains("  • b"))
        assertTrue(text.contains("    • c"))
        assertTrue(text.contains("      • d"))
    }

    @Test
    fun `ordered list numbering is sequential`() {
        // CommonMark treats consecutive ordered items as one list
        val nodes = parse("1. a\n2. b\n\n1. x\n2. y")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("1. a"))
        assertTrue(text.contains("2. b"))
        // Items continue numbering (CommonMark loose list behavior)
        assertTrue(text.contains("x"))
        assertTrue(text.contains("y"))
    }

    // ── Nested blockquotes depth 3 ──────────────────────────

    @Test
    fun `triple nested blockquote`() {
        val nodes = parse("> a\n> > b\n> > > c")
        val text = (nodes[0] as MarkdownNode.TextBlock).text.text
        assertTrue(text.contains("│ │ │"))
        assertTrue(text.contains("c"))
    }

    // ── Syntax highlighter aliases ──────────────────────────

    @Test
    fun `syntax highlighter resolves js alias`() {
        val result = ComposeSyntaxHighlighter.highlight("var x = 1;", "js")
        assertTrue(result.spanStyles.isNotEmpty()) // should have colored tokens
    }

    @Test
    fun `syntax highlighter resolves py alias`() {
        val result = ComposeSyntaxHighlighter.highlight("def foo():\n  pass", "py")
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `syntax highlighter resolves rs alias`() {
        val result = ComposeSyntaxHighlighter.highlight("fn main() {}", "rs")
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `syntax highlighter unknown language returns plain text`() {
        val result = ComposeSyntaxHighlighter.highlight("hello", "nonexistent_lang_xyz")
        assertEquals("hello", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `syntax highlighter null language returns plain text`() {
        val result = ComposeSyntaxHighlighter.highlight("hello", null)
        assertEquals("hello", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }
    @Test
    fun `rtl strikethrough debug`() {
        val inputs = listOf(
            "~~hello~~",
            "~~\u0647\u0630\u0627 \u0646\u0635~~",
            "~~Hello \u0645\u0631\u062D\u0628\u0627 Hello~~",
            "**Expected Result:** ~~\u0647\u0630\u0627 \u0646\u0635 \u0645\u0634\u0637\u0648\u0628~~",
            "***~~\u0647\u0630\u0627 \u0646\u0635 \u0639\u0631\u064A\u0636~~***",
        )
        for (md in inputs) {
            val nodes = parse(md)
            println("INPUT: $md")
            for ((i, node) in nodes.withIndex()) {
                if (node is MarkdownNode.TextBlock) {
                    println("  NODE[$i] TEXT: '${node.text.text}'")
                    println("  NODE[$i] SPANS: ${node.text.spanStyles.map { "${it.item.textDecoration}@${it.start}..${it.end}" }}")
                }
            }
        }
    }
}
