package com.malykbrown.musicwallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Efeitos de imagem para compor o papel de parede:
 *  - centerCrop: recorta para encher o ecrã sem distorcer
 *  - stackBlur: blur rápido por downscale + box blur (CPU, sem RenderScript)
 *  - composeWallpaper: capa nítida centrada sobre fundo desfocado (estilo Apple Music)
 */
object ImageEffects {

    fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        if (w <= 0 || h <= 0) return src
        val scale = max(w.toFloat() / src.width, h.toFloat() / src.height)
        val sw = (src.width * scale).roundToInt()
        val sh = (src.height * scale).roundToInt()
        val scaled = Bitmap.createScaledBitmap(src, sw, sh, true)
        val x = (sw - w) / 2
        val y = (sh - h) / 2
        return Bitmap.createBitmap(scaled, x.coerceAtLeast(0), y.coerceAtLeast(0),
            min(w, sw), min(h, sh))
    }

    /** Blur barato e estável: reduz, aplica box blur em pixels, amplia de volta. */
    fun stackBlur(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return src
        val factor = 8
        val small = Bitmap.createScaledBitmap(
            src, max(1, src.width / factor), max(1, src.height / factor), true)
        val blurred = boxBlur(small, max(1, radius / factor))
        return Bitmap.createScaledBitmap(blurred, src.width, src.height, true)
    }

    private fun boxBlur(src: Bitmap, r: Int): Bitmap {
        val w = src.width; val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)

        // passagem horizontal
        for (y in 0 until h) {
            var sumR = 0; var sumG = 0; var sumB = 0
            val row = y * w
            for (x in -r..r) {
                val p = pixels[row + x.coerceIn(0, w - 1)]
                sumR += Color.red(p); sumG += Color.green(p); sumB += Color.blue(p)
            }
            val div = 2 * r + 1
            for (x in 0 until w) {
                temp[row + x] = Color.rgb(sumR / div, sumG / div, sumB / div)
                val pOut = pixels[row + (x - r).coerceIn(0, w - 1)]
                val pIn = pixels[row + (x + r + 1).coerceIn(0, w - 1)]
                sumR += Color.red(pIn) - Color.red(pOut)
                sumG += Color.green(pIn) - Color.green(pOut)
                sumB += Color.blue(pIn) - Color.blue(pOut)
            }
        }
        // passagem vertical
        for (x in 0 until w) {
            var sumR = 0; var sumG = 0; var sumB = 0
            for (y in -r..r) {
                val p = temp[x + y.coerceIn(0, h - 1) * w]
                sumR += Color.red(p); sumG += Color.green(p); sumB += Color.blue(p)
            }
            val div = 2 * r + 1
            for (y in 0 until h) {
                pixels[x + y * w] = Color.rgb(sumR / div, sumG / div, sumB / div)
                val pOut = temp[x + (y - r).coerceIn(0, h - 1) * w]
                val pIn = temp[x + (y + r + 1).coerceIn(0, h - 1) * w]
                sumR += Color.red(pIn) - Color.red(pOut)
                sumG += Color.green(pIn) - Color.green(pOut)
                sumB += Color.blue(pIn) - Color.blue(pOut)
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Composição do papel de parede. Modos:
     *  "full"   → a capa preenche o ecrã inteiro (center-crop, nítida;
     *             o desfoque só entra se o utilizador subir o slider)
     *  "center" → fundo desfocado + capa nítida centrada (estilo Apple Music)
     */
    fun composeWallpaper(
        art: Bitmap,
        w: Int,
        h: Int,
        blurRadius: Int,
        mode: String,
        scrim: Float = 0.18f
    ): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        if (mode == "full") {
            // Capa em ecrã inteiro: nítida por defeito, desfocada só se pedires
            var full = centerCrop(art, w, h)
            if (blurRadius > 0) full = stackBlur(full, blurRadius)
            canvas.drawBitmap(full, 0f, 0f, paint)
            // scrim mais leve para não matar a arte nítida
            canvas.drawColor(Color.argb((scrim * 0.6f * 255).roundToInt(), 0, 0, 0))
            return out
        }

        // modo "center": fundo desfocado + capa nítida centrada
        val fill = stackBlur(centerCrop(art, w, h), max(30, blurRadius))
        canvas.drawBitmap(fill, 0f, 0f, paint)
        canvas.drawColor(Color.argb((scrim * 255).roundToInt(), 0, 0, 0))

        val side = (w * 0.86f).roundToInt()
        val left = (w - side) / 2
        val top = (h - side) / 2
        val dst = Rect(left, top, left + side, top + side)
        canvas.drawBitmap(art, null, dst, paint)
        return out
    }
}
