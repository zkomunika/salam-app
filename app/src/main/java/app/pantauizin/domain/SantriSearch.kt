package app.pantauizin.domain

data class SantriItem(val id: Long, val nama: String, val kamar: String, val aktif: Boolean)

object SantriSearch {
    /**
     * Hanya santri aktif; cocok sebagian tanpa beda huruf; query di-trim dan spasi ganda dirapikan;
     * query kosong = semua aktif; [kamar] non-null membatasi ke kamar itu; urut nama.
     */
    fun filter(all: List<SantriItem>, query: String, kamar: String? = null): List<SantriItem> {
        val q = query.trim().replace(Regex("\\s+"), " ").lowercase()
        return all.asSequence()
            .filter { it.aktif }
            .filter { kamar == null || it.kamar == kamar }
            .filter { q.isEmpty() || it.nama.lowercase().contains(q) }
            .sortedBy { it.nama.lowercase() }
            .toList()
    }

    /** Kamar unik dari santri aktif; angka dulu secara numerik, lalu teks non-angka secara abjad. */
    fun kamarOptions(all: List<SantriItem>): List<String> {
        val unik = all.filter { it.aktif }.map { it.kamar }.distinct()
        val (angka, teks) = unik.partition { it.toLongOrNull() != null }
        return angka.sortedBy { it.toLong() } + teks.sortedBy { it.lowercase() }
    }
}

