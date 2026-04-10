package io.noties.markwon.syntax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.noties.prism4j.GrammarLocator;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.languages.Prism_bash;
import io.noties.prism4j.languages.Prism_brainfuck;
import io.noties.prism4j.languages.Prism_haskell;
import io.noties.prism4j.languages.Prism_julia;
import io.noties.prism4j.languages.Prism_php;
import io.noties.prism4j.languages.Prism_ruby;
import io.noties.prism4j.languages.Prism_c;
import io.noties.prism4j.languages.Prism_clike;
import io.noties.prism4j.languages.Prism_clojure;
import io.noties.prism4j.languages.Prism_cpp;
import io.noties.prism4j.languages.Prism_csharp;
import io.noties.prism4j.languages.Prism_css;
import io.noties.prism4j.languages.Prism_css_extras;
import io.noties.prism4j.languages.Prism_dart;
import io.noties.prism4j.languages.Prism_diff;
import io.noties.prism4j.languages.Prism_dockerfile;
import io.noties.prism4j.languages.Prism_git;
import io.noties.prism4j.languages.Prism_go;
import io.noties.prism4j.languages.Prism_groovy;
import io.noties.prism4j.languages.Prism_hcl;
import io.noties.prism4j.languages.Prism_java;
import io.noties.prism4j.languages.Prism_javascript;
import io.noties.prism4j.languages.Prism_json;
import io.noties.prism4j.languages.Prism_kotlin;
import io.noties.prism4j.languages.Prism_latex;
import io.noties.prism4j.languages.Prism_lua;
import io.noties.prism4j.languages.Prism_makefile;
import io.noties.prism4j.languages.Prism_markdown;
import io.noties.prism4j.languages.Prism_markup;
import io.noties.prism4j.languages.Prism_perl;
import io.noties.prism4j.languages.Prism_python;
import io.noties.prism4j.languages.Prism_rust;
import io.noties.prism4j.languages.Prism_scala;
import io.noties.prism4j.languages.Prism_sql;
import io.noties.prism4j.languages.Prism_swift;
import io.noties.prism4j.languages.Prism_toml;
import io.noties.prism4j.languages.Prism_typescript;
import io.noties.prism4j.languages.Prism_yaml;

public class BaseCodeGrammar implements GrammarLocator {

