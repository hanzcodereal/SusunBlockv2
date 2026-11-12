package id.hanzdev.susunblock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import id.hanzdev.susunblock.game.GameEngine
import id.hanzdev.susunblock.game.SaveManager
import id.hanzdev.susunblock.game.Shapes
import id.hanzdev.susunblock.game.SoundManager
import id.hanzdev.susunblock.game.Themes
import id.hanzdev.susunblock.ui.LogoView
import id.hanzdev.susunblock.ui.UiKit

/**
 * Layar pembuka: logo, nama aplikasi, dan versi.
 * Menu dibuka tepat saat persiapan sungguhan selesai, tanpa waktu tunggu buatan.
 */
class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    @Volatile private var cancelled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = Themes.all[0]

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(theme.background)
            setPadding(0, 0, 0, UiKit.dp(this@SplashActivity, 28f))
        }

        fun spacer() = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, 0, 1f)
        }

        val logoSize = UiKit.dp(this, 160f)
        val logo = LogoView(this)
        val title = UiKit.text(this, "Susun Block", 38f, theme.primaryText, bold = true)
        val subtitle = UiKit.text(this, "Game puzzle susun blok", 15f, theme.tertiaryText)

        val version = UiKit.text(this, "Versi ${versionName()}", 13f, theme.tertiaryText)

        root.addView(spacer())
        root.addView(
            logo,
            LinearLayout.LayoutParams(logoSize, logoSize).apply { gravity = Gravity.CENTER_HORIZONTAL }
        )
        root.addView(title, UiKit.params(this, 20f))
        root.addView(subtitle, UiKit.params(this, 4f))
        root.addView(spacer())
        root.addView(version, UiKit.params(this, 0f))

        setContentView(root)

        // Animasi masuk: logo membesar dengan pantulan, teks muncul perlahan
        logo.scaleX = 0.7f
        logo.scaleY = 0.7f
        logo.alpha = 0f
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
            .setInterpolator(OvershootInterpolator(1.2f)).start()
        for ((i, v) in listOf<View>(title, subtitle, version).withIndex()) {
            v.alpha = 0f
            v.animate().alpha(1f).setStartDelay(250L + i * 90L).setDuration(500).start()
        }

        startLoading()
    }

    /** Menjalankan persiapan sungguhan di thread latar, lalu membuka menu begitu semuanya selesai. */
    private fun startLoading() {
        val app = applicationContext
        val steps = listOf<() -> Unit>(
            // Memuat pengaturan: membaca satu nilai memaksa berkas simpanan selesai dimuat.
            { SaveManager(app).bestScore },
            // Memuat bentuk dan tema.
            { check(Shapes.all.isNotEmpty() && Themes.all.isNotEmpty()) },
            // Menyiapkan efek suara.
            { SoundManager.preload() },
            // Memuat mesin game dan menjalankan pembuat blok sekali sebelum permainan dimulai.
            { GameEngine().newGame() }
        )

        Thread {
            for (step in steps) {
                if (cancelled) return@Thread
                try {
                    step()
                } catch (e: Exception) {
                    // Persiapan bersifat pelengkap; jika gagal, game tetap dibuka.
                }
            }
            handler.post { openMenu() }
        }.start()
    }

    private fun versionName(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    @Suppress("DEPRECATION")
    private fun openMenu() {
        if (started || isFinishing) return
        started = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        cancelled = true
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
