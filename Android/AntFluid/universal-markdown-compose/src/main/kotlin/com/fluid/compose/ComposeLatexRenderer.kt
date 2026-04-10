package com.fluid.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ru.noties.jlatexmath.JLatexMathAndroid
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * Renders LaTeX formulas to Bitmap using JLatexMath.
 * Thread-safe: all cache access is synchronized.
 */
object ComposeLatexRenderer {

    @Volatile
    private var initialized = false
    private val cache = LinkedHashMap<String, Bitmap?>(64, 0.75f, true)

    fun init(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    JLatexMathAndroid.init(context)
                    initialized = true
                }
            }
        }
    }

    fun render(
        formula: String,
        textSize: Float = 40f,
        textColor: Color = Color(0xFFE0E0E0),
    ): Bitmap? = synchronized(cache) {
        cache[formula]?.let { return it }
        try {
            val drawable = JLatexMathDrawable.builder(formula)
                .textSize(textSize)
                .color(textColor.toArgb())
                .build()
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            cache[formula] = bmp
            if (cache.size > 200) cache.remove(cache.keys.first())
            bmp
        } catch (_: Exception) {
            cache[formula] = null
            null
        }
    }
}
