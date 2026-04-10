package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.MULTILINE;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_dockerfile {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("dockerfile",
            token("comment", pattern(compile("#.*"))),
            token("string", pattern(compile("([\"'])(?:[^\\\\]|\\\\.)*?\\1"))),
            token("keyword", pattern(compile("^\\s*(?:FROM|RUN|CMD|LABEL|MAINTAINER|EXPOSE|ENV|ADD|COPY|ENTRYPOINT|VOLUME|USER|WORKDIR|ARG|ONBUILD|STOPSIGNAL|HEALTHCHECK|SHELL)\\b", MULTILINE))),
            token("variable", pattern(compile("\\$\\{?[\\w.]+\\}?"))),
            token("punctuation", pattern(compile("[\\\\=]")))
        );
    }
}
