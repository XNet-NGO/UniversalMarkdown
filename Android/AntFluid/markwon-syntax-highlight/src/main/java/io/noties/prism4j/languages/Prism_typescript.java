package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_typescript {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("typescript",
            token("comment", pattern(compile("//.*|/\\*[\\s\\S]*?\\*/"))),
            token("string", pattern(compile("([\"'`])(?:[^\\\\]|\\\\.)*?\\1"))),
            token("keyword", pattern(compile("\\b(?:abstract|as|async|await|break|case|catch|class|const|continue|debugger|declare|default|delete|do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in|instanceof|interface|is|keyof|let|module|namespace|new|of|package|private|protected|public|readonly|return|require|set|static|super|switch|this|throw|try|type|typeof|undefined|var|void|while|with|yield)\\b"))),
            token("boolean", pattern(compile("\\b(?:true|false)\\b"))),
            token("number", pattern(compile("\\b(?:0x[\\da-fA-F]+|0o[0-7]+|0b[01]+|NaN|Infinity|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b"))),
            token("builtin", pattern(compile("\\b(?:string|number|boolean|symbol|any|never|unknown|void|null|undefined|object|Array|Promise|Record|Partial|Required|Readonly|Pick|Omit|Exclude|Extract|Map|Set)\\b"))),
            token("function", pattern(compile("\\b[a-zA-Z_$]\\w*(?=\\s*[(<])"))),
            token("operator", pattern(compile("[-+*/%=!<>&|^~?:]+|=>|\\.{3}"))),
            token("punctuation", pattern(compile("[{}\\[\\];(),.]+")))
        );
    }
}
