package app.pantauizin.domain

/** Teks template bawaan, disalin persis dari spec bagian 6.1. */
object DefaultTemplates {
    const val A: String = """Assalamualaikum Warahmatullahi Wabarakatuh.

Kami dari Pengurus Asrama hendak memberitahukan bahwa putra Bapak/Ibu yang bernama {nama} (kamar {kamar}) telah melewati batas waktu izin pulang yang ditentukan, yaitu {batas_awal}, dan sampai saat ini belum kembali ke pondok.

Mohon Bapak/Ibu berkenan memberikan keterangan mengenai keterlambatan tersebut serta kapan putra Bapak/Ibu akan kembali.

Atas perhatian dan kerja samanya, kami ucapkan terima kasih.
Wassalamualaikum Warahmatullahi Wabarakatuh."""

    const val B: String = """Assalamualaikum Warahmatullahi Wabarakatuh.

Kami dari Pengurus Asrama hendak memberitahukan bahwa putra Bapak/Ibu yang bernama {nama} (kamar {kamar}) sebelumnya telah dikonfirmasi akan kembali paling lambat {batas_aktif}, dan sampai saat ini belum kembali ke pondok.

Mohon Bapak/Ibu berkenan memberikan keterangan terbaru mengenai kondisi putra Bapak/Ibu serta kapan akan kembali.

Atas perhatian dan kerja samanya, kami ucapkan terima kasih.
Wassalamualaikum Warahmatullahi Wabarakatuh."""
}

