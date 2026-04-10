package io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import io.noties.prism4j.Prism4j;
import static io.noties.prism4j.Prism4j.*;
import static java.util.regex.Pattern.*;

public class Prism_ruby {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("ruby",
            token("comment", pattern(compile("#.*", MULTILINE))),
            token("string",
                pattern(compile("\"(?:\\\\[\\s\\S]|#\\{[^}]+\\}|[^\"\\\\])*\"")),
                pattern(compile("'(?:\\\\[\\s\\S]|[^'\\\\])*'"))),
            token("symbol", pattern(compile(":[a-zA-Z_]\\w*"))),
            token("regex", pattern(compile("/(?:\\\\.|[^/\\\\\\n])+/[gimsux]*"))),
            token("keyword", pattern(compile("\\b(?:alias|and|begin|break|case|class|def|defined\\?|do|else|elsif|end|ensure|false|for|if|in|module|next|nil|not|or|raise|redo|require|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield|__FILE__|__LINE__|__ENCODING__|attr_accessor|attr_reader|attr_writer|include|extend|prepend|private|protected|public|puts|print|p|gets|chomp|each|map|select|reject|reduce|inject|collect|detect|any\\?|all\\?|none\\?|freeze|frozen\\?|lambda|proc|block_given\\?)\\b"))),
            token("builtin", pattern(compile("\\b(?:Array|Hash|String|Integer|Float|Symbol|NilClass|TrueClass|FalseClass|Regexp|Range|IO|File|Dir|Time|Struct|Comparable|Enumerable|Kernel|Object|BasicObject|Module|Class|Numeric|Complex|Rational)\\b"))),
            token("boolean", pattern(compile("\\b(?:true|false|nil)\\b"))),
            token("number", pattern(compile("\\b0x[\\da-fA-F]+\\b|\\b\\d[\\d_]*(?:\\.[\\d_]+)?(?:[eE][+-]?\\d+)?\\b"))),
            token("operator", pattern(compile("=>|<=>|&&|\\|\\||<<|>>|[!=<>]=?|[+\\-*/%&|^~]|\\.\\.\\??"))),
            token("punctuation", pattern(compile("[{}()\\[\\];.,]")))
        );
    }
}
