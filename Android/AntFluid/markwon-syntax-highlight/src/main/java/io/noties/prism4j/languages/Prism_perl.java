package io.noties.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static io.noties.prism4j.Prism4j.grammar;
import static io.noties.prism4j.Prism4j.pattern;
import static io.noties.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import io.noties.prism4j.Prism4j;

public class Prism_perl {
    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("perl",
            token("comment", pattern(compile("#.*|^=\\w[\\s\\S]*?^=cut\\b"))),
            token("string", pattern(compile("([\"'])(?:[^\\\\]|\\\\.)*?\\1|q[qwx]?(?:\\([^)]*\\)|\\{[^}]*\\}|\\[[^\\]]*\\]|<[^>]*>|/[^/]*/|![^!]*!)"))),
            token("regex", pattern(compile("(?:m|qr)?/(?:[^/\\\\]|\\\\.)+/[msixpodualngcer]*"))),
            token("keyword", pattern(compile("\\b(?:BEGIN|END|AUTOLOAD|DESTROY|__DATA__|__END__|__FILE__|__LINE__|__PACKAGE__|chomp|chop|defined|delete|die|do|each|else|elsif|eval|exists|for|foreach|given|goto|grep|if|keys|last|local|map|my|next|no|our|package|pop|pos|print|printf|push|redo|require|return|reverse|say|shift|sort|splice|split|sub|tie|tied|undef|unless|unshift|untie|until|use|values|warn|when|while|wantarray)\\b"))),
            token("variable", pattern(compile("[&*$@%]\\{?\\w+\\}?|\\$\\{?[^\\s{}]+\\}?"))),
            token("function", pattern(compile("\\b\\w+(?=\\s*\\()"))),
            token("number", pattern(compile("\\b(?:0x[\\da-fA-F_]+|0b[01_]+|0[0-7_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[eE][+-]?\\d+)?)\\b"))),
            token("operator", pattern(compile("-[rwxoRWXOezsfdlpSbctugkTBMAC]\\b|[-+*/%=!<>&|^~.]=?|\\b(?:and|cmp|eq|ge|gt|le|lt|ne|not|or|x|xor)\\b"))),
            token("punctuation", pattern(compile("[{}\\[\\];(),.:]+")))
        );
    }
}
