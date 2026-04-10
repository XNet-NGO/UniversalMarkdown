package io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import io.noties.prism4j.Prism4j;
import static io.noties.prism4j.Prism4j.*;
import static java.util.regex.Pattern.*;

public class Prism_php {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("php",
            token("comment",
                pattern(compile("/\\*[\\s\\S]*?\\*/")),
                pattern(compile("(?://|#).*", MULTILINE))),
            token("string",
                pattern(compile("\"(?:\\\\[\\s\\S]|\\$\\w+|[^\"\\\\$])*\"")),
                pattern(compile("'(?:\\\\[\\s\\S]|[^'\\\\])*'"))),
            token("variable", pattern(compile("\\$\\w+"))),
            token("keyword", pattern(compile("\\b(?:abstract|and|array|as|break|callable|case|catch|class|clone|const|continue|declare|default|die|do|echo|else|elseif|empty|enddeclare|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|final|finally|fn|for|foreach|function|global|goto|if|implements|include|include_once|instanceof|insteadof|interface|isset|list|match|namespace|new|or|print|private|protected|public|readonly|require|require_once|return|static|switch|throw|trait|try|unset|use|var|while|xor|yield|yield\\s+from|__CLASS__|__DIR__|__FILE__|__FUNCTION__|__LINE__|__METHOD__|__NAMESPACE__|__TRAIT__)\\b"))),
            token("builtin", pattern(compile("\\b(?:true|false|null|self|parent|static|int|float|string|bool|void|never|mixed|array|object|callable|iterable|null)\\b", CASE_INSENSITIVE))),
            token("number", pattern(compile("\\b0x[\\da-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"))),
            token("operator", pattern(compile("=>|->|\\?->|\\?\\.|\\.\\.\\.|\\.=|<=>|&&|\\|\\||\\?\\?=?|[!=<>]=?|[+\\-*/%&|^~]|\\*\\*"))),
            token("punctuation", pattern(compile("[{}()\\[\\];.,:]")))
        );
    }
}
