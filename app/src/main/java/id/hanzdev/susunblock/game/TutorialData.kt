package id.hanzdev.susunblock.game

object TutorialData {
    val examples: List<TutorialExample> = listOf(
        TutorialExample(
            shapeId = 0, targetRow = 2, targetCol = 3,
            grid = listOf(
                "...#....",
                "...#....",
                "........",
                "........",
                "........",
                "........",
                "...#....",
                "...#...."
            ),
            hint = "Seret blok ke tempat yang ditandai\nuntuk memenuhi satu kolom."
        ),
        TutorialExample(
            shapeId = 1, targetRow = 3, targetCol = 2,
            grid = listOf(
                "........",
                "........",
                "........",
                "###.####",
                "##...###",
                "........",
                "........",
                "........"
            ),
            hint = "Baris atau kolom yang penuh\nakan terhapus dan memberi skor."
        ),
        TutorialExample(
            shapeId = 7, targetRow = 3, targetCol = 3,
            grid = listOf(
                "...##...",
                "...##...",
                "...##...",
                "###..###",
                "###..###",
                "...##...",
                "...##...",
                "...##..."
            ),
            hint = "Hapus beberapa garis sekaligus\nuntuk skor dan kombo lebih besar!"
        )
    )
}
