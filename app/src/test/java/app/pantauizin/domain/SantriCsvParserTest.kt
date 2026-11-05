package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SantriCsvParserTest {
    @Test fun headerDilewati() {
        val hasil = SantriCsvParser.parse("nama,kamar\nABC,1\n")
        assertEquals(listOf(SantriRow("ABC", "1")), hasil)
    }

    @Test fun spasiGandaDanApostrofDirapikan() {
        val hasil = SantriCsvParser.parse("nama,kamar\n\"DRIYA  NUR  'AZMI\",12\n".replace("\"DRIYA", "DRIYA").replace("AZMI\"", "AZMI"))
        assertEquals(listOf(SantriRow("DRIYA NUR 'AZMI", "12")), hasil)
    }

    @Test fun spasiDiTepiDirapikan() {
        val hasil = SantriCsvParser.parse("nama,kamar\n  ZAKY FAUZI  , 5 \n")
        assertEquals(listOf(SantriRow("ZAKY FAUZI", "5")), hasil)
    }

    @Test fun kamarKosongDilewati() {
        val hasil = SantriCsvParser.parse("nama,kamar\nJENJEN ABDUSSALAM,\nABC,1\n")
        assertEquals(listOf(SantriRow("ABC", "1")), hasil)
    }

    @Test fun namaKosongDilewati() {
        val hasil = SantriCsvParser.parse("nama,kamar\n,3\nABC,1\n")
        assertEquals(listOf(SantriRow("ABC", "1")), hasil)
    }

    @Test fun barisKosongDilewati() {
        val hasil = SantriCsvParser.parse("nama,kamar\n\nABC,1\n\n   \nDEF,2\n")
        assertEquals(listOf(SantriRow("ABC", "1"), SantriRow("DEF", "2")), hasil)
    }

    @Test fun namaKembarBedaHurufHanyaSatu() {
        val hasil = SantriCsvParser.parse("nama,kamar\nABC,1\nabc,2\n")
        assertEquals(listOf(SantriRow("ABC", "1")), hasil)
    }

    @Test fun fieldBerkutipTerbacaUtuh() {
        val hasil = SantriCsvParser.parse("nama,kamar\n\"A, B\",3\n")
        assertEquals(listOf(SantriRow("A, B", "3")), hasil)
    }

    @Test fun apostrofLangsungTerbaca() {
        val hasil = SantriCsvParser.parse("nama,kamar\nDRIYA NUR 'AZMI,12\n")
        assertEquals(listOf(SantriRow("DRIYA NUR 'AZMI", "12")), hasil)
    }

    @Test fun spasiGandaDalamNamaTanpaKutip() {
        val hasil = SantriCsvParser.parse("nama,kamar\nDRIYA  NUR  'AZMI,12\n")
        assertEquals(listOf(SantriRow("DRIYA NUR 'AZMI", "12")), hasil)
    }
}

