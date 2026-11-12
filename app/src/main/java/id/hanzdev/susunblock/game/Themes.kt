package id.hanzdev.susunblock.game

class GameTheme(
    val name: String,
    val blockColors: IntArray,
    val primaryText: Int,
    val secondaryText: Int,
    val tertiaryText: Int,
    val field: Int,
    val background: Int,
    val cellDefault: Int
)

object Themes {
    private fun c(rgb: Int): Int = rgb or (0xFF shl 24)

    val all: List<GameTheme> = listOf(
        GameTheme(
            name = "Sweetie 16",
            blockColors = intArrayOf(
                c(0x38B863), c(0x5D275D), c(0xB13E52), c(0xEF7D57), c(0x267179), c(0x29366F)
            ),
            primaryText = c(0xF4F4F4), secondaryText = c(0xFFCD75), tertiaryText = c(0x74EFF7),
            field = c(0x566C86), background = c(0x333C57), cellDefault = c(0x95B0C2)
        ),
        GameTheme(
            name = "Endesga 64",
            blockColors = intArrayOf(
                c(0x33984B), c(0xC42430), c(0xED7614), c(0xFFCD00), c(0xDB3FFD),
                c(0x7A09FA), c(0x00CDF9), c(0x8A4836), c(0xD3FC7E), c(0xFDD2ED)
            ),
            primaryText = c(0xFFFFFF), secondaryText = c(0xFFA214), tertiaryText = c(0x94FDFF),
            field = c(0x00396D), background = c(0x03193F), cellDefault = c(0x0069AA)
        ),
        GameTheme(
            name = "Twilight Bog",
            blockColors = intArrayOf(
                c(0x4E5A6D), c(0x785A96), c(0xA66470), c(0x5D8DA2), c(0xA48DB6)
            ),
            primaryText = c(0xE1E6EA), secondaryText = c(0x737546), tertiaryText = c(0xB8CFB9),
            field = c(0x737546), background = c(0x383F39), cellDefault = c(0x839C77)
        ),
        GameTheme(
            name = "Vinik 24",
            blockColors = intArrayOf(
                c(0x6EAA78), c(0x8B5480), c(0x666093), c(0x9A4F50), c(0x7E9E99),
                c(0x6E6962), c(0xC28D75)
            ),
            primaryText = c(0xFFFFFF), secondaryText = c(0xFFFFFF), tertiaryText = c(0xFFFFFF),
            field = c(0x5D6872), background = c(0xC5CCB8), cellDefault = c(0x93A167)
        )
    )

    fun lerp(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        val ca = ((a ushr 24) and 0xFF)
        val cb = ((b ushr 24) and 0xFF)
        val alpha = (ca + (cb - ca) * tt).toInt()
        val r = (((a shr 16) and 0xFF) + ((((b shr 16) and 0xFF) - ((a shr 16) and 0xFF)) * tt)).toInt()
        val g = (((a shr 8) and 0xFF) + ((((b shr 8) and 0xFF) - ((a shr 8) and 0xFF)) * tt)).toInt()
        val bl = ((a and 0xFF) + (((b and 0xFF) - (a and 0xFF)) * tt)).toInt()
        return (alpha shl 24) or (r shl 16) or (g shl 8) or bl
    }

    fun luminance(color: Int): Float {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }
}
