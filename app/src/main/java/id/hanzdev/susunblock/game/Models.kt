package id.hanzdev.susunblock.game

/** Bentuk sebuah blok. `cells[y * sizeX + x] == true` berarti sel terisi. */
class BlockShape(val id: Int, val sizeX: Int, val sizeY: Int, val cells: BooleanArray) {
    val count: Int = cells.count { it }

    fun filled(x: Int, y: Int): Boolean = cells[y * sizeX + x]
}

/** Blok yang sedang berada di salah satu slot. */
class Piece(val shapeId: Int, var colorIdx: Int) {
    val shape: BlockShape get() = Shapes.all[shapeId]
}

class ClearedCell(val row: Int, val col: Int, val colorIdx: Int)

class MoveResult(
    val placedCount: Int,
    val placedRows: IntArray,
    val placedCols: IntArray,
    val colorIdx: Int,
    val rowsCleared: Int,
    val colsCleared: Int,
    val cleared: List<ClearedCell>,
    val allClear: Boolean,
    /** Poin yang didapat dari langkah ini, sudah termasuk pengali kombo. */
    val gained: Int,
    /** Jumlah langkah beruntun yang menghapus garis, termasuk langkah ini. 0 bila langkah ini tidak menghapus. */
    val streak: Int
) {
    val linesCleared: Int get() = rowsCleared + colsCleared
}

class TutorialExample(
    val shapeId: Int,
    val targetRow: Int,
    val targetCol: Int,
    /** '#' = terisi, '.' = kosong */
    val grid: List<String>,
    val hint: String
)
