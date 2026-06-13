package com.malykbrown.musicwallpaper

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live wallpaper: desenha a capa do álbum actual e faz crossfade
 * suave (~900 ms) sempre que a música muda.
 */
class MusicWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MusicEngine()

    inner class MusicEngine : Engine() {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var collectJob: Job? = null

        private var current: Bitmap? = null   // frame totalmente visível
        private var incoming: Bitmap? = null  // frame a entrar (crossfade)
        private var fade = 1f                 // 0 → incoming invisível, 1 → completo
        private var animator: ValueAnimator? = null

        private var surfaceW = 0
        private var surfaceH = 0
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceW = width
            surfaceH = height
            // Recompõe a arte actual para o novo tamanho
            NowPlaying.state.value.art?.let { setNewArt(it, animate = false) }
                ?: draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                startCollecting()
                draw()
            } else {
                stopCollecting()
                animator?.cancel()
            }
        }

        override fun onDestroy() {
            stopCollecting()
            animator?.cancel()
            super.onDestroy()
        }

        private fun startCollecting() {
            if (collectJob?.isActive == true) return
            collectJob = scope.launch {
                NowPlaying.state.collect { info ->
                    info.art?.let { setNewArt(it, animate = true) }
                }
            }
        }

        private fun stopCollecting() {
            collectJob?.cancel()
            collectJob = null
        }

        private fun setNewArt(art: Bitmap, animate: Boolean) {
            if (surfaceW <= 0 || surfaceH <= 0) return
            scope.launch {
                val prefs = applicationContext.getSharedPreferences("mw_prefs", MODE_PRIVATE)
                val blur = prefs.getInt("blur", 0)
                val mode = prefs.getString("mode", "full") ?: "full"
                val composed = withContext(Dispatchers.Default) {
                    ImageEffects.composeWallpaper(art, surfaceW, surfaceH, blur, mode)
                }
                if (!animate || current == null) {
                    current = composed
                    incoming = null
                    fade = 1f
                    draw()
                } else {
                    incoming = composed
                    startCrossfade()
                }
            }
        }

        private fun startCrossfade() {
            animator?.cancel()
            fade = 0f
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    fade = it.animatedValue as Float
                    draw()
                    if (fade >= 1f) {
                        current = incoming ?: current
                        incoming = null
                    }
                }
                start()
            }
        }

        private fun draw() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                canvas.drawColor(Color.BLACK)
                current?.let {
                    paint.alpha = 255
                    canvas.drawBitmap(it, 0f, 0f, paint)
                }
                incoming?.let {
                    paint.alpha = (fade * 255).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(it, 0f, 0f, paint)
                    paint.alpha = 255
                }
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                }
            }
        }
    }
}
