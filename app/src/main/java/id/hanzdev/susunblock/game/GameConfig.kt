package id.hanzdev.susunblock.game

/** Konstanta permainan. */
object GameConfig {
    const val ROWS = 8
    const val COLS = 8
    const val SLOTS = 3

    // Skor (mengikuti pola Block Blast: poin per sel, dikali jumlah garis dan pengali kombo)
    /** Poin per sel yang ikut terhapus. Satu baris penuh = 8 sel = 80 poin. */
    const val POINTS_PER_CLEARED_CELL = 10
    /** Tambahan pengali per langkah beruntun: langkah ke-2 x1.5, ke-3 x2, dan seterusnya. */
    const val COMBO_STEP = 0.5f
    /** Bonus saat papan benar-benar kosong. */
    const val ALL_CLEAR_BONUS = 1000

    // Pembuat blok klasik: pencarian mundur
    /** Batas percobaan penempatan saat mencari tiga blok yang bisa dimainkan berurutan. Menjaga kecepatan di HP. */
    const val SEARCH_BUDGET = 600

    // Mode Acak Seimbang
    /** Berapa kali mencoba mencari tiga blok sebelum menerima hasil terbaik yang ada. */
    const val BALANCED_ATTEMPTS = 4
    /** Mulai kepadatan papan ini, minimal satu dari tiga blok harus bisa menghapus garis. */
    const val BALANCED_HELPER_FILL = 0.5f
    /** Jumlah posisi muat yang sudah dianggap banyak saat memilih blok. */
    const val BALANCED_SPOT_CAP = 12

    // Pembuat blok mode heuristik: kesulitan bertahap
    /** Bobot nilai tiap baris/kolom yang bisa dihapus oleh sebuah penempatan. */
    const val LINE_CLEAR_SCORE = 60f
    /** Seberapa besar penempatan yang bagus memengaruhi peluang sebuah blok dipilih. */
    const val FIT_WEIGHT = 3.0
    /** Mulai kepadatan papan ini, blok pertama kadang diusahakan bisa menghapus garis. */
    const val HELPER_BUSY_FRACTION = 0.5f
    /** Kesulitan awal (0..1). Naik bertahap sampai 1 saat skor mencapai DIFFICULTY_FULL_SCORE. */
    const val DIFFICULTY_BASE = 0.35f
    const val DIFFICULTY_FULL_SCORE = 8000f
    /** Hukuman untuk lubang kosong yang terkurung (1, 2, dan 3 sel). */
    const val HOLE_1_PENALTY = 20f
    const val HOLE_2_PENALTY = 10f
    const val HOLE_3_PENALTY = 4f
}
