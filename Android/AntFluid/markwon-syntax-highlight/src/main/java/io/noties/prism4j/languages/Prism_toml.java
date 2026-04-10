package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.MULTILINE;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_toml {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("toml",
            token("comment", pattern(compile("#.*"))),
            token("string", pattern(compile("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:[^\"\\\\]|\\\\.)*\"|'[^']*'"))),
            token("property", pattern(compile("^\\s*[\\w.-]+(?=\\s*=)", MULTILINE))),
            token("tag", pattern(compile("\\[\\[?[\\w.-]+\\]\\]?"))),
            token("boolean", pattern(compile("\\b(?:true|false)\\b"))),
            token("number", pattern(compile("[+-]?(?:0x[\\da-fA-F_]+|0o[0-7_]+|0b[01_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[eE][+-]?\\d+)?|inf|nan)"))),
            token("punctuation", pattern(compile("[=,\\[\\]{}]")))
        );
    }
}