    @SuppressWarnings("ConstantConditions")
    private static final Prism4j.Grammar NULL =
            new Prism4j.Grammar() {
                @NonNull
                @Override
                public String name() {
                    return null;
                }
                @NonNull
                @Override
                public List<Prism4j.Token> tokens() {
                    return null;
                }
            };
    private final Map<String, Prism4j.Grammar> cache = new HashMap<>(3);
    @Nullable
    @Override
    public Prism4j.Grammar grammar(@NonNull Prism4j prism4j, @NonNull String language) {
        final String name = realLanguageName(language);
        Prism4j.Grammar grammar = cache.get(name);
        if (grammar != null) {
            if (NULL == grammar) {
                grammar = null;
            }
            return grammar;
        }
        grammar = obtainGrammar(prism4j, name);
        if (grammar == null) {
            cache.put(name, NULL);
        } else {
            cache.put(name, grammar);
            triggerModify(prism4j, name);
        }
        return grammar;
    }
    @NonNull
    protected String realLanguageName(@NonNull String name) {
        final String out;
        switch (name) {
            case "js":
            case "jsx":
                out = "javascript";
                break;
            case "ts":
            case "typescript":
            case "tsx":
                out = "typescript";
                break;
            case "xml":
            case "html":
            case "mathml":
            case "svg":
                out = "markup";
                break;
            case "dotnet":
            case "fsharp":
            case "f#":
            case "ballerina":
            case "bal":
                out = "csharp";
                break;
            case "jsonp":
                out = "json";
                break;
            case "rust":
            case "rs":
                out = "rust";
                break;
            case "zig":
            case "nim":
            case "crystal":
            case "v":
            case "vlang":
            case "carbon":
                out = "c";
                break;
            case "ruby":
            case "rb":
            case "elixir":
            case "ex":
            case "exs":
                out = "ruby";
                break;
            case "bash":
            case "sh":
            case "shell":
            case "zsh":
            case "fish":
            case "powershell":
            case "ps1":
            case "pwsh":
                out = "bash";
                break;
            case "haskell":
            case "hs":
            case "purescript":
                out = "haskell";
                break;
            case "r":
            case "R":
                out = "python";
                break;
            case "julia":
            case "jl":
                out = "julia";
                break;
            case "kt":
                out = "kotlin";
                break;
            case "py":
                out = "python";
                break;
            case "lua":
                out = "lua";
                break;
            case "pl":
            case "perl":
                out = "perl";
                break;
            case "toml":
            case "ini":
            case "cfg":
                out = "toml";
                break;
            case "dockerfile":
            case "docker":
                out = "dockerfile";
                break;
            case "diff":
            case "patch":
                out = "diff";
                break;
            case "hcl":
            case "tf":
            case "terraform":
                out = "hcl";
                break;
            case "yml":
                out = "yaml";
                break;
            case "md":
                out = "markdown";
                break;
            default:
                out = name;
        }
        return out;
    }
    @Nullable
    protected Prism4j.Grammar obtainGrammar(@NonNull Prism4j prism4j, @NonNull String name) {
        final Prism4j.Grammar grammar;
        switch (name) {
            case "brainfuck":
                grammar = Prism_brainfuck.create(prism4j);
                break;
            case "bash":
                grammar = Prism_bash.create(prism4j);
                break;
            case "ruby":
                grammar = Prism_ruby.create(prism4j);
                break;
            case "haskell":
                grammar = Prism_haskell.create(prism4j);
                break;
            case "julia":
                grammar = Prism_julia.create(prism4j);
                break;
            case "php":
                grammar = Prism_php.create(prism4j);
                break;
            case "c":
                grammar = Prism_c.create(prism4j);
                break;
            case "clike":
                grammar = Prism_clike.create(prism4j);
                break;
            case "clojure":
                grammar = Prism_clojure.create(prism4j);
                break;
            case "cpp":
                grammar = Prism_cpp.create(prism4j);
                break;
            case "csharp":
                grammar = Prism_csharp.create(prism4j);
                break;
            case "css":
                grammar = Prism_css.create(prism4j);
                break;
            case "css-extras":
                grammar = Prism_css_extras.create(prism4j);
                break;
            case "dart":
                grammar = Prism_dart.create(prism4j);
                break;
            case "git":
                grammar = Prism_git.create(prism4j);
                break;
            case "go":
                grammar = Prism_go.create(prism4j);
                break;
            case "groovy":
                grammar = Prism_groovy.create(prism4j);
                break;
            case "java":
                grammar = Prism_java.create(prism4j);
                break;
            case "javascript":
                grammar = Prism_javascript.create(prism4j);
                break;
            case "json":
                grammar = Prism_json.create(prism4j);
                break;
            case "kotlin":
                grammar = Prism_kotlin.create(prism4j);
                break;
            case "latex":
                grammar = Prism_latex.create(prism4j);
                break;
            case "makefile":
                grammar = Prism_makefile.create(prism4j);
                break;
            case "markdown":
                grammar = Prism_markdown.create(prism4j);
                break;
            case "markup":
                grammar = Prism_markup.create(prism4j);
                break;
            case "python":
                grammar = Prism_python.create(prism4j);
                break;
            case "scala":
                grammar = Prism_scala.create(prism4j);
                break;
            case "sql":
                grammar = Prism_sql.create(prism4j);
                break;
            case "swift":
                grammar = Prism_swift.create(prism4j);
                break;
            case "yaml":
                grammar = Prism_yaml.create(prism4j);
                break;
            case "rust":
                grammar = Prism_rust.create(prism4j);
                break;
            case "typescript":
                grammar = Prism_typescript.create(prism4j);
                break;
            case "lua":
                grammar = Prism_lua.create(prism4j);
                break;
            case "toml":
                grammar = Prism_toml.create(prism4j);
                break;
            case "dockerfile":
                grammar = Prism_dockerfile.create(prism4j);
                break;
            case "diff":
                grammar = Prism_diff.create(prism4j);
                break;
            case "perl":
                grammar = Prism_perl.create(prism4j);
                break;
            case "hcl":
                grammar = Prism_hcl.create(prism4j);
                break;
            default:
                grammar = null;
        }
        return grammar;
    }
    protected void triggerModify(@NonNull Prism4j prism4j, @NonNull String name) {
        switch (name) {
            case "markup":
                prism4j.grammar("css");
                prism4j.grammar("javascript");
                break;
            case "css":
                prism4j.grammar("css-extras");
                break;
        }
    }
    @Override
    @NonNull
    public Set<String> languages() {
        final Set<String> set = new HashSet<String>(40);
        set.add("bash");
        set.add("brainfuck");
        set.add("diff");
        set.add("dockerfile");
        set.add("haskell");
        set.add("hcl");
        set.add("julia");
        set.add("lua");
        set.add("perl");
        set.add("php");
        set.add("ruby");
        set.add("rust");
        set.add("toml");
        set.add("typescript");
        set.add("c");
        set.add("clike");
        set.add("clojure");
        set.add("cpp");
        set.add("csharp");
        set.add("css");
        set.add("css-extras");
        set.add("dart");
        set.add("git");
        set.add("go");
        set.add("groovy");
        set.add("java");
        set.add("javascript");
        set.add("json");
        set.add("kotlin");
        set.add("latex");
        set.add("makefile");
        set.add("markdown");
        set.add("markup");
        set.add("python");
        set.add("scala");
        set.add("sql");
        set.add("swift");
        set.add("yaml");
        return set;
    }
}
