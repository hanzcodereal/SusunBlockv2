<p align="center">
  <img src="assets/logo.png" alt="Logo Susun Block" width="160">
</p>

<h1 align="center">Susun Block</h1>

Game puzzle susun blok untuk Android, dibuat dengan **Kotlin**. Seret blok ke papan 8×8, penuhi baris atau kolom untuk menghapusnya, kejar kombo, dan pecahkan skor terbaikmu!

![Versi](https://img.shields.io/badge/Versi-1.0.0-blue)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-green)
![Bahasa](https://img.shields.io/badge/Kotlin-1.9-purple)
![Lisensi](https://img.shields.io/badge/Lisensi-Non--Commercial-orange)

## Developer

| | |
|---|---|
| **Developer** | Hanz Dev Official |
| **Dukung developer** | https://saweria.co/hanzreally |
| **Kontak** | hanzzcodee@gmail.com |

## Fitur

- **Layar pembuka** – logo, nama aplikasi, indikator memuat, dan kredit developer
- **Dua algoritma blok** – *Acak Seimbang* (acak, selalu bisa dimainkan, makin pintar saat papan padat) dan *Kesulitan bertahap* (makin sulit seiring skor). Bisa dipilih di Pengaturan.
- **Blok acak yang selalu bisa dimainkan** – tiga blok dijamin bisa ditempatkan berurutan di papan saat ini
- **4 tema warna** – tema berganti saat kamu menghapus 2 garis atau lebih sekaligus
- **Tutorial interaktif** – 3 langkah singkat untuk pemain baru
- **Skor, kombo, dan bonus "Semua Bersih!"**
- **Simpan otomatis** – progres tersimpan setiap langkah
- **Main ulang** – tombol Jeda (roda gigi) berisi Lanjutkan, Main Ulang, Pengaturan, dan Menu Utama; tutorial juga bisa diulang dari Pengaturan
- **Kontak developer** – dialog rapi dengan tombol Kirim Email, Salin Email, dan Dukung Developer (dibuka dari Pengaturan)
- **Efek suara & getaran** – bisa dimatikan di Pengaturan (suara dibuat lewat kode, tanpa berkas audio)
- Seluruh teks permainan dalam **Bahasa Indonesia**

## Cara Bermain

1. Seret salah satu dari tiga blok di bawah papan ke papan 8×8.
2. Baris atau kolom yang terisi penuh akan terhapus dan menambah skor.
3. Hapus garis berturut-turut dalam 3 langkah untuk mendapat **kombo**.
4. Kosongkan seluruh papan untuk bonus **+1000**.
5. Permainan selesai saat tidak ada blok yang muat.

### Rumus skor (pola Block Blast)

| Aksi | Poin |
|---|---|
| Tiap sel blok yang diletakkan | +1 |
| Tiap sel yang ikut terhapus | +10 × jumlah garis yang terhapus sekaligus |
| Pengali kombo | langkah beruntun ke-n dikali (1 + 0,5 × (n − 1)) |
| Papan bersih | +1000 |

Contoh: satu baris terhapus = 8 sel × 10 = 80 poin. Dua baris sekaligus = 15 sel unik (sel perpotongan dihitung sekali) × 10 × 2 = 300 poin.
Kombo putus bila satu langkah tidak menghapus garis apa pun.

> Rumus Block Blast asli tidak dipublikasikan. Angka di atas adalah pola yang umum dipahami dari permainannya, dan semua konstantanya ada di `game/GameConfig.kt` sehingga mudah diubah.

## Build

### Persyaratan

- JDK 17
- Android SDK (platform 34)
- Gradle 8.7+ (atau buat wrapper: `gradle wrapper --gradle-version 8.7`)

### Lokal

```bash
gradle assembleDebug     # APK debug  -> app/build/outputs/apk/debug/
gradle assembleRelease   # APK release -> app/build/outputs/apk/release/
```

Atau buka folder proyek ini di **Android Studio** lalu jalankan modul `app`.

## Rilis lewat GitHub (manual)

Build **tidak berjalan otomatis**. Workflow `.github/workflows/build-release.yml` hanya jalan saat kamu memulainya sendiri:

1. Buka repo di GitHub, lalu tab **Actions**.
2. Pilih **Build APK (Manual)** di daftar kiri.
3. Klik **Run workflow**, isi **nomor versi** (mis. `1.0.0`), lalu pilih apakah hasilnya dipublikasikan ke **GitHub Release**.
4. Klik **Run workflow** (hijau) dan tunggu sampai selesai.

APK hasil build bernama `SusunBlock-vX.Y.Z.apk`. Unduh dari halaman **Releases**, atau dari bagian **Artifacts** di halaman run jika opsi Release dimatikan.

### Tanda tangan rilis (opsional, disarankan)

Tanpa keystore, APK ditandatangani dengan debug key (tetap bisa dipasang, tapi tidak cocok untuk Play Store). Untuk memakai keystore sendiri, buat lalu simpan sebagai **Repository secrets**:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias susunblock
base64 -w0 release.jks   # salin hasilnya ke secret KEYSTORE_BASE64
```

| Secret | Isi |
|---|---|
| `KEYSTORE_BASE64` | isi `release.jks` dalam base64 |
| `KEYSTORE_PASSWORD` | kata sandi keystore |
| `KEY_ALIAS` | alias kunci (mis. `susunblock`) |
| `KEY_PASSWORD` | kata sandi kunci |

> Simpan keystore-mu baik-baik. Update aplikasi harus ditandatangani dengan kunci yang sama.

## Struktur Proyek

```
app/src/main/java/id/hanzdev/susunblock/
├── SplashActivity.kt      # Layar pembuka (logo, nama, kredit)
├── MainActivity.kt        # Menu utama
├── GameActivity.kt        # Layar permainan
├── SettingsActivity.kt    # Pengaturan
├── Dev.kt                 # Info developer
├── game/
│   ├── GameEngine.kt      # Papan, skor, kombo, pembuat blok
│   ├── Shapes.kt          # Daftar bentuk blok
│   ├── Themes.kt          # Tema warna
│   ├── TutorialData.kt    # Contoh tutorial
│   ├── SaveManager.kt     # Simpan/muat progres & pengaturan
│   ├── SoundManager.kt    # Efek suara
│   ├── GameConfig.kt      # Konstanta permainan
│   └── Models.kt
└── ui/
    ├── GameView.kt        # Tampilan game (Canvas, drag & drop, animasi)
    └── UiKit.kt           # Helper tampilan menu
```

## Pengembangan

### Menambah blok baru

Buka `game/Shapes.kt`, tambahkan satu baris dan perbarui `largeIds`/`smallIds`:

```kotlin
s(39, 3, 2, "110011"),   // id, lebar, tinggi, sel (1 = terisi, baris demi baris)
```

### Menambah tema baru

Buka `game/Themes.kt` dan tambahkan `GameTheme(...)` ke daftar `Themes.all`.

### Pembuat blok

Pilih di **Pengaturan → Algoritma Blok**. Perubahan berlaku untuk permainan baru:

**Acak Seimbang**:
1. **Bentuk acak berbobot** – tiap bentuk punya bobot dasar di `Shapes.baseWeight()`. Bentuk boleh berulang.
2. **Menyesuaikan kepadatan papan** – saat papan mulai padat, blok yang punya banyak posisi muat lebih sering dipilih, dan blok besar (5 sel ke atas) dikurangi.
3. **Minimal satu blok bisa menghapus garis** – saat papan sudah separuh penuh, dicoba agar salah satu dari tiga blok bisa langsung menghapus garis.
4. **Jaminan bisa dimainkan** – tiga blok yang keluar selalu bisa ditempatkan berurutan. Posisi yang menghapus garis diutamakan. Bila jalan buntu, pencarian mundur mencoba kemungkinan lain (maksimal 600 percobaan).

**Kesulitan bertahap** (heuristik kesulitan):
1. **Kecocokan penempatan** – setiap bentuk dinilai dari posisi terbaiknya: menghapus garis, menempel rapi, dan tidak meninggalkan lubang kosong yang terkurung.
2. **Kesulitan naik bertahap** – dari 0,35 sampai 1 mengikuti skor (penuh di 8000 poin). Makin sulit, makin jarang blok yang pas, makin sering blok canggung.
3. **Bantuan** – saat papan mulai penuh, blok pertama kadang diusahakan bisa menghapus garis. Peluangnya berkurang seiring kesulitan.
4. **Ukuran menyesuaikan papan** – papan kosong lebih sering memberi blok besar, papan padat lebih sering memberi blok kecil.

Rumus skor di atas mengikuti pola permainan Block Blast yang umum dipahami. Algoritma asli Block Blast tidak dipublikasikan, jadi pembuat blok dibuat sendiri.

Konstanta ada di `game/GameConfig.kt`.


## Kredit & Lisensi

Susun Block adalah port Kotlin/Android dari proyek Unity **BlockPuzzle** karya **Babken Inants**. Kode asli dirilis di bawah *Non-Commercial Open License* (lihat [LICENSE](LICENSE)): penggunaan **non-komersial**, atribusi wajib dipertahankan, dan karya turunan harus memakai lisensi yang sama.

## Kontak & Dukungan

- Kontak: hanzzcodee@gmail.com
- Dukung developer: https://saweria.co/hanzreally
