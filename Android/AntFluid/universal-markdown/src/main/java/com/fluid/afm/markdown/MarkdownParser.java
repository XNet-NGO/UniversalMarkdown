package com.fluid.afm.markdown;

import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fluid.afm.ContextHolder;
import com.fluid.afm.StreamOutStateObserver;
import com.fluid.afm.markdown.code.CodeBlockPlugin;
import com.fluid.afm.markdown.widget.PrinterMarkDownTextView;
import com.fluid.afm.styles.MarkdownStyles;
import com.fluid.afm.utils.MDLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonPlugin;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.inlineparser.MarkwonInlineParser;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public class MarkdownParser {

    public static final String TAG = "MarkdownParser";
    private Markwon.Builder mMarkwonBuilder;
    private Markwon mMarkwon;
    private final PrinterMarkDownTextView mTextView;
    private final List<StreamOutStateObserver> mStreamOutStateObservers = new ArrayList<>();
    private MarkdownStyles mProductStyles;
    private MarkwonTheme mMarkwonTheme;

    public MarkdownParser(Context context, List<AbstractMarkwonPlugin> plugins, PrinterMarkDownTextView textView, MarkdownStyles styles) {
        this.mTextView = textView;
        mProductStyles = styles;
        init(context, plugins);
    }

    public void updateMarkdownStyles(MarkdownStyles styles) {
        if (styles == null) {
            return;
        }
        mProductStyles = styles;
        if (mMarkwonTheme != null) {
            mMarkwonTheme.updateStyles(styles);
        }
        if (mTextView == null) {
            return;
        }
        updateTextViewStyle(styles);
        mTextView.setText(mTextView.getText());
    }

    private void updateTextViewStyle(MarkdownStyles styles) {
        if (styles == null) {
            return;
        }
        if (styles.paragraphStyle().fontSize() > 0) {
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, styles.paragraphStyle().fontSize());
        }
        if (styles.paragraphStyle().lineHeight() > 0) {
            mTextView.setLineSpacing(0, 1.f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mTextView.setFallbackLineSpacing(false);
            }
            Paint.FontMetrics fontMetrics = mTextView.getPaint().getFontMetrics();
            mTextView.setLineSpacing(styles.paragraphStyle().lineHeight() - (fontMetrics.bottom- fontMetrics.top), 1f);
        }
        if (styles.paragraphStyle().fontColor() != 0) {
            mTextView.setTextColor(styles.paragraphStyle().fontColor());
        }
    }

    public void setPrintingState(boolean isPrinting) {
        try {
            for (StreamOutStateObserver observer : mStreamOutStateObservers) {
                observer.onStreamOutStateChanged(isPrinting);
            }
        } catch (Exception e) {
            MDLogger.e(TAG, "delieverIsAnimationFinish error", e);
        }
    }

    private void init(final Context context, List<AbstractMarkwonPlugin> customPlugins) {
        if(ContextHolder.getContext() == null) {
            ContextHolder.setContext(context.getApplicationContext());
        }
        List<MarkwonPlugin> plugins = new ArrayList<>();
        if (customPlugins != null) {
            plugins.addAll(customPlugins);
        }
        for (Object obj : plugins) {
            if (obj instanceof StreamOutStateObserver) {
                mStreamOutStateObservers.add((StreamOutStateObserver) obj);
            }
        }
        plugins.addAll(getDefaultPlugins(context));
        mMarkwonBuilder = Markwon.builderWithPlugs(context, plugins);
        mMarkwonTheme = MarkwonTheme.emptyBuilder().setStyles(mProductStyles).setTextView(mTextView).build(plugins);
        mMarkwonBuilder.setMarkdownTheme(mMarkwonTheme);
        mMarkwon = mMarkwonBuilder.build();
        updateTextViewStyle(mProductStyles);
    }

    private ArrayList<AbstractMarkwonPlugin> getDefaultPlugins(final Context context) {
        long startTime = SystemClock.elapsedRealtime();
        ArrayList<AbstractMarkwonPlugin> plugins = new ArrayList<>(4);
        plugins.add(CorePlugin.create(context));
        plugins.add(CodeBlockPlugin.create(context, true));
        plugins.add(new AbstractMarkwonPlugin() {

            @NonNull
            @Override
            public String processMarkdown(@NonNull String markdown) {
                String processed = markdown;
                if (io.noties.markwon.ext.latex.LatexDocumentTranspiler.isLatexDocument(processed)) {
                    processed = io.noties.markwon.ext.latex.LatexDocumentTranspiler.transpile(processed);
                }
                // Wrap inline LaTeX document snippets in code fences
                processed = wrapInlineLatexDocs(processed);
                // Escape < and > outside code fences to prevent HTML parsing
                processed = escapeHtmlOutsideCode(processed);
                // Escape lone * that aren't bold/italic markers (e.g., "a * b" in math)
                processed = escapeLoneAsterisks(processed);
                // Replace Unicode arrows/quotes that may not render in all span typefaces
                processed = processed.replace("\u2192", " -> ");  // →
                processed = processed.replace("\u2190", " <- ");  // ←
                processed = processed.replace("\u2194", " <-> "); // ↔
                processed = processed.replace("\u21D2", " => ");  // ⇒
                processed = processed.replace("\u2019", "'");     // '
                processed = processed.replace("\u2018", "'");     // '
                processed = processed.replace("\u201C", "\"");    // "
                processed = processed.replace("\u201D", "\"");    // "
                processed = processed.replace("\u2013", "-");     // –
                processed = processed.replace("\u2014", "--");    // —
                String regex = "\\${1,2}\\s*\\\\bm\\{([A-Za-z]{1,9})\\}\\s*\\${1,2}";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(processed);
                StringBuffer output = new StringBuffer();
                while (matcher.find()) {
                    String content = matcher.group(1);
                    matcher.appendReplacement(output, "***" + content + "***");
                }
                matcher.appendTail(output);
                return super.processMarkdown(output.toString());
            }

            private String escapeLoneAsterisks(String input) {
                // Escape * that has space on both sides (literal, not emphasis)
                // "a * b" → "a \* b", but "**bold**" and "*italic*" stay
                StringBuilder result = new StringBuilder();
                boolean inFence = false;
                for (String line : input.split("\n", -1)) {
                    if (line.trim().startsWith("```")) inFence = !inFence;
                    if (inFence) {
                        result.append(line);
                    } else {
                        result.append(line.replaceAll("(?<= )\\*(?= )", "\\\\*"));
                    }
                    result.append("\n");
                }
                return result.toString();
            }

            private String escapeHtmlOutsideCode(String input) {
                StringBuilder result = new StringBuilder();
                boolean inFence = false;
                boolean inInlineCode = false;
                for (String line : input.split("\n", -1)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("```")) inFence = !inFence;
                    if (inFence) {
                        result.append(line);
                    } else {
                        // Escape < and > that look like HTML tags but aren't in code
                        // Keep markdown-valid HTML like <br>, <sup>, <sub>, <mark>, <del>, <ins>
                        String escaped = line;
                        // Replace < > that form unknown tags with entities
                        escaped = escaped.replaceAll("<(?!/?(?:br|sup|sub|mark|del|ins|a |a>|em|strong|code|pre|ul|ol|li|p|h[1-6]|table|tr|td|th|thead|tbody|hr|blockquote|img ))", "&lt;");
                        result.append(escaped);
                    }
                    result.append("\n");
                }
                return result.toString();
            }

            private String wrapInlineLatexDocs(String input) {
                // Extract fenced code blocks, process only non-fenced content
                java.util.List<String> fences = new java.util.ArrayList<>();
                String placeholder = "\u0000FENCE%d\u0000";
                java.util.regex.Matcher fm = java.util.regex.Pattern.compile("```[\\s\\S]*?```").matcher(input);
                StringBuffer stripped = new StringBuffer();
                int idx = 0;
                while (fm.find()) {
                    fences.add(fm.group());
                    fm.appendReplacement(stripped, String.format(placeholder, idx++));
                }
                fm.appendTail(stripped);
                // Wrap \documentclass...\end{document} only in non-fenced content
                String processed = stripped.toString().replaceAll(
                    "(\\\\documentclass[\\s\\S]*?\\\\end\\{document\\})",
                    "\n```latex\n$1\n```\n");
                // Restore fenced blocks
                for (int i = 0; i < fences.size(); i++) {
                    processed = processed.replace(String.format(placeholder, i), fences.get(i));
                }
                return processed;
            }

        });
        plugins.add(MarkwonInlineParserPlugin.create(MarkwonInlineParser.factoryBuilder()));
        MDLogger.d(TAG, "addPlugin costTime=" + (SystemClock.elapsedRealtime() - startTime));
        return plugins;
    }

    public TextView getTextView() {
        return mTextView;
    }

    public void setTextSetter(Markwon.TextSetter setter) {
        mMarkwonBuilder.textSetter(setter);
    }

    public Markwon getMarkwon() {
        return mMarkwon;
    }
}