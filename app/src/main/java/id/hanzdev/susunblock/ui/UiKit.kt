package id.hanzdev.susunblock.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import id.hanzdev.susunblock.Dev
import id.hanzdev.susunblock.game.Themes
import kotlin.math.min

/** Helper kecil untuk membuat tampilan menu tanpa file layout XML. */
object UiKit {
    fun dp(c: Context, v: Float): Int = (v * c.resources.displayMetrics.density + 0.5f).toInt()

    /** Lebar konten menu: maksimal 320dp atau 85% lebar layar. */
    fun contentWidth(c: Context): Int {
        val screen = c.resources.displayMetrics.widthPixels
        return min(dp(c, 320f), (screen * 0.85f).toInt())
    }

    fun params(c: Context, topMarginDp: Float = 0f, widthPx: Int = contentWidth(c)): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(c, topMarginDp)
            gravity = Gravity.CENTER_HORIZONTAL
        }

    fun roundBg(color: Int, radiusPx: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx
        }

    fun text(c: Context, text: String, sizeSp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(c).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    fun button(c: Context, label: String, bg: Int, fg: Int, onClick: () -> Unit): TextView =
        TextView(c).apply {
            text = label
            setTextColor(fg)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(c, 20f), dp(c, 14f), dp(c, 20f), dp(c, 14f))
            background = roundBg(bg, dp(c, 16f).toFloat())
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
        }

    fun openUrl(c: Context, url: String) {
        try {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(c, "Tidak bisa membuka tautan", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyEmail(c: Context) {
        val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Email developer", Dev.EMAIL))
        Toast.makeText(c, "Email disalin", Toast.LENGTH_SHORT).show()
    }

    /** Dialog "Tentang Developer": email tidak ditampilkan, hanya lewat tombol Kirim/Salin. */
    fun showDeveloperDialog(c: Context) {
        val theme = Themes.all[0]
        val dialog = Dialog(c)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val width = contentWidth(c)

        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundBg(Themes.lerp(theme.background, Color.WHITE, 0.08f), dp(c, 24f).toFloat())
            setPadding(dp(c, 20f), dp(c, 24f), dp(c, 20f), dp(c, 16f))
        }
        val logo = dp(c, 72f)
        root.addView(
            LogoView(c),
            LinearLayout.LayoutParams(logo, logo).apply { gravity = Gravity.CENTER_HORIZONTAL }
        )
        fun lp(topDp: Float) = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(c, topDp) }

        root.addView(text(c, Dev.NAME, 20f, theme.primaryText, bold = true), lp(14f))
        root.addView(text(c, "Developer Susun Block", 13f, theme.tertiaryText), lp(2f))

        root.addView(
            button(c, "Kirim Email", theme.secondaryText, theme.background) {
                dialog.dismiss()
                sendEmail(c)
            },
            lp(22f)
        )
        root.addView(
            button(c, "Salin Email", Color.argb(40, 255, 255, 255), Color.WHITE) {
                dialog.dismiss()
                copyEmail(c)
            },
            lp(10f)
        )
        root.addView(
            button(c, "Dukung Developer", Color.argb(40, 255, 255, 255), theme.secondaryText) {
                dialog.dismiss()
                openUrl(c, Dev.SUPPORT_URL)
            },
            lp(10f)
        )
        val close = text(c, "Tutup", 15f, theme.tertiaryText, bold = true).apply {
            setPadding(0, dp(c, 12f), 0, dp(c, 4f))
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(close, lp(8f))

        dialog.setContentView(root, ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT))
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun sendEmail(c: Context) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Dev.EMAIL}"))
            intent.putExtra(Intent.EXTRA_SUBJECT, "Susun Block - Masukan")
            c.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(c, "Tidak ada aplikasi email. Kirim ke ${Dev.EMAIL}", Toast.LENGTH_LONG).show()
        }
    }
}

/** Logo Susun Block: papan kecil berisi susunan blok bevel (sama dengan ikon aplikasi). */
class LogoView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private val poly = Path()

    // 'o' oranye, 'g' hijau, 'y' kuning, 'b' biru, '.' sel kosong
    private val pattern = arrayOf("ogg", "oy.", "bby")

    private fun colorOf(ch: Char): Int = when (ch) {
        'o' -> Color.rgb(0xEF, 0x7D, 0x57)
        'g' -> Color.rgb(0x38, 0xB8, 0x63)
        'y' -> Color.rgb(0xFF, 0xCD, 0x75)
        else -> Color.rgb(0x41, 0xA6, 0xF6)
    }

    private fun shade(color: Int, f: Float): Int {
        val r = (Color.red(color) * f).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * f).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * f).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun fillPoly(canvas: Canvas, color: Int, x1: Float, y1: Float, x2: Float, y2: Float,
                     x3: Float, y3: Float, x4: Float, y4: Float) {
        poly.reset()
        poly.moveTo(x1, y1); poly.lineTo(x2, y2); poly.lineTo(x3, y3); poly.lineTo(x4, y4)
        poly.close()
        paint.color = color
        canvas.drawPath(poly, paint)
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        val theme = Themes.all[0]

        // Papan
        rect.set(0f, 0f, size, size)
        paint.color = theme.field
        canvas.drawRoundRect(rect, size * 0.17f, size * 0.17f, paint)

        val pad = size * 0.075f
        val pitch = (size - 2 * pad) / 3f
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val ch = pattern[r][c]
                val inset = pitch * 0.03125f
                val x0 = pad + c * pitch + inset
                val y0 = pad + r * pitch + inset
                val x1 = pad + (c + 1) * pitch - inset
                val y1 = pad + (r + 1) * pitch - inset
                val w = x1 - x0
                val radius = w * 0.13f
                rect.set(x0, y0, x1, y1)
                if (ch == '.') {
                    paint.color = theme.cellDefault
                    canvas.drawRoundRect(rect, radius, radius, paint)
                    continue
                }
                val color = colorOf(ch)
                val b = w * 0.17f
                canvas.save()
                clip.reset()
                clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
                canvas.clipPath(clip)
                fillPoly(canvas, shade(color, 1.05f), x0, y0, x1, y0, x1 - b, y0 + b, x0 + b, y0 + b)
                fillPoly(canvas, shade(color, 0.95f), x0, y0, x0 + b, y0 + b, x0 + b, y1 - b, x0, y1)
                fillPoly(canvas, shade(color, 0.74f), x1, y0, x1, y1, x1 - b, y1 - b, x1 - b, y0 + b)
                fillPoly(canvas, shade(color, 0.62f), x0, y1, x0 + b, y1 - b, x1 - b, y1 - b, x1, y1)
                paint.color = shade(color, 0.86f)
                canvas.drawRect(x0 + b, y0 + b, x1 - b, y1 - b, paint)
                canvas.restore()
            }
        }
    }
}
