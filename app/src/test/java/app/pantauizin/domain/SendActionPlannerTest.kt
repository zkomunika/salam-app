package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder

class SendActionPlannerTest {
    @Test
    fun planDenganNomorWaMengembalikanOpenWhatsApp() {
        val teks = "Assalamualaikum Bapak/Ibu, santri ZAKY terlambat kembali."
        val expectedUrl = "https://wa.me/6281234567890?text=" + URLEncoder.encode(teks, "UTF-8")
        assertEquals(
            SendAction.OpenWhatsApp(expectedUrl),
            SendActionPlanner.plan(teks, "6281234567890")
        )
    }

    @Test
    fun planTanpaNomorWaMengembalikanShareText() {
        val teks = "Assalamualaikum Bapak/Ibu, santri ZAKY terlambat kembali."
        assertEquals(SendAction.ShareText(teks), SendActionPlanner.plan(teks, null))
        assertEquals(SendAction.ShareText(teks), SendActionPlanner.plan(teks, "   "))
    }
}

