package id.hanzdev.susunblock.game

import android.content.Context

/** Penyimpanan progres dan pengaturan memakai SharedPreferences. */
class SaveManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("susun_block_save", Context.MODE_PRIVATE)

    var sfxOn: Boolean
        get() = prefs.getBoolean("sfx", true)
        set(v) { prefs.edit().putBoolean("sfx", v).apply() }

    var hapticsOn: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(v) { prefs.edit().putBoolean("haptics", v).apply() }

    var tutorialDone: Boolean
        get() = prefs.getBoolean("tutorial_done", false)
        set(v) { prefs.edit().putBoolean("tutorial_done", v).apply() }

    /** Algoritma blok: false = Acak Seimbang, true = kesulitan bertahap. Berlaku untuk permainan baru. */
    var heuristicBlocks: Boolean
        get() = prefs.getBoolean("heuristic_blocks", false)
        set(v) { prefs.edit().putBoolean("heuristic_blocks", v).apply() }

    /** Skor terbaik untuk algoritma tertentu. Tiap algoritma punya catatan sendiri. */
    fun bestScore(heuristic: Boolean): Int = prefs.getInt(bestKey(heuristic), 0)

    /** Skor terbaik untuk algoritma yang sedang dipilih. */
    val bestScore: Int get() = bestScore(heuristicBlocks)

    private fun bestKey(heuristic: Boolean) = if (heuristic) "best_heuristic" else "best_balanced"

    fun save(engine: GameEngine) {
        if (engine.tutorialMode) return
        prefs.edit()
            .putInt(bestKey(engine.heuristicBlocks), engine.bestScore)
            .putBoolean("game_over", engine.gameOver)
            .putInt("score", engine.score)
            .putInt("combo", engine.streak)
            .putInt("theme", engine.themeIndex)
            .putString("field", engine.colors.joinToString(","))
            .putString("pieces", engine.pieces.joinToString(",") {
                if (it == null) "-" else "${it.shapeId}:${it.colorIdx}"
            })
            .apply()
    }

    /** Memuat progres. Bila tidak ada simpanan valid atau game sebelumnya sudah selesai, mulai game baru. */
    fun load(engine: GameEngine) {
        engine.bestScore = bestScore(engine.heuristicBlocks)
        if (prefs.getBoolean("game_over", false) || !restore(engine)) {
            engine.newGame()
            return
        }
        engine.gameOver = false
        if (engine.pieces.all { it == null }) engine.spawnPieces()
        if (engine.isStuck()) engine.newGame()
    }

    /** Mengisi [engine] dari simpanan. false bila data tidak lengkap atau rusak. */
    private fun restore(engine: GameEngine): Boolean {
        try {
            val fieldText = prefs.getString("field", null) ?: return false
            val parts = fieldText.split(",")
            if (parts.size != engine.colors.size) return false
            for (i in parts.indices) engine.colors[i] = parts[i].trim().toInt()

            val pieceParts = (prefs.getString("pieces", "") ?: "").split(",")
            if (pieceParts.size != GameConfig.SLOTS) return false
            for (i in 0 until GameConfig.SLOTS) {
                val text = pieceParts[i].trim()
                engine.pieces[i] = if (text == "-" || text.isEmpty()) {
                    null
                } else {
                    val (id, color) = text.split(":")
                    val shapeId = id.toInt()
                    require(shapeId in Shapes.all.indices)
                    Piece(shapeId, color.toInt())
                }
            }
            engine.score = prefs.getInt("score", 0)
            engine.streak = prefs.getInt("combo", 0).coerceAtLeast(0)
            engine.themeIndex = prefs.getInt("theme", 0).coerceIn(0, Themes.all.size - 1)
            engine.tutorialMode = false
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
