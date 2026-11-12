package id.hanzdev.susunblock

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.Toast
import id.hanzdev.susunblock.game.SaveManager
import id.hanzdev.susunblock.game.Themes
import id.hanzdev.susunblock.ui.UiKit

/** Pengaturan: efek suara, getaran, algoritma blok, hapus data, dan info developer. */
class SettingsActivity : Activity() {
    private lateinit var save: SaveManager
    private var cleared = false
    private var replayTutorial = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        save = SaveManager(this)
        val theme = Themes.all[0]

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(theme.background)
            setPadding(0, UiKit.dp(this@SettingsActivity, 40f), 0, UiKit.dp(this@SettingsActivity, 32f))
        }

        root.addView(
            UiKit.text(this, "Pengaturan", 30f, theme.primaryText, bold = true),
            UiKit.params(this)
        )

        fun toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit): Switch =
            Switch(this).apply {
                text = label
                textSize = 18f
                setTextColor(theme.primaryText)
                isChecked = initial
                setPadding(UiKit.dp(this@SettingsActivity, 8f), UiKit.dp(this@SettingsActivity, 12f),
                    UiKit.dp(this@SettingsActivity, 8f), UiKit.dp(this@SettingsActivity, 12f))
                setOnCheckedChangeListener { _, checked -> onChange(checked) }
            }

        root.addView(toggle("Efek Suara", save.sfxOn) { save.sfxOn = it }, UiKit.params(this, 28f))
        root.addView(toggle("Getaran", save.hapticsOn) { save.hapticsOn = it }, UiKit.params(this, 4f))

        // Algoritma blok: berlaku untuk permainan yang baru dimulai
        root.addView(
            UiKit.text(this, "Algoritma Blok", 18f, theme.primaryText, bold = true),
            UiKit.params(this, 28f)
        )
        root.addView(
            UiKit.text(this, "Berlaku untuk permainan baru.", 13f, theme.tertiaryText),
            UiKit.params(this, 2f)
        )
        val blockBlast = RadioButton(this).apply {
            id = View.generateViewId()
            text = "Acak Seimbang (selalu bisa dimainkan)"
        }
        val gradual = RadioButton(this).apply {
            id = View.generateViewId()
            text = "Kesulitan bertahap (makin sulit seiring skor)"
        }
        for (rb in listOf(blockBlast, gradual)) {
            rb.textSize = 16f
            rb.setTextColor(theme.primaryText)
            rb.setPadding(UiKit.dp(this, 8f), UiKit.dp(this, 10f), UiKit.dp(this, 8f), UiKit.dp(this, 10f))
        }
        val algoGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            addView(blockBlast)
            addView(gradual)
            check(if (save.heuristicBlocks) gradual.id else blockBlast.id)
            setOnCheckedChangeListener { _, checkedId ->
                save.heuristicBlocks = checkedId == gradual.id
            }
        }
        root.addView(algoGroup, UiKit.params(this, 4f))

        root.addView(
            UiKit.button(this, "Hapus Data Permainan", Color.rgb(196, 36, 48), Color.WHITE) {
                confirmClear()
            },
            UiKit.params(this, 28f)
        )

        root.addView(
            UiKit.button(this, "Ulangi Tutorial", Color.argb(40, 255, 255, 255), Color.WHITE) {
                confirmReplayTutorial()
            },
            UiKit.params(this, 12f)
        )

        root.addView(
            UiKit.text(this, "Developer", 16f, theme.secondaryText, bold = true),
            UiKit.params(this, 36f)
        )
        root.addView(UiKit.text(this, Dev.NAME, 15f, theme.primaryText), UiKit.params(this, 4f))
        root.addView(
            UiKit.button(this, "Kontak & Dukungan", theme.field, Color.WHITE) {
                UiKit.showDeveloperDialog(this)
            },
            UiKit.params(this, 12f)
        )
        root.addView(
            UiKit.button(this, "Kembali", Color.argb(40, 255, 255, 255), Color.WHITE) { finishWithResult() },
            UiKit.params(this, 28f)
        )
        root.addView(
            UiKit.text(this, "Susun Block v${versionName()}", 13f, theme.tertiaryText),
            UiKit.params(this, 24f)
        )

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                setBackgroundColor(theme.background)
                addView(
                    root,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
            }
        )
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Skor terbaik dan semua progres permainan akan dihapus. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                save.clearAll()
                cleared = true
                Toast.makeText(this, "Data permainan dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmReplayTutorial() {
        AlertDialog.Builder(this)
            .setTitle("Ulangi Tutorial")
            .setMessage("Tutorial akan diputar lagi dan permainan yang sedang berjalan dimulai dari awal. Lanjutkan?")
            .setPositiveButton("Ulangi") { _, _ ->
                save.tutorialDone = false
                replayTutorial = true
                finishWithResult()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun versionName(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun finishWithResult() {
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_CLEARED, cleared).putExtra(EXTRA_REPLAY_TUTORIAL, replayTutorial)
        )
        finish()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        finishWithResult()
    }

    companion object {
        const val EXTRA_CLEARED = "extra_cleared"
        const val EXTRA_REPLAY_TUTORIAL = "extra_replay_tutorial"
    }
}
