package id.hanzdev.susunblock.game

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Efek suara sederhana yang dibuat langsung lewat kode (tanpa berkas audio),
 * sehingga tidak ada masalah lisensi aset suara.
 *
 * Data suara dihitung sekali lalu disimpan di cache bersama. [preload] dipanggil
 * saat aplikasi dibuka agar tidak ada penghitungan saat permainan berlangsung.
 */
class SoundManager(private val save: SaveManager) {
    private val pool = Executors.newCachedThreadPool()

    fun pickup() = play(FX_PICKUP, 35)
    fun place() = play(FX_PLACE, 70)
    fun button() = play(FX_BUTTON, 50)
    fun lines(count: Int) = play(linesFreqs((count + 2).coerceAtMost(6)), 70)
    fun allClear() = play(FX_ALL_CLEAR, 90)
    fun gameOver() = play(FX_GAME_OVER, 170)
    fun newBest() = play(FX_NEW_BEST, 110)

    private fun play(freqs: IntArray, msEach: Int) {
        if (!save.sfxOn) return
        pool.execute {
            try {
                val samples = buffer(freqs, msEach)
                @Suppress("DEPRECATION")
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    samples.size * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(msEach.toLong() * freqs.size + 80)
                track.release()
            } catch (e: Exception) {
                // Suara bersifat opsional, abaikan kesalahan.
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050

        private val FX_PICKUP = intArrayOf(700)
        private val FX_PLACE = intArrayOf(330)
        private val FX_BUTTON = intArrayOf(520)
        private val FX_ALL_CLEAR = intArrayOf(523, 659, 784, 1047, 1319)
        private val FX_GAME_OVER = intArrayOf(392, 330, 262, 196)
        private val FX_NEW_BEST = intArrayOf(523, 659, 784, 1047)
        private fun linesFreqs(n: Int) = IntArray(n) { 440 + it * 110 }

        private val cache = ConcurrentHashMap<String, ShortArray>()

        private fun buffer(freqs: IntArray, msEach: Int): ShortArray =
            cache.getOrPut("${freqs.joinToString(",")}@$msEach") { synth(freqs, msEach) }

        /** Membuat semua efek suara di muka. Aman dipanggil dari thread latar. */
        fun preload() {
            buffer(FX_PICKUP, 35)
            buffer(FX_PLACE, 70)
            buffer(FX_BUTTON, 50)
            for (n in 2..6) buffer(linesFreqs(n), 70)
            buffer(FX_ALL_CLEAR, 90)
            buffer(FX_GAME_OVER, 170)
            buffer(FX_NEW_BEST, 110)
        }

        private fun synth(freqs: IntArray, msEach: Int): ShortArray {
            val perNote = SAMPLE_RATE * msEach / 1000
            val samples = ShortArray(perNote * freqs.size)
            var pos = 0
            for (f in freqs) {
                for (i in 0 until perNote) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val attack = (i / (SAMPLE_RATE * 0.005)).coerceAtMost(1.0)
                    val release = ((perNote - i) / (perNote * 0.4)).coerceAtMost(1.0)
                    val env = attack * release
                    samples[pos++] = (sin(2.0 * PI * f * t) * 0.3 * 32767 * env).toInt().toShort()
                }
            }
            return samples
        }
    }
}
