package id.hanzdev.susunblock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import id.hanzdev.susunblock.game.BlockShape
import id.hanzdev.susunblock.game.ClearedCell
import id.hanzdev.susunblock.game.GameConfig
import id.hanzdev.susunblock.game.GameEngine
import id.hanzdev.susunblock.game.GameTheme
import id.hanzdev.susunblock.game.SaveManager
import id.hanzdev.susunblock.game.SoundManager
import id.hanzdev.susunblock.game.Themes
import id.hanzdev.susunblock.game.TutorialData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Layar permainan Susun Block: seluruh tampilan digambar langsung di Canvas. */
class GameView(
    context: Context,
    private val engine: GameEngine,
    private val save: SaveManager,
    private val sound: SoundManager,
    private val callbacks: Callbacks
) : View(context) {

    interface Callbacks {
        fun onOpenSettings()
        fun onExitToMenu()
    }

    private class Fx(val row: Int, val col: Int, val colorIdx: Int, val start: Long, val duration: Long)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()
    private val clipPath = Path()
    private val poly = Path()

    // ---- Tata letak
    private var vw = 0f
    private var vh = 0f
    /** 1 unit dunia Unity dalam piksel (layar didesain 6 x 12 unit). */
    private var u = 0f
    /** Jarak antar sel di papan (piksel). */
    private var cell = 0f
    private var gridX = 0f
    private var gridY = 0f
    private var fieldLeft = 0f
    private var fieldTop = 0f
    private var fieldSize = 0f
    private val slotRects = Array(GameConfig.SLOTS) { RectF() }
    private val menuRect = RectF()
    private val skipRect = RectF()
    private val btnAgain = RectF()
    private val btnMenu = RectF()

    // ---- Waktu frame
    private var now = 0L

    // ---- Drag & drop
    private var pickedSlot = -1
    private var touchX = 0f
    private var touchY = 0f
    private var dragCX = 0f
    private var dragCY = 0f
    private var pickT0 = 0L
    private var hasValid = false
    private var lastRow = 0
    private var lastCol = 0
    private var previewRemove: List<Int> = emptyList()

    // ---- Animasi kembali ke slot
    private var retSlot = -1
    private var retT0 = 0L
    private var retFromX = 0f
    private var retFromY = 0f
    private var retFromCell = 0f

    // ---- Animasi lain
    private val spawnT0 = LongArray(GameConfig.SLOTS)
    private val fxList = ArrayList<Fx>()
    private var comboText = ""
    private var comboT0 = 0L
    private var allClearT0 = 0L
    private var themeFrom = -1
    private var themeT0 = 0L
    private var shownScore = 0f

    // ---- Game over
    private var goT0 = 0L
    private var goSoundPlayed = false
    private var bestAtStart = 0
    private var newBestThisGame = false

    // ---- Poin yang baru didapat (muncul di atas blok yang diletakkan)
    private var gainText = ""
    private var gainT0 = 0L
    private var gainX = 0f
    private var gainY = 0f

    // ---- Menu jeda (0 = tutup, 1 = menu, 2 = konfirmasi main ulang)
    private var pauseMode = 0
    private val pauseBtns = Array(4) { RectF() }
    private val panelRect = RectF()

    // ---- Tutorial
    private var tutIndex = 0
    private var tutWaiting = false

    init {
        val t = SystemClock.uptimeMillis()
        for (i in spawnT0.indices) spawnT0[i] = t
        shownScore = engine.score.toFloat()
        bestAtStart = engine.bestScore
    }

    // =========================================================== Siklus hidup

    fun startTutorialIfNeeded() {
        if (!save.tutorialDone) startTutorial()
    }

    /** Dipanggil setelah semua data dihapus dari menu pengaturan. */
    fun resetAfterDataCleared() {
        engine.bestScore = 0
        engine.newGame()
        bestAtStart = 0
        shownScore = 0f
        fxList.clear()
        pickedSlot = -1
        retSlot = -1
        hasValid = false
        startTutorial()
    }

    // ============================================================== Ukuran

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        vw = w.toFloat()
        vh = h.toFloat()
        // Sama seperti CameraFitter di Unity: area aman 6 x 12 unit, dipusatkan di layar.
        u = min(vw / 6f, vh / 12f)
        cell = 0.65f * u
        fieldSize = 5.44f * u
        fieldLeft = vw / 2f - fieldSize / 2f
        fieldTop = vh / 2f - 3.22f * u
        gridX = fieldLeft + 0.12f * u
        gridY = fieldTop + 0.12f * u

        for (i in 0 until GameConfig.SLOTS) {
            val cx = vw / 2f + (i - 1) * 2.0f * u
            val cy = vh / 2f + 3.5f * u
            slotRects[i].set(cx - 1.0f * u, cy - 1.5f * u, cx + 1.0f * u, cy + 1.5f * u)
        }

        val gearCx = vw / 2f + 2.23f * u
        val gearCy = vh / 2f - 5.35f * u
        menuRect.set(gearCx - 0.313f * u, gearCy - 0.313f * u, gearCx + 0.313f * u, gearCy + 0.313f * u)
        skipRect.set(vw / 2f - 1.5f * u, vh / 2f + 5.05f * u, vw / 2f + 1.5f * u, vh / 2f + 5.75f * u)
    }

    // ============================================================= Warna tema

    private fun themeProgress(): Float =
        if (themeFrom < 0) 1f else ((now - themeT0) / 1000f).coerceIn(0f, 1f)

    private fun currentTheme(): GameTheme = Themes.all[engine.themeIndex]

    private inline fun themeMix(sel: (GameTheme) -> Int): Int {
        val t = themeProgress()
        return if (themeFrom < 0 || t >= 1f) {
            sel(currentTheme())
        } else {
            Themes.lerp(sel(Themes.all[themeFrom]), sel(currentTheme()), t)
        }
    }

    private fun blockColor(idx: Int): Int {
        val to = currentTheme().blockColors
        val toColor = to[idx.coerceAtLeast(0) % to.size]
        val t = themeProgress()
        if (themeFrom < 0 || t >= 1f) return toColor
        val from = Themes.all[themeFrom].blockColors
        return Themes.lerp(from[idx.coerceAtLeast(0) % from.size], toColor, t)
    }

    /** Pastikan teks tetap terbaca di atas latar (beberapa tema punya teks putih di latar terang). */
    private fun readable(textColor: Int): Int {
        val bg = themeMix { it.background }
        return if (abs(Themes.luminance(textColor) - Themes.luminance(bg)) < 0.3f) {
            if (Themes.luminance(bg) > 0.5f) Color.rgb(34, 34, 34) else Color.WHITE
        } else {
            textColor
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    // ============================================================= Menggambar

    override fun onDraw(canvas: Canvas) {
        now = SystemClock.uptimeMillis()
        var animating = false

        canvas.drawColor(themeMix { it.background })
        if (themeFrom >= 0 && themeProgress() >= 1f) themeFrom = -1
        if (themeFrom >= 0) animating = true

        drawHeader(canvas)
        drawField(canvas)
        if (fxList.isNotEmpty()) {
            drawFx(canvas)
            animating = true
        }
        drawPreview(canvas)
        drawSlots(canvas)
        drawPicked(canvas)
        drawPopups(canvas)
        drawGain(canvas)
        if (engine.tutorialMode) drawTutorialOverlay(canvas)
        if (engine.gameOver) drawGameOver(canvas)
        if (pauseMode != 0) drawPause(canvas)

        // Skor menghitung naik secara halus
        val target = engine.score.toFloat()
        if (abs(target - shownScore) > 0.5f) {
            shownScore += (target - shownScore) * 0.18f
            if (abs(target - shownScore) < 1f) shownScore = target
            animating = true
        } else {
            shownScore = target
        }

        if (pickedSlot >= 0 || retSlot >= 0 || engine.tutorialMode || engine.gameOver) animating = true
        if (now - comboT0 < COMBO_MS || now - allClearT0 < ALL_CLEAR_MS) animating = true
        if ((now - gainT0).toFloat() < GAIN_MS) animating = true
        for (t in spawnT0) if (now - t < SPAWN_MS) animating = true

        if (animating) postInvalidateOnAnimation()
    }

    private fun drawHeader(canvas: Canvas) {
        val primary = readable(themeMix { it.primaryText })
        val secondary = readable(themeMix { it.secondaryText })

        // Ikon pengaturan (roda gigi) di kanan atas
        drawGear(
            canvas, menuRect.centerX(), menuRect.centerY(), 0.626f * u,
            themeMix { it.cellDefault }, themeMix { it.background }
        )

        if (engine.tutorialMode) return

        // Kiri atas: skor terbaik algoritma yang sedang dipakai. Skor sekarang besar di tengah.
        drawText(
            canvas, engine.bestScore.toString(), vw / 2f - 2.59f * u, vh / 2f - 5.26f * u,
            0.45f * u, secondary, bold = true, align = Paint.Align.LEFT
        )
        drawText(
            canvas, shownScore.roundToInt().toString(), vw / 2f, vh / 2f - 3.83f * u,
            0.78f * u, primary, bold = false
        )
    }

    private fun drawGear(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, hole: Int) {
        val r = size / 2f
        fill.color = color
        for (i in 0 until 8) {
            canvas.save()
            canvas.rotate(i * 45f, cx, cy)
            rect.set(cx - r * 0.24f, cy - r, cx + r * 0.24f, cy - r * 0.5f)
            canvas.drawRoundRect(rect, r * 0.08f, r * 0.08f, fill)
            canvas.restore()
        }
        canvas.drawCircle(cx, cy, r * 0.76f, fill)
        fill.color = hole
        canvas.drawCircle(cx, cy, r * 0.42f, fill)
    }

    private fun drawField(canvas: Canvas) {
        // Bingkai papan
        rect.set(fieldLeft, fieldTop, fieldLeft + fieldSize, fieldTop + fieldSize)
        fill.color = themeMix { it.field }
        canvas.drawRoundRect(rect, 0.127f * u, 0.127f * u, fill)

        val emptyColor = themeMix { it.cellDefault }
        for (r in 0 until GameConfig.ROWS) {
            for (c in 0 until GameConfig.COLS) {
                val idx = engine.colors[r * GameConfig.COLS + c]
                val l = gridX + c * cell
                val t = gridY + r * cell
                if (idx < 0) {
                    drawCell(canvas, l, t, cell, emptyColor, flat = true)
                } else {
                    drawCell(canvas, l, t, cell, blockColor(idx))
                }
            }
        }
    }

    private fun drawFx(canvas: Canvas) {
        val iter = fxList.iterator()
        while (iter.hasNext()) {
            val fx = iter.next()
            val elapsed = now - fx.start
            if (elapsed >= fx.duration) {
                iter.remove()
                continue
            }
            val l = gridX + fx.col * cell
            val t = gridY + fx.row * cell
            val color = blockColor(fx.colorIdx)
            if (elapsed < 0) {
                // Masih menunggu giliran: tampilkan sel seperti semula
                drawCell(canvas, l, t, cell, color)
            } else {
                val p = elapsed.toFloat() / fx.duration
                val scale = 1f - p
                val s = cell * (0.4f + 0.6f * scale)
                val cx = l + cell / 2f
                val cy = t + cell / 2f
                val flash = if (p < 0.25f) Themes.lerp(color, Color.WHITE, 1f - p / 0.25f) else color
                drawCell(canvas, cx - s / 2f, cy - s / 2f, s, flash, alpha = (255 * (1f - p * p)).toInt())
            }
        }
    }

    private fun drawPreview(canvas: Canvas) {
        if (!hasValid || pickedSlot < 0) return
        val piece = engine.pieces[pickedSlot] ?: return
        val color = blockColor(piece.colorIdx)
        for (idx in previewRemove) {
            val r = idx / GameConfig.COLS
            val c = idx % GameConfig.COLS
            drawCell(canvas, gridX + c * cell, gridY + r * cell, cell, color, alpha = 215)
        }
        val shape = piece.shape
        for (y in 0 until shape.sizeY) for (x in 0 until shape.sizeX) {
            if (!shape.filled(x, y)) continue
            drawCell(
                canvas, gridX + (lastCol + x) * cell, gridY + (lastRow + y) * cell,
                cell, color, alpha = 150
            )
        }
    }

    /** Jarak antar sel blok yang belum diambil (rumus yang sama dengan BlockSpawner di Unity). */
    private fun restCellSize(shape: BlockShape, @Suppress("UNUSED_PARAMETER") slot: RectF): Float {
        val distanceForBlock = (6f - 0.4f * (2 + GameConfig.SLOTS - 1)) / GameConfig.SLOTS
        val sx = distanceForBlock / (shape.sizeX * 0.65f)
        val sy = distanceForBlock / (shape.sizeY * 0.65f)
        val scale = min(0.7f, min(sx, sy))
        return 0.5f * scale * u
    }

    private fun drawSlots(canvas: Canvas) {
        for (i in 0 until GameConfig.SLOTS) {
            val piece = engine.pieces[i] ?: continue
            if (i == pickedSlot) continue
            val shape = piece.shape
            val slot = slotRects[i]
            var cs = restCellSize(shape, slot)
            var cx = slot.centerX()
            var cy = slot.centerY()

            if (i == retSlot) {
                val p = ((now - retT0) / RETURN_MS).coerceIn(0f, 1f)
                cx = retFromX + (cx - retFromX) * p
                cy = retFromY + (cy - retFromY) * p
                cs = retFromCell + (cs - retFromCell) * p
                if (p >= 1f) retSlot = -1
            }

            val sp = ((now - spawnT0[i]).toFloat() / SPAWN_MS).coerceIn(0f, 1f)
            if (sp < 1f) cs *= easeOutBack(sp)

            drawPiece(canvas, shape, cx, cy, cs, blockColor(piece.colorIdx))
        }
    }

    private fun drawPicked(canvas: Canvas) {
        if (pickedSlot < 0) return
        val piece = engine.pieces[pickedSlot] ?: return
        val shape = piece.shape
        val rest = restCellSize(shape, slotRects[pickedSlot])
        val t = ((now - pickT0) / 90f).coerceIn(0f, 1f)
        val cs = rest + (cell - rest) * t
        drawPiece(canvas, shape, dragCX, dragCY, cs, blockColor(piece.colorIdx))
    }

    private fun drawPopups(canvas: Canvas) {
        val tertiary = readable(themeMix { it.tertiaryText })
        val secondary = readable(themeMix { it.secondaryText })

        val cp = (now - comboT0).toFloat() / COMBO_MS
        if (cp in 0f..1f && comboText.isNotEmpty()) {
            val scale = if (cp < 0.2f) 0.6f + 2f * cp else 1f
            val alpha = if (cp > 0.7f) ((1f - cp) / 0.3f * 255).toInt() else 255
            drawText(
                canvas, comboText, vw / 2f, fieldTop - 0.2f * u,
                0.42f * u * scale, tertiary, alpha
            )
        }

        val ap = (now - allClearT0).toFloat() / ALL_CLEAR_MS
        if (ap in 0f..1f) {
            val scale = easeOutBack(min(1f, ap * 2f))
            val alpha = if (ap > 0.7f) ((1f - ap) / 0.3f * 255).toInt() else 255
            val cy = fieldTop + fieldSize / 2f
            drawText(canvas, "Semua Bersih!", vw / 2f, cy, vw * 0.11f * scale, secondary, alpha)
            drawText(
                canvas, "+1000", vw / 2f, cy + vw * 0.12f * scale,
                vw * 0.08f * scale, tertiary, alpha
            )
        }
    }

    /** Poin yang baru didapat, muncul di atas blok yang diletakkan lalu memudar. */
    private fun drawGain(canvas: Canvas) {
        val p = (now - gainT0).toFloat() / GAIN_MS
        if (p !in 0f..1f || gainText.isEmpty()) return
        val alpha = ((1f - p) * 255).toInt()
        val primary = readable(themeMix { it.primaryText })
        drawText(canvas, gainText, gainX, gainY - (0.3f + 0.9f * p) * u, 0.4f * u, primary, alpha)
    }

    private fun drawTutorialOverlay(canvas: Canvas) {
        val ex = TutorialData.examples.getOrNull(tutIndex) ?: return
        val primary = readable(themeMix { it.primaryText })
        val secondary = readable(themeMix { it.secondaryText })

        drawText(canvas, "Cara Bermain", vw / 2f, vh / 2f - 4.6f * u, 0.55f * u, primary)
        val lines = ex.hint.split("\n")
        for ((i, line) in lines.withIndex()) {
            drawText(canvas, line, vw / 2f, vh / 2f - 3.95f * u + i * 0.36f * u, 0.27f * u, secondary)
        }

        // Tanda tujuan (berkedip)
        val piece = engine.pieces[GameConfig.SLOTS / 2]
        if (piece != null && !tutWaiting) {
            val pulse = 0.5f + 0.5f * sin(now / 260.0 * PI).toFloat()
            val alpha = (80 + 110 * pulse).toInt()
            val shape = piece.shape
            val color = blockColor(piece.colorIdx)
            for (y in 0 until shape.sizeY) for (x in 0 until shape.sizeX) {
                if (!shape.filled(x, y)) continue
                drawCell(
                    canvas, gridX + (ex.targetCol + x) * cell, gridY + (ex.targetRow + y) * cell,
                    cell, color, alpha = alpha
                )
            }
        }

        drawText(canvas, "Lewati", vw / 2f, skipRect.centerY() + 0.12f * u, 0.3f * u, secondary)
    }

    private fun drawGameOver(canvas: Canvas) {
        val elapsed = now - goT0
        if (elapsed < GAME_OVER_DELAY_MS) return
        val a = ((elapsed - GAME_OVER_DELAY_MS) / 300f).coerceIn(0f, 1f)

        fill.color = withAlpha(Color.BLACK, (170 * a).toInt())
        canvas.drawRect(0f, 0f, vw, vh, fill)

        if (!goSoundPlayed) {
            goSoundPlayed = true
            if (newBestThisGame) sound.newBest()
        }

        val pw = vw * 0.84f
        val ph = vh * 0.42f
        val left = (vw - pw) / 2f
        val top = (vh - ph) / 2f
        rect.set(left, top, left + pw, top + ph)
        fill.color = withAlpha(themeMix { it.field }, (255 * a).toInt())
        canvas.drawRoundRect(rect, cell * 0.5f, cell * 0.5f, fill)

        val alpha = (255 * a).toInt()
        val primary = readable(themeMix { it.primaryText })
        val secondary = readable(themeMix { it.secondaryText })
        val tertiary = readable(themeMix { it.tertiaryText })

        drawText(canvas, "Permainan Selesai", vw / 2f, top + ph * 0.15f, vw * 0.07f, primary, alpha)
        drawText(canvas, "Skor", vw / 2f, top + ph * 0.27f, vw * 0.04f, secondary, alpha)
        val count = ((elapsed - GAME_OVER_DELAY_MS) / 900f).coerceIn(0f, 1f)
        drawText(
            canvas, (engine.score * count).roundToInt().toString(), vw / 2f, top + ph * 0.40f,
            vw * 0.11f, primary, alpha
        )
        if (newBestThisGame) {
            val blink = 0.4f + 0.6f * (0.5f + 0.5f * sin(now / 250.0 * PI).toFloat())
            drawText(
                canvas, "SKOR TERBAIK BARU!", vw / 2f, top + ph * 0.50f,
                vw * 0.05f, tertiary, (alpha * blink).toInt()
            )
        } else {
            drawText(
                canvas, "Skor Terbaik: ${engine.bestScore}", vw / 2f, top + ph * 0.50f,
                vw * 0.045f, secondary, alpha
            )
        }

        val bw = pw * 0.8f
        val bh = ph * 0.15f
        val bl = (vw - bw) / 2f
        btnAgain.set(bl, top + ph * 0.60f, bl + bw, top + ph * 0.60f + bh)
        btnMenu.set(bl, top + ph * 0.79f, bl + bw, top + ph * 0.79f + bh)
        drawButton(canvas, btnAgain, "Main Lagi", themeMix { it.secondaryText }, themeMix { it.background }, alpha)
        drawButton(canvas, btnMenu, "Menu Utama", withAlpha(Color.WHITE, 40), Color.WHITE, alpha)
    }

    private fun drawButton(canvas: Canvas, r: RectF, label: String, bg: Int, fg: Int, alpha: Int) {
        fill.color = withAlpha(bg, if ((bg ushr 24) == 0xFF) alpha else min(alpha, bg ushr 24))
        canvas.drawRoundRect(r, r.height() / 2.5f, r.height() / 2.5f, fill)
        drawText(canvas, label, r.centerX(), r.centerY() + vw * 0.017f, vw * 0.05f, fg, alpha)
    }

    // ---- Primitif menggambar

    private fun drawText(
        canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int,
        alpha: Int = 255, bold: Boolean = true, align: Paint.Align = Paint.Align.CENTER
    ) {
        textPaint.textSize = size
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.textAlign = align
        textPaint.color = withAlpha(color, alpha)
        canvas.drawText(text, x, y, textPaint)
    }

    private fun shade(color: Int, f: Float): Int {
        val r = (((color shr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
        val g = (((color shr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * f).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun fillPoly(
        canvas: Canvas, color: Int, alpha: Int,
        x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float
    ) {
        poly.reset()
        poly.moveTo(x1, y1)
        poly.lineTo(x2, y2)
        poly.lineTo(x3, y3)
        poly.lineTo(x4, y4)
        poly.close()
        fill.color = withAlpha(color, alpha)
        canvas.drawPath(poly, fill)
    }

    /**
     * Menggambar satu sel. [size] = jarak antar sel (ada celah 6,25%).
     * Sel berisi digambar dengan efek bevel seperti sprite asli; [flat] = sel kosong yang datar.
     */
    private fun drawCell(
        canvas: Canvas, l: Float, t: Float, size: Float, color: Int,
        alpha: Int = 255, flat: Boolean = false
    ) {
        val inset = size * 0.03125f
        val x0 = l + inset
        val y0 = t + inset
        val x1 = l + size - inset
        val y1 = t + size - inset
        val w = x1 - x0
        val radius = w * 0.11f

        if (flat) {
            rect.set(x0, y0, x1, y1)
            fill.color = withAlpha(color, alpha)
            canvas.drawRoundRect(rect, radius, radius, fill)
            return
        }

        val b = w * 0.17f
        canvas.save()
        rect.set(x0, y0, x1, y1)
        clipPath.reset()
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        fillPoly(canvas, shade(color, 0.89f), alpha, x0, y0, x1, y0, x1 - b, y0 + b, x0 + b, y0 + b)   // atas
        fillPoly(canvas, shade(color, 0.85f), alpha, x0, y0, x0 + b, y0 + b, x0 + b, y1 - b, x0, y1)   // kiri
        fillPoly(canvas, shade(color, 0.71f), alpha, x1, y0, x1, y1, x1 - b, y1 - b, x1 - b, y0 + b)   // kanan
        fillPoly(canvas, shade(color, 0.59f), alpha, x0, y1, x0 + b, y1 - b, x1 - b, y1 - b, x1, y1)   // bawah
        fill.color = withAlpha(shade(color, 0.78f), alpha)                                              // tengah
        canvas.drawRect(x0 + b, y0 + b, x1 - b, y1 - b, fill)
        canvas.restore()
    }

    private fun drawPiece(
        canvas: Canvas, shape: BlockShape, cx: Float, cy: Float, cs: Float, color: Int
    ) {
        val left = cx - shape.sizeX * cs / 2f
        val top = cy - shape.sizeY * cs / 2f
        for (y in 0 until shape.sizeY) for (x in 0 until shape.sizeX) {
            if (shape.filled(x, y)) drawCell(canvas, left + x * cs, top + y * cs, cs, color)
        }
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    // ================================================================ Sentuhan

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return handleDown(e.x, e.y)
            MotionEvent.ACTION_MOVE -> if (pickedSlot >= 0) {
                touchX = e.x
                touchY = e.y
                onMoved()
                invalidate()
            }
            MotionEvent.ACTION_UP -> handleUp(false)
            MotionEvent.ACTION_CANCEL -> handleUp(true)
        }
        return true
    }

    private fun handleDown(x: Float, y: Float): Boolean {
        if (pauseMode != 0) {
            handlePauseTouch(x, y)
            return true
        }
        if (engine.gameOver) {
            if (now - goT0 < GAME_OVER_DELAY_MS) return true
            if (btnAgain.contains(x, y)) {
                sound.button()
                restartGame()
            } else if (btnMenu.contains(x, y)) {
                sound.button()
                callbacks.onExitToMenu()
            }
            return true
        }
        if (inflated(menuRect).contains(x, y)) {
            sound.button()
            pauseMode = 1
            invalidate()
            return true
        }
        if (engine.tutorialMode) {
            if (skipRect.contains(x, y)) {
                sound.button()
                finishTutorial()
                return true
            }
            if (tutWaiting) return true
        }
        if (pickedSlot >= 0) return true
        for (i in 0 until GameConfig.SLOTS) {
            if (engine.pieces[i] != null && i != retSlot && slotRects[i].contains(x, y)) {
                pickedSlot = i
                touchX = x
                touchY = y
                pickT0 = SystemClock.uptimeMillis()
                hasValid = false
                previewRemove = emptyList()
                updateDragPosition()
                sound.pickup()
                invalidate()
                return true
            }
        }
        return true
    }

    private fun inflated(r: RectF): RectF {
        val d = r.width() * 0.6f
        return RectF(r.left - d, r.top - d, r.right + d, r.bottom + d)
    }

    private fun updateDragPosition() {
        if (engine.pieces[pickedSlot] == null) return
        // Blok melayang di atas jari: offset Y = tinggi jari dari bawah * 5 unit, dibatasi 1..5 unit
        val fromBottomFraction = ((vh - touchY) / vh).coerceIn(0f, 1f)
        val yOff = (fromBottomFraction * 5f).coerceIn(1f, 5f) * u
        val xOff = ((touchX / vw) - 0.5f).coerceIn(-0.5f, 1f) * u
        dragCX = touchX + xOff
        dragCY = touchY - yOff
    }

    private fun onMoved() {
        val piece = engine.pieces[pickedSlot] ?: return
        val shape = piece.shape
        updateDragPosition()

        val left = dragCX - shape.sizeX * cell / 2f
        val top = dragCY - shape.sizeY * cell / 2f
        val col = ((left - gridX) / cell).roundToInt()
        val row = ((top - gridY) / cell).roundToInt()

        if (engine.canPlace(shape, row, col)) {
            lastRow = row
            lastCol = col
            hasValid = true
            if (engine.tutorialMode && !isTutorialTarget(row, col)) hasValid = false
        } else if (hasValid) {
            // Posisi sah terakhir tetap dipertahankan jika jari bergeser sedikit
            val dr = row - lastRow
            val dc = col - lastCol
            if (abs(dr) > 1 || abs(dc) > 1) {
                hasValid = false
            } else if (engine.canPlace(shape, lastRow + dr, lastCol)) {
                lastRow += dr
            } else if (engine.canPlace(shape, lastRow, lastCol + dc)) {
                lastCol += dc
            }
            if (hasValid && engine.tutorialMode && !isTutorialTarget(lastRow, lastCol)) hasValid = false
        }
        updatePreview()
    }

    private fun isTutorialTarget(row: Int, col: Int): Boolean {
        val ex = TutorialData.examples.getOrNull(tutIndex) ?: return false
        return ex.targetRow == row && ex.targetCol == col
    }

    private fun updatePreview() {
        previewRemove = emptyList()
        if (!hasValid || pickedSlot < 0) return
        val piece = engine.pieces[pickedSlot] ?: return
        previewRemove = engine.cellsOfPotentialClears(piece.shape, lastRow, lastCol)
    }

    private fun handleUp(cancelled: Boolean) {
        val slot = pickedSlot
        if (slot < 0) return
        pickedSlot = -1
        val place = !cancelled && hasValid && !engine.gameOver
        val row = lastRow
        val col = lastCol
        hasValid = false
        previewRemove = emptyList()

        if (place) {
            doPlace(slot, row, col)
        } else {
            val piece = engine.pieces[slot]
            if (piece != null) {
                val rest = restCellSize(piece.shape, slotRects[slot])
                val t = ((SystemClock.uptimeMillis() - pickT0) / 90f).coerceIn(0f, 1f)
                retSlot = slot
                retT0 = SystemClock.uptimeMillis()
                retFromX = dragCX
                retFromY = dragCY
                retFromCell = rest + (cell - rest) * t
            }
        }
        invalidate()
    }

    // ============================================================ Aturan main

    private fun doPlace(slot: Int, row: Int, col: Int) {
        val t = SystemClock.uptimeMillis()
        val res = engine.place(slot, row, col)
        sound.place()

        // Efek hapus garis, merambat dari posisi blok yang diletakkan
        if (res.cleared.isNotEmpty()) {
            val cr = res.placedRows.average()
            val cc = res.placedCols.average()
            for (cl: ClearedCell in res.cleared) {
                val dist = abs(cl.row - cr) + abs(cl.col - cc)
                fxList.add(Fx(cl.row, cl.col, cl.colorIdx, t + (dist * 28).toLong(), 320L))
            }
            sound.lines(res.linesCleared)
            haptic(true, res.linesCleared)
        } else {
            haptic(false, 1)
        }

        if (res.linesCleared >= 2 && !engine.tutorialMode) {
            themeFrom = engine.themeIndex
            engine.nextTheme()
            themeT0 = t
        }
        // Kombo: langkah beruntun yang menghapus garis
        if (res.streak >= 2) {
            comboText = "Kombo x${res.streak}"
            comboT0 = t
        }
        if (res.gained > 0 && !engine.tutorialMode) {
            gainText = "+${res.gained}"
            gainT0 = t
            gainX = gridX + (res.placedCols.average().toFloat() + 0.5f) * cell
            gainY = gridY + (res.placedRows.average().toFloat() + 0.5f) * cell
        }
        if (res.allClear && !engine.tutorialMode) {
            allClearT0 = t
            sound.allClear()
        }

        if (engine.tutorialMode) {
            tutWaiting = true
            postDelayed({ advanceTutorial() }, 900L)
            return
        }

        if (engine.pieces.all { it == null }) {
            if (engine.spawnPieces()) {
                for (i in spawnT0.indices) spawnT0[i] = t
            } else {
                triggerGameOver()
                return
            }
        }
        if (engine.isStuck()) {
            triggerGameOver()
            return
        }
        save.save(engine)
    }

    private fun triggerGameOver() {
        engine.gameOver = true
        goT0 = SystemClock.uptimeMillis()
        goSoundPlayed = false
        newBestThisGame = engine.score > 0 && engine.bestScore > bestAtStart && engine.score >= engine.bestScore
        sound.gameOver()
        haptic(true, 2)
        save.save(engine)
    }

    private fun restartGame() {
        engine.newGame()
        bestAtStart = engine.bestScore
        shownScore = 0f
        fxList.clear()
        comboT0 = 0L
        allClearT0 = 0L
        themeFrom = -1
        val t = SystemClock.uptimeMillis()
        for (i in spawnT0.indices) spawnT0[i] = t
        save.save(engine)
        invalidate()
    }

    // ============================================================ Menu jeda

    private fun pauseLabels(): List<String> = when (pauseMode) {
        1 -> if (engine.tutorialMode) {
            listOf("Lanjutkan", "Pengaturan", "Menu Utama")
        } else {
            listOf("Lanjutkan", "Main Ulang", "Pengaturan", "Menu Utama")
        }
        2 -> listOf("Ya, Ulangi", "Batal")
        else -> emptyList()
    }

    private fun drawPause(canvas: Canvas) {
        fill.color = withAlpha(Color.BLACK, 170)
        canvas.drawRect(0f, 0f, vw, vh, fill)

        val labels = pauseLabels()
        val pw = min(vw * 0.84f, 5.2f * u)
        val bh = 0.8f * u
        val gap = 0.22f * u
        val head = if (pauseMode == 2) 2.0f * u else 1.25f * u
        val ph = head + labels.size * (bh + gap) + 0.25f * u
        val left = (vw - pw) / 2f
        val top = (vh - ph) / 2f
        rect.set(left, top, left + pw, top + ph)
        panelRect.set(rect)
        fill.color = themeMix { it.field }
        canvas.drawRoundRect(rect, 0.3f * u, 0.3f * u, fill)

        val primary = readable(themeMix { it.primaryText })
        val secondary = readable(themeMix { it.secondaryText })
        if (pauseMode == 1) {
            drawText(canvas, "Jeda", vw / 2f, top + 0.85f * u, 0.55f * u, primary)
        } else {
            drawText(canvas, "Mulai ulang permainan?", vw / 2f, top + 0.8f * u, 0.38f * u, primary)
            drawText(canvas, "Skor saat ini akan hilang.", vw / 2f, top + 1.4f * u, 0.28f * u, secondary)
        }

        val bw = pw * 0.82f
        val bl = (vw - bw) / 2f
        var y = top + head
        for ((i, label) in labels.withIndex()) {
            pauseBtns[i].set(bl, y, bl + bw, y + bh)
            if (label == "Lanjutkan" || label == "Ya, Ulangi") {
                drawButton(
                    canvas, pauseBtns[i], label,
                    themeMix { it.secondaryText }, themeMix { it.background }, 255
                )
            } else {
                drawButton(canvas, pauseBtns[i], label, withAlpha(Color.WHITE, 40), Color.WHITE, 255)
            }
            y += bh + gap
        }
    }

    private fun handlePauseTouch(x: Float, y: Float) {
        val labels = pauseLabels()
        for (i in labels.indices) {
            if (!pauseBtns[i].contains(x, y)) continue
            sound.button()
            when (labels[i]) {
                "Lanjutkan" -> pauseMode = 0
                "Main Ulang" -> pauseMode = 2
                "Batal" -> pauseMode = 1
                "Ya, Ulangi" -> {
                    pauseMode = 0
                    restartGame()
                }
                "Pengaturan" -> {
                    pauseMode = 0
                    callbacks.onOpenSettings()
                }
                "Menu Utama" -> callbacks.onExitToMenu()
            }
            invalidate()
            return
        }
        // Ketuk di luar panel menutup menu jeda
        if (!panelRect.contains(x, y)) {
            pauseMode = 0
            invalidate()
        }
    }

    /** Tombol kembali sistem: true bila sudah dipakai untuk menutup menu jeda. */
    fun handleBack(): Boolean {
        if (pauseMode == 0) return false
        pauseMode = 0
        invalidate()
        return true
    }

    /** Memutar ulang tutorial (permainan yang berjalan dimulai dari awal setelah tutorial). */
    fun replayTutorial() {
        pauseMode = 0
        pickedSlot = -1
        retSlot = -1
        hasValid = false
        fxList.clear()
        engine.gameOver = false
        startTutorial()
    }

    // ================================================================ Tutorial

    private fun startTutorial() {
        engine.tutorialMode = true
        tutIndex = 0
        tutWaiting = false
        loadTutorialExample()
    }

    private fun loadTutorialExample() {
        val ex = TutorialData.examples[tutIndex]
        engine.loadTutorial(ex)
        fxList.clear()
        tutWaiting = false
        val t = SystemClock.uptimeMillis()
        for (i in spawnT0.indices) spawnT0[i] = t
        invalidate()
    }

    private fun advanceTutorial() {
        if (!engine.tutorialMode) return
        tutIndex++
        if (tutIndex >= TutorialData.examples.size) finishTutorial() else loadTutorialExample()
    }

    private fun finishTutorial() {
        val best = engine.bestScore
        engine.newGame() // juga menonaktifkan mode tutorial
        engine.bestScore = best
        bestAtStart = best
        shownScore = 0f
        fxList.clear()
        tutWaiting = false
        save.tutorialDone = true
        val t = SystemClock.uptimeMillis()
        for (i in spawnT0.indices) spawnT0[i] = t
        save.save(engine)
        invalidate()
    }

    // ================================================================= Getaran

    private fun haptic(heavy: Boolean, times: Int) {
        if (!save.hapticsOn) return
        val type = if (heavy) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY
        performHapticFeedback(type)
        for (i in 1 until times) postDelayed({ performHapticFeedback(type) }, 90L * i)
    }

    private companion object {
        const val SPAWN_MS = 260L
        const val RETURN_MS = 110f
        const val COMBO_MS = 750f
        const val ALL_CLEAR_MS = 900f
        const val GAME_OVER_DELAY_MS = 1100L
        const val GAIN_MS = 700f
    }
}
