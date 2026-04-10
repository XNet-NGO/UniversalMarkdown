package io.noties.markwon.ext.latex;

import androidx.annotation.NonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transpiles full LaTeX documents to Markdown, preserving math expressions
 * for JLatexMath rendering. Handles \documentclass, \section, \textbf, etc.
 */
public class LatexDocumentTranspiler {

    private static final Pattern PREAMBLE = Pattern.compile(
        "(?s).*?\\\\begin\\{document\\}\\s*");
    private static final Pattern END_DOC = Pattern.compile(
        "\\\\end\\{document\\}.*$");

    public static boolean isLatexDocument(@NonNull String text) {
        return text.contains("\\documentclass") || text.contains("\\begin{document}");
    }

    @NonNull
    public static String transpile(@NonNull String latex) {
        if (!isLatexDocument(latex)) return latex;

        String s = latex;
        // Strip preamble and \end{document}
        s = PREAMBLE.matcher(s).replaceFirst("");
        s = END_DOC.matcher(s).replaceFirst("");

        // Sections
        s = replaceCmd(s, "\\\\chapter\\{([^}]*)\\}", "\n# $1\n");
        s = replaceCmd(s, "\\\\section\\{([^}]*)\\}", "\n## $1\n");
        s = replaceCmd(s, "\\\\subsection\\{([^}]*)\\}", "\n### $1\n");
        s = replaceCmd(s, "\\\\subsubsection\\{([^}]*)\\}", "\n#### $1\n");
        s = replaceCmd(s, "\\\\paragraph\\{([^}]*)\\}", "\n**$1** ");

        // Text formatting
        s = replaceCmd(s, "\\\\textbf\\{([^}]*)\\}", "**$1**");
        s = replaceCmd(s, "\\\\textit\\{([^}]*)\\}", "*$1*");
        s = replaceCmd(s, "\\\\emph\\{([^}]*)\\}", "*$1*");
        s = replaceCmd(s, "\\\\underline\\{([^}]*)\\}", "$1");
        s = replaceCmd(s, "\\\\texttt\\{([^}]*)\\}", "`$1`");
        s = replaceCmd(s, "\\\\text\\{([^}]*)\\}", "$1");
        s = replaceCmd(s, "\\\\textrm\\{([^}]*)\\}", "$1");
        s = replaceCmd(s, "\\\\textsf\\{([^}]*)\\}", "$1");
        s = replaceCmd(s, "\\\\textsc\\{([^}]*)\\}", "$1");
        s = replaceCmd(s, "\\\\footnote\\{([^}]*)\\}", " ($1)");

        // Environments — itemize
        s = s.replaceAll("\\\\begin\\{itemize\\}", "");
        s = s.replaceAll("\\\\end\\{itemize\\}", "");
        s = replaceCmd(s, "\\\\item\\s*", "\n- ");

        // Environments — enumerate
        s = s.replaceAll("\\\\begin\\{enumerate\\}", "");
        s = s.replaceAll("\\\\end\\{enumerate\\}", "");
        // \item in enumerate context already handled above as "- "

        // Environments — verbatim
        s = s.replaceAll("\\\\begin\\{verbatim\\}", "\n```\n");
        s = s.replaceAll("\\\\end\\{verbatim\\}", "\n```\n");
        s = s.replaceAll("\\\\begin\\{lstlisting\\}(\\[[^]]*\\])?", "\n```\n");
        s = s.replaceAll("\\\\end\\{lstlisting\\}", "\n```\n");

        // Environments — quote/quotation
        s = s.replaceAll("\\\\begin\\{quote\\}", "\n> ");
        s = s.replaceAll("\\\\end\\{quote\\}", "\n");
        s = s.replaceAll("\\\\begin\\{quotation\\}", "\n> ");
        s = s.replaceAll("\\\\end\\{quotation\\}", "\n");

        // Environments — center/flushleft/flushright
        s = s.replaceAll("\\\\begin\\{center\\}", "");
        s = s.replaceAll("\\\\end\\{center\\}", "");
        s = s.replaceAll("\\\\begin\\{flushleft\\}", "");
        s = s.replaceAll("\\\\end\\{flushleft\\}", "");
        s = s.replaceAll("\\\\begin\\{flushright\\}", "");
        s = s.replaceAll("\\\\end\\{flushright\\}", "");

        // Environments — abstract
        s = s.replaceAll("\\\\begin\\{abstract\\}", "\n**Abstract**\n\n> ");
        s = s.replaceAll("\\\\end\\{abstract\\}", "\n");

        // Environments — figure
        s = s.replaceAll("\\\\begin\\{figure\\}(\\[[^]]*\\])?", "");
        s = s.replaceAll("\\\\end\\{figure\\}", "");
        s = replaceCmd(s, "\\\\caption\\{([^}]*)\\}", "\n*$1*\n");
        s = replaceCmd(s, "\\\\label\\{([^}]*)\\}", "");
        s = replaceCmd(s, "\\\\includegraphics(\\[[^]]*\\])?\\{([^}]*)\\}", "![]($2)");

        // Tables — tabular
        s = transpileTabular(s);

        // Horizontal rules
        s = s.replaceAll("\\\\hrule", "\n---\n");
        s = s.replaceAll("\\\\hline", "");

        // Line breaks and spacing
        s = s.replaceAll("\\\\\\\\", "\n");
        s = s.replaceAll("\\\\newline", "\n");
        s = s.replaceAll("\\\\newpage", "\n---\n");
        s = s.replaceAll("\\\\clearpage", "\n---\n");
        s = s.replaceAll("\\\\par\\b", "\n\n");
        s = s.replaceAll("\\\\noindent\\b", "");
        s = s.replaceAll("\\\\vspace\\{[^}]*\\}", "\n");
        s = s.replaceAll("\\\\hspace\\{[^}]*\\}", " ");
        s = s.replaceAll("\\\\bigskip", "\n\n");
        s = s.replaceAll("\\\\medskip", "\n");
        s = s.replaceAll("\\\\smallskip", "\n");

        // Title/author/date
        s = replaceCmd(s, "\\\\title\\{([^}]*)\\}", "\n# $1\n");
        s = replaceCmd(s, "\\\\author\\{([^}]*)\\}", "\n*$1*\n");
        s = replaceCmd(s, "\\\\date\\{([^}]*)\\}", "\n*$1*\n");
        s = s.replaceAll("\\\\maketitle", "");

        // References
        s = replaceCmd(s, "\\\\ref\\{([^}]*)\\}", "[$1]");
        s = replaceCmd(s, "\\\\cite\\{([^}]*)\\}", "[$1]");
        s = replaceCmd(s, "\\\\href\\{([^}]*)\\}\\{([^}]*)\\}", "[$2]($1)");
        s = replaceCmd(s, "\\\\url\\{([^}]*)\\}", "$1");

        // Strip remaining unknown commands with arguments
        s = replaceCmd(s, "\\\\[a-zA-Z]+\\{([^}]*)\\}", "$1");
        // Strip remaining commands without arguments
        s = s.replaceAll("\\\\[a-zA-Z]+\\b", "");

        // Clean up: collapse multiple blank lines
        s = s.replaceAll("\n{3,}", "\n\n");
        return s.trim();
    }

