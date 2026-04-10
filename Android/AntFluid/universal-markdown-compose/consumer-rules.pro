# fluid-markdown-compose consumer proguard rules

# Keep CommonMark extension node classes accessed via reflection
-keep class org.commonmark.ext.gfm.tables.** { *; }
-keep class org.commonmark.ext.gfm.strikethrough.** { *; }
-keep class io.noties.markwon.ext.latex.JLatexMathNode { *; }
-keep class io.noties.markwon.ext.latex.JLatexMathBlock { *; }
-keep class io.noties.markwon.ext.tasklist.TaskListItem { *; }

# Keep JLatexMath rendering classes
-keep class ru.noties.jlatexmath.** { *; }

# Keep Prism4j grammar classes (loaded by name)
-keep class io.noties.prism4j.languages.** { *; }
