package io.noties.markwon.ext.latex;

import androidx.annotation.Nullable;

import org.commonmark.node.Node;

import java.util.regex.Pattern;

import io.noties.markwon.inlineparser.InlineProcessor;

/**
 * Inline math processor: handles single $...$ only.
 * Block $$...$$ is handled by JLatexMathBlockParser.
 *
 * Constraints to avoid false positives:
 * - No newlines (inline only)
 * - No box-drawing chars (U+2500-U+259F)
 * - Must not start/end with space
 * - Content must contain at least one math-like character
 * - Content must not look like a regex pattern
 * - Must not be preceded/followed by another $ (avoids $$)
 *
 * @since 4.3.0
 */
class JLatexMathInlineProcessor extends InlineProcessor {

    private static final Pattern RE = Pattern.compile(
        "(?<!\\$)\\$([^$\\n\\u2500-\\u259F]+?)\\$(?!\\$)");

    private static final Pattern MATH_CHARS = Pattern.compile(
        "[\\\\^_{}+=/<>\\d]|\\\\[a-zA-Z]");

    // Reject regex patterns: (?:, [^, \s, \w, \d, {n,m}
    private static final Pattern REGEX_CHARS = Pattern.compile(
        "\\(\\?[:|!=<]|\\[\\^|\\\\[swdSWD]|\\{\\d+,");

    @Override
    public char specialCharacter() {
        return '$';
    }

    @Nullable
    @Override
    protected Node parse() {
        final String latex = match(RE);
        if (latex == null) {
            return null;
        }

        String content = latex.substring(1, latex.length() - 1);

        if (content.startsWith(" ") || content.endsWith(" ")) {
            return null;
        }

        if (!MATH_CHARS.matcher(content).find()) {
            return null;
        }

        if (REGEX_CHARS.matcher(content).find()) {
            return null;
        }

        final JLatexMathNode node = new JLatexMathNode();
        node.latex(content);
        return node;
    }
}
