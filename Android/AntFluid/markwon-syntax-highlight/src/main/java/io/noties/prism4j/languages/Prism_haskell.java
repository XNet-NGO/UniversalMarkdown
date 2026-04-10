package io.noties.prism4j.languages;

import androidx.annotation.NonNull;
import io.noties.prism4j.Prism4j;
import static io.noties.prism4j.Prism4j.*;
import static java.util.regex.Pattern.*;

public class Prism_haskell {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("haskell",
            token("comment",
                pattern(compile("\\{-[\\s\\S]*?-\\}")),
                pattern(compile("--.*", MULTILINE))),
            token("string",
                pattern(compile("\"(?:\\\\[\\s\\S]|[^\"\\\\])*\"")),
                pattern(compile("'(?:\\\\.|[^'\\\\])+'"))),
            token("keyword", pattern(compile("\\b(?:as|case|class|data|default|deriving|do|else|forall|foreign|hiding|if|import|in|infix|infixl|infixr|instance|let|module|newtype|of|qualified|then|type|where|_)\\b"))),
            token("builtin", pattern(compile("\\b(?:Int|Integer|Float|Double|Char|String|Bool|IO|Maybe|Either|Ordering|Show|Read|Eq|Ord|Num|Functor|Monad|Applicative|Foldable|Traversable|True|False|Just|Nothing|Left|Right|map|filter|foldl|foldr|head|tail|take|drop|zip|unzip|putStrLn|print|return|pure)\\b"))),
            token("operator", pattern(compile("=>|->|<-|::|\\.\\.|\\.|\\\\/|/\\\\|[=!<>]=?|[+\\-*/%$&|^~?@#]+"  ))),
            token("number", pattern(compile("\\b0x[\\da-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"))),
            token("punctuation", pattern(compile("[{}()\\[\\];,`]")))
        );
    }
}
