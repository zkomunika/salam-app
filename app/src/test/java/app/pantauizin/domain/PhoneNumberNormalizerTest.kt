package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberNormalizerTest {
    private fun valid(s: String) = PhoneResult.Valid(s)

    @Test fun formatBerbedaMenjadiSama() {
        assertEquals(valid("6281234567890"), PhoneNumberNormalizer.normalize("0812-3456-7890"))
        assertEquals(valid("6281234567890"), PhoneNumberNormalizer.normalize("+62 812 3456 7890"))
        assertEquals(valid("6281234567890"), PhoneNumberNormalizer.normalize("6281234567890"))
        assertEquals(valid("6281234567890"), PhoneNumberNormalizer.normalize("81234567890"))
    }

    @Test fun kurungDanStrip() {
        assertEquals(valid("628123456789"), PhoneNumberNormalizer.normalize("(0812) 345-6789"))
    }

    @Test fun kosongDianggapTidakAda() {
        assertEquals(PhoneResult.Empty, PhoneNumberNormalizer.normalize(""))
        assertEquals(PhoneResult.Empty, PhoneNumberNormalizer.normalize("   "))
        assertEquals(PhoneResult.Empty, PhoneNumberNormalizer.normalize(null))
    }

    @Test fun tidakValid() {
        assertEquals(PhoneResult.Invalid, PhoneNumberNormalizer.normalize("abc"))
        assertEquals(PhoneResult.Invalid, PhoneNumberNormalizer.normalize("0812"))
        assertEquals(PhoneResult.Invalid, PhoneNumberNormalizer.normalize("0812345678901234567"))
    }

    @Test fun berisiHurufDitolak() {
        assertEquals(PhoneResult.Invalid, PhoneNumberNormalizer.normalize("0812abc34567"))
    }
}

