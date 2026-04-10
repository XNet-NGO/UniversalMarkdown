package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_lua {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("lua",
            token("comment", pattern(compile("--(?:\\[=*\\[[\\s\\S]*?\\]=*\\]|.*)"))),
            token("string", pattern(compile("([\"'])(?:[^\\\\]|\\\\.)*?\\1|\\[=*\\[[\\s\\S]*?\\]=*\\]"))),
            token("keyword", pattern(compile("\\b(?:and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while)\\b"))),
            token("function", pattern(compile("\\b[a-zA-Z_]\\w*(?=\\s*[({])"))),
            token("number", pattern(compile("\\b(?:0x[\\da-fA-F]+(?:\\.[\\da-fA-F]+)?(?:[pP][+-]?\\d+)?|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b"))),
            token("operator", pattern(compile("[-+*/%^#=<>~]+|\\.{2,3}"))),
            token("punctuation", pattern(compile("[{}\\[\\];(),.:]+")))
        );
    }
}