    private static String replaceCmd(String s, String regex, String replacement) {
        return Pattern.compile(regex).matcher(s).replaceAll(replacement);
    }

    @NonNull
    static String transpileTabular(@NonNull String input) {
        Pattern tabularPattern = Pattern.compile(
            "(?s)\\\\begin\\{tabular\\}\\{([^}]*)\\}(.*?)\\\\end\\{tabular\\}");
        Matcher m = tabularPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String colSpec = m.group(1);
            String body = m.group(2);
            String mdTable = convertTabularToMarkdown(colSpec, body);
            m.appendReplacement(sb, Matcher.quoteReplacement(mdTable));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String convertTabularToMarkdown(String colSpec, String body) {
        // Count columns from spec (l, c, r, p{})
        int cols = 0;
        for (char c : colSpec.toCharArray()) {
            if (c == 'l' || c == 'c' || c == 'r' || c == 'p') cols++;
        }
        if (cols == 0) cols = 1;

        // Split rows by \\
        String cleaned = body.replaceAll("\\\\hline", "").trim();
        String[] rows = cleaned.split("\\\\\\\\");

        StringBuilder table = new StringBuilder("\n");
        boolean headerDone = false;
        for (String row : rows) {
            row = row.trim();
            if (row.isEmpty()) continue;
            String[] cells = row.split("&");
            table.append("|");
            for (int i = 0; i < cols; i++) {
                String cell = i < cells.length ? cells[i].trim() : "";
                table.append(" ").append(cell).append(" |");
            }
            table.append("\n");
            if (!headerDone) {
                table.append("|");
                for (int i = 0; i < cols; i++) table.append("---|");
                table.append("\n");
                headerDone = true;
            }
        }
        return table.toString();
    }
}
