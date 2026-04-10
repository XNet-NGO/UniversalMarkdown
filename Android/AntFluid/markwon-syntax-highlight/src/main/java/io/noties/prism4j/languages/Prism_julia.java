package io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import io.noties.prism4j.Prism4j;
import static io.noties.prism4j.Prism4j.*;
import static java.util.regex.Pattern.*;

public class Prism_julia {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("julia",
            token("comment", pattern(compile("#=.*?=#|#.*", MULTILINE))),
            token("string",
                pattern(compile("\"\"\"[\\s\\S]*?\"\"\"")),
                pattern(compile("\"(?:\\\\[\\s\\S]|[^\"\\\\])*\""))),
            token("keyword", pattern(compile("\\b(?:abstract|baremodule|begin|break|catch|const|continue|do|else|elseif|end|export|finally|for|function|global|if|import|in|let|local|macro|module|mutable|outer|primitive|quote|return|struct|try|type|typealias|using|where|while)\\b"))),
            token("builtin", pattern(compile("\\b(?:Int8|Int16|Int32|Int64|Int128|UInt8|UInt16|UInt32|UInt64|UInt128|Float16|Float32|Float64|Bool|Char|String|Symbol|Nothing|Missing|Any|Union|Tuple|NamedTuple|Array|Vector|Matrix|Dict|Set|Pair|Ref|Regex|IO|println|print|typeof|sizeof|length|push!|pop!|map|filter|reduce|collect|enumerate|zip|sort|sort!|unique|sum|prod|minimum|maximum|abs|sqrt|log|exp|sin|cos|rand|randn|zeros|ones|fill|range|LinRange)\\b"))),
            token("boolean", pattern(compile("\\b(?:true|false|nothing|missing)\\b"))),
            token("number", pattern(compile("\\b0x[\\da-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?(?:im)?\\b"))),
            token("operator", pattern(compile("=>|->|<:|>:|::|\\.\\.\\.|[.]=|[!=<>]=?|&&|\\|\\||[+\\-*/%^÷\\\\&|~⊻]"))),
            token("punctuation", pattern(compile("[{}()\\[\\];.,@]")))
        );
    }
}
