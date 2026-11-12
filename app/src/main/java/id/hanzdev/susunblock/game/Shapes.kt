package id.hanzdev.susunblock.game

/** Semua bentuk blok. Id 0..29 = blok besar, id 30..38 = blok kecil. */
object Shapes {
    private fun s(id: Int, sx: Int, sy: Int, bits: String) =
        BlockShape(id, sx, sy, BooleanArray(bits.length) { bits[it] == '1' })

    val all: List<BlockShape> = listOf(
        // Blok besar
        s(0, 1, 4, "1111"),
        s(1, 3, 2, "010111"),
        s(2, 2, 3, "111010"),
        s(3, 2, 3, "101101"),
        s(4, 2, 3, "011110"),
        s(5, 3, 2, "110011"),
        s(6, 3, 2, "011110"),
        s(7, 2, 2, "1111"),
        s(8, 3, 2, "111111"),
        s(9, 3, 3, "111111111"),
        s(10, 3, 3, "111100100"),
        s(11, 3, 3, "111001001"),
        s(12, 3, 3, "001001111"),
        s(13, 3, 3, "100100111"),
        s(14, 1, 5, "11111"),
        s(15, 5, 1, "11111"),
        s(16, 4, 1, "1111"),
        s(17, 2, 3, "101110"),
        s(18, 2, 3, "011101"),
        s(19, 3, 2, "111010"),
        s(20, 2, 3, "111111"),
        s(21, 2, 3, "110101"),
        s(22, 3, 2, "111100"),
        s(23, 3, 2, "111001"),
        s(24, 2, 3, "101011"),
        s(25, 2, 3, "010111"),
        s(26, 1, 3, "111"),
        s(27, 3, 1, "111"),
        s(28, 3, 3, "100010001"),
        s(29, 3, 3, "001010100"),
        // Blok kecil
        s(30, 2, 2, "0111"),
        s(31, 2, 2, "1101"),
        s(32, 2, 2, "1110"),
        s(33, 2, 2, "1011"),
        s(34, 1, 2, "11"),
        s(35, 2, 1, "11"),
        s(36, 2, 2, "1001"),
        s(37, 2, 2, "0110"),
        s(38, 1, 1, "1")
    )

    /**
     * Pengali peluang dasar tiap bentuk (1.0 = normal, 2.0 = dua kali lebih sering, 0.0 = tidak pernah muncul).
     * Ini hanya "tuas" dasar; pembuat blok cerdas tetap menyesuaikan dengan kondisi papan dan kesulitan.
     * Bentuk yang tidak tercantum dianggap 1.0.
     */
    private val baseWeights: Map<Int, Double> = mapOf(
        // Blok besar
        0 to 1.0,   // garis tegak 4
        1 to 1.0, 2 to 1.0, 3 to 1.0, 4 to 1.0, 5 to 1.0, 6 to 1.0,
        7 to 1.0,   // kotak 2x2
        8 to 1.7,   // persegi panjang 3x2
        9 to 2.8,   // kotak 3x3 penuh (sebelumnya paling jarang muncul)
        10 to 1.2, 11 to 1.2, 12 to 1.2, 13 to 1.2, // L besar 3x3
        14 to 1.0, 15 to 1.0, // garis 5
        16 to 1.0,  // garis datar 4
        17 to 1.0, 18 to 1.0, 19 to 1.0,
        20 to 1.9,  // persegi panjang 2x3
        21 to 1.0, 22 to 1.0, 23 to 1.0, 24 to 1.0, 25 to 1.0,
        26 to 1.0, 27 to 1.0, // garis 3
        28 to 1.0, 29 to 1.0, // diagonal 3x3
        // Blok kecil
        30 to 1.0, 31 to 1.0, 32 to 1.0, 33 to 1.0,
        34 to 1.4, 35 to 1.4, // domino
        36 to 1.6,  // dua sel diagonal
        37 to 1.3,  // diagonal 2x2
        38 to 1.4   // satu sel
    )

    fun baseWeight(id: Int): Double = baseWeights[id] ?: 1.0

    val largeIds: List<Int> = (0..29).toList()
    val smallIds: List<Int> = (30..38).toList()
}
