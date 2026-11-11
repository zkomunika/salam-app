package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BuktiTypeTest {
    private val pdfHeader = "%PDF-1.7".toByteArray()
    private val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    private val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test
    fun ekstensiGambarDikenali() {
        assertEquals(BuktiJenis.GAMBAR, BuktiType.detect("bukti/a.jpg", null))
        assertEquals(BuktiJenis.GAMBAR, BuktiType.detect("bukti/a.JPEG", null))
        assertEquals(BuktiJenis.GAMBAR, BuktiType.detect("bukti/a.png", null))
        assertEquals(BuktiJenis.GAMBAR, BuktiType.detect("bukti/a.webp", null))
    }

    @Test
    fun ekstensiPdfDikenali() {
        assertEquals(BuktiJenis.PDF, BuktiType.detect("bukti/a.pdf", null))
    }

    @Test
    fun ekstensiBinDeteksiDariIsiBerkas() {
        assertEquals(BuktiJenis.PDF, BuktiType.detect("bukti/a.bin", pdfHeader))
        assertEquals(BuktiJenis.GAMBAR, BuktiType.detect("bukti/a.bin", jpegHeader))
        assertEquals(BuktiJenis.GAMBAR, BuktiType.detect("bukti/a.bin", pngHeader))
    }

    @Test
    fun isiBerkasMengalahkanEkstensiYangSalah() {
        assertEquals(BuktiJenis.PDF, BuktiType.detect("bukti/a.jpg", pdfHeader))
    }

    @Test
    fun takDikenaliJadiLainnya() {
        assertEquals(BuktiJenis.LAINNYA, BuktiType.detect("bukti/a.bin", null))
        assertEquals(BuktiJenis.LAINNYA, BuktiType.detect("bukti/a.bin", byteArrayOf(1, 2, 3)))
        assertEquals(BuktiJenis.LAINNYA, BuktiType.detect("bukti/a.docx", null))
    }
}

