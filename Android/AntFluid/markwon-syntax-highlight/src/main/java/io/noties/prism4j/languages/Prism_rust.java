package io.noties.prism4j.languages;

import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_rust {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("rust",
            token("comment", pattern(compile("//.*|/\\*[\\s\\S]*?\\*/"))),
            token("string", pattern(compile("b?\"(?:[^\"\\\\]|\\\\.)*\"|b?r#*\"[\\s\\S]*?\"#*"))),
            token("char", pattern(compile("b?'(?:[^'\\\\]|\\\\(?:.|x[0-9a-fA-F]{2}|u\\{[0-9a-fA-F]{1,6}\\}))'"))),
            token("attribute", pattern(compile("#!?\\[[\\s\\S]*?\\]"))),
            token("keyword", pattern(compile("\\b(?:as|async|await|break|const|continue|crate|dyn|else|enum|extern|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|type|union|unsafe|use|where|while|yield)\\b"))),
            token("boolean", pattern(compile("\\b(?:true|false)\\b"))),
            token("function", pattern(compile("\\b[a-z_]\\w*(?=\\s*\\()"))),
            token("macro", pattern(compile("\\b\\w+!"))),
            token("number", pattern(compile("\\b(?:0x[0-9a-fA-F_]+|0o[0-7_]+|0b[01_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[eE][+-]?\\d+)?)(?:_?(?:u8|u16|u32|u64|u128|usize|i8|i16|i32|i64|i128|isize|f32|f64))?\\b"))),
            token("punctuation", pattern(compile("[{}\\[\\];(),.:]+"))),
            token("operator", pattern(compile("[-+*/%!^&|<>=@?]+")))
        );
    }
}
