package com.fluid.compose;

import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.PostProcessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.ext.latex.JLatexMathNode;

/**
 * PostProcessor that finds inline $...$ LaTeX in Text nodes and replaces
 * them with JLatexMathNode custom nodes. Uses the same constraints as
 * JLatexMathInlineProcessor to avoid false positives.
 */
public class InlineLatexPostProcessor implements PostProcessor {

    private static final Pattern INLINE_LATEX = Pattern.compile(
        "(?<!\\$)\\$([^$\\n\\u2500-\\u259F]+?)\\$(?!\\$)");

    private static final Pattern MATH_CHARS = Pattern.compile(
        "[\\\\^_{}+=/<>\\d]|\\\\[a-zA-Z]");

    private static final Pattern REGEX_CHARS = Pattern.compile(
        "\\(\\?[:|!=<]|\\[\\^|\\\\[swdSWD]|\\{\\d+,");

    @Override
    public Node process(Node node) {
        processNode(node);
        return node;
    }

    private void processNode(Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            Node next = child.getNext();
            if (child instanceof Text) {
                processText((Text) child);
            } else {
                processNode(child);
            }
            child = next;
        }
    }

    private void processText(Text textNode) {
        String text = textNode.getLiteral();
        Matcher m = INLINE_LATEX.matcher(text);
        if (!m.find()) return;

        m.reset();
        int lastEnd = 0;
        Node insertBefore = textNode;
        boolean replaced = false;

        while (m.find()) {
            String content = m.group(1);
            if (content.startsWith(" ") || content.endsWith(" ")) continue;
            if (!MATH_CHARS.matcher(content).find()) continue;
            if (REGEX_CHARS.matcher(content).find()) continue;

            // Text before the match
            if (m.start() > lastEnd) {
                Text before = new Text(text.substring(lastEnd, m.start()));
                insertBefore.insertBefore(before);
                insertBefore = before;
            }

            // LaTeX node
            JLatexMathNode latex = new JLatexMathNode();
            latex.latex(content);
            insertBefore.insertBefore(latex);
            insertBefore = latex;

            lastEnd = m.end();
            replaced = true;
        }

        if (replaced) {
            // Remaining text after last match
            if (lastEnd < text.length()) {
                Text after = new Text(text.substring(lastEnd));
                textNode.insertBefore(after);
            }
            textNode.unlink();
        }
    }
}
