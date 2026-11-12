package id.hanzdev.susunblock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import id.hanzdev.susunblock.game.SaveManager
import id.hanzdev.susunblock.game.Themes
import id.hanzdev.susunblock.ui.LogoView
import id.hanzdev.susunblock.ui.UiKit

/** Menu utama Susun Block. */
class MainActivity : Activity() {
    private lateinit var save: SaveManager
    private lateinit var bestText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        save = SaveManager(this)
        val theme = Themes.all[0]

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(theme.background)
            setPadding(0, UiKit.dp(this@MainActivity, 40f), 0, UiKit.dp(this@MainActivity, 24f))
        }

        val logoSize = UiKit.dp(this, 140f)
        root.addView(
            LogoView(this),
            LinearLayout.LayoutParams(logoSize, logoSize).apply { gravity = Gravity.CENTER_HORIZONTAL }
        )
        root.addView(
            UiKit.text(this, "Susun Block", 38f, theme.primaryText, bold = true),
            UiKit.params(this, 16f)
        )
        root.addView(
            UiKit.text(this, "Game puzzle susun blok", 15f, theme.tertiaryText),
            UiKit.params(this, 4f)
        )

        bestText = UiKit.text(this, "", 16f, theme.secondaryText, bold = true)
        root.addView(bestText, UiKit.params(this, 24f))

        root.addView(
            UiKit.button(this, "Main", theme.field, Color.WHITE) {
                startActivity(Intent(this, GameActivity::class.java))
            },
            UiKit.params(this, 28f)
        )
        root.addView(
            UiKit.button(this, "Pengaturan", Color.argb(40, 255, 255, 255), Color.WHITE) {
                startActivity(Intent(this, SettingsActivity::class.java))
            },
            UiKit.params(this, 12f)
        )

        // Pendorong ke bawah agar kredit berada di dasar layar
        root.addView(android.view.View(this), LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(
            UiKit.text(this, "Dikembangkan oleh ${Dev.NAME}", 13f, theme.tertiaryText),
            UiKit.params(this, 24f)
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(theme.background)
            addView(
                root,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        bestText.text = "Terbaik Acak Seimbang: ${save.bestScore(false)}\n" +
            "Terbaik Kesulitan bertahap: ${save.bestScore(true)}"
    }
}
