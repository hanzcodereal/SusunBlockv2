package id.hanzdev.susunblock

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import id.hanzdev.susunblock.game.GameEngine
import id.hanzdev.susunblock.game.SaveManager
import id.hanzdev.susunblock.game.SoundManager
import id.hanzdev.susunblock.ui.GameView

/** Layar permainan. */
class GameActivity : Activity() {
    private lateinit var save: SaveManager
    private lateinit var engine: GameEngine
    private lateinit var gameView: GameView
    /** Setelah data dihapus dari Pengaturan, progres di memori tidak boleh disimpan lagi. */
    private var dataCleared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        save = SaveManager(this)
        engine = GameEngine(heuristicBlocks = save.heuristicBlocks)
        save.load(engine)
        val sound = SoundManager(save)

        gameView = GameView(this, engine, save, sound, object : GameView.Callbacks {
            override fun onOpenSettings() {
                if (!engine.tutorialMode) save.save(engine)
                @Suppress("DEPRECATION")
                startActivityForResult(
                    Intent(this@GameActivity, SettingsActivity::class.java), REQ_SETTINGS
                )
            }

            override fun onExitToMenu() {
                finish()
            }
        })
        setContentView(gameView)
        gameView.startTutorialIfNeeded()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_SETTINGS) return
        if (data?.getBooleanExtra(SettingsActivity.EXTRA_CLEARED, false) == true) {
            dataCleared = true
            gameView.resetAfterDataCleared()
        } else if (data?.getBooleanExtra(SettingsActivity.EXTRA_REPLAY_TUTORIAL, false) == true) {
            gameView.replayTutorial()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (!gameView.handleBack()) super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        if (!engine.tutorialMode && !dataCleared) save.save(engine)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
    }

    companion object {
        const val REQ_SETTINGS = 1001
    }
}
