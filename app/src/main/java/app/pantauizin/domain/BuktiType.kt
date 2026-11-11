package app.pantauizin.domain

enum class BuktiJenis { GAMBAR, PDF, LAINNYA }

object BuktiType {
    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp")

    /**
     * Menentukan jenis bukti. Isi berkas ([header], beberapa byte awal) didahulukan karena
     * penyimpanan bisa memakai ekstensi `bin` bila mime sumber tidak diketahui.
     */
    fun detect(path: String, header: ByteArray?): BuktiJenis {
        fromHeader(header)?.let { return it }
        return when (path.substringAfterLast('.', "").lowercase()) {
            "pdf" -> BuktiJenis.PDF
            in IMAGE_EXT -> BuktiJenis.GAMBAR
            else -> BuktiJenis.LAINNYA
        }
    }

    private fun fromHeader(h: ByteArray?): BuktiJenis? {
        if (h == null) return null
        if (h.size >= 4 && h[0] == '%'.code.toByte() && h[1] == 'P'.code.toByte() &&
            h[2] == 'D'.code.toByte() && h[3] == 'F'.code.toByte()
        ) return BuktiJenis.PDF
        if (h.size >= 3 && h[0] == 0xFF.toByte() && h[1] == 0xD8.toByte() && h[2] == 0xFF.toByte()) {
            return BuktiJenis.GAMBAR
        }
        if (h.size >= 4 && h[0] == 0x89.toByte() && h[1] == 0x50.toByte() &&
            h[2] == 0x4E.toByte() && h[3] == 0x47.toByte()
        ) return BuktiJenis.GAMBAR
        if (h.size >= 12 && String(h, 0, 4) == "RIFF" && String(h, 8, 4) == "WEBP") {
            return BuktiJenis.GAMBAR
        }
        return null
    }
}

