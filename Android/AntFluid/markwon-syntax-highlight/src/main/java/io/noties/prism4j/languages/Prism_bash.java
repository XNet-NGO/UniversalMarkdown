package io.noties.prism4j.languages;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.noties.prism4j.GrammarUtils;
import io.noties.prism4j.Prism4j;

import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

public class Prism_bash {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("bash",
            token("comment", pattern(compile("(^|[^\"\\\\$])#.*", MULTILINE), true)),
            token("string", pattern(compile("\"(?:\\\\[\\s\\S]|\\$\\([^)]+\\)|\\$(?!\\()|`[^`]+`|[^\"\\\\`$])*\""))),
            token("variable", pattern(compile("\\$(?:\\w+|\\{[^}]+\\})"))),
            token("function", pattern(compile("\\b(?:alias|bg|bind|break|builtin|caller|cd|command|compgen|complete|continue|declare|dirs|disown|echo|enable|eval|exec|exit|export|false|fc|fg|getopts|hash|help|history|jobs|kill|let|local|logout|mapfile|popd|printf|pushd|pwd|read|readarray|readonly|return|set|shift|shopt|source|suspend|test|times|trap|true|type|typeset|ulimit|umask|unalias|unset|wait)\\b"))),
            token("keyword", pattern(compile("\\b(?:if|then|else|elif|fi|for|while|in|until|do|done|case|esac|function|select|time|coproc)\\b"))),
            token("operator", pattern(compile("&&|\\|\\||[<>]=?|[!=]=|<<-?|>>|[|&;()$]"))),
            token("number", pattern(compile("\\b0x[\\dA-Fa-f]+\\b|(?:\\b\\d+\\.?\\d*|\\B\\.\\d+)(?:[Ee]-?\\d+)?\\b")))
        );
    }
}
