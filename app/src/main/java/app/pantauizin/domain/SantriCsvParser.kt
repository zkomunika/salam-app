package app.pantauizin.domain

data class SantriRow(val nama: String, val kamar: String)

object SantriCsvParser {
    /**
     * Membaca CSV `nama,kamar`. Melewati header, baris kosong, nama/kamar kosong, dan nama kembar
     * (tanpa beda huruf; yang pertama disimpan). Spasi dirapikan; field berkutip didukung.
     */
    fun parse(csv: String): List<SantriRow> {
        val terlihat = HashSet<String>()
        val hasil = mutableListOf<SantriRow>()
        for (barisMentah in csv.removePrefix("\uFEFF").lineSequence()) {
            if (barisMentah.isBlank()) continue
            val kolom = splitLine(barisMentah)
            val nama = rapikan(kolom.getOrNull(0).orEmpty())
            val kamar = rapikan(kolom.getOrNull(1).orEmpty())
            if (nama.equals("nama", ignoreCase = true) && kamar.equals("kamar", ignoreCase = true)) continue
            if (nama.isEmpty() || kamar.isEmpty()) continue
            if (!terlihat.add(nama.lowercase())) continue
            hasil += SantriRow(nama, kamar)
        }
        return hasil
    }

    private fun rapikan(s: String): String = s.trim().replace(Regex("\\s+"), " ")

    private fun splitLine(line: String): List<String> {
        val hasil = mutableListOf<String>()
        val sb = StringBuilder()
        var dalamKutip = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                dalamKutip && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"')
                    i++
                }
                c == '"' -> dalamKutip = !dalamKutip
                c == ',' && !dalamKutip -> {
                    hasil += sb.toString()
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        hasil += sb.toString()
        return hasil
    }
}

