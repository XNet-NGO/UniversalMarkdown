package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.MULTILINE;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_diff {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("diff",
            token("inserted", pattern(compile("^\\+.*$", MULTILINE))),
            token("deleted", pattern(compile("^-.*$", MULTILINE))),
            token("comment", pattern(compile("^[@@].*[@@]$", MULTILINE))),
            token("keyword", pattern(compile("^(?:diff|index|---|\\.\\.\\.)[\\s\\S]*$", MULTILINE)))
        );
    }
}
