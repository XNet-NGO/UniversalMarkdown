package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.MULTILINE;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_hcl {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("hcl",
            token("comment", pattern(compile("#.*|//.*|/\\*[\\s\\S]*?\\*/"))),
            token("string", pattern(compile("\"(?:[^\"\\\\]|\\\\.)*\""))),
            token("keyword", pattern(compile("\\b(?:resource|data|variable|output|locals|module|provider|terraform|backend|required_providers|required_version|for_each|count|depends_on|lifecycle|provisioner|connection|dynamic|for|in|if)\\b"))),
            token("boolean", pattern(compile("\\b(?:true|false|null)\\b"))),
            token("property", pattern(compile("\\b[\\w-]+(?=\\s*=)"))),
            token("number", pattern(compile("\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"))),
            token("function", pattern(compile("\\b[a-z_]\\w*(?=\\s*\\()"))),
            token("variable", pattern(compile("\\b(?:var|local|data|module|each|count|self|path|terraform)\\.[\\w.]+"))),
            token("punctuation", pattern(compile("[{}\\[\\]=(),.:]+")))
        );
    }
}
