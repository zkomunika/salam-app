package app.pantauizin.domain

import java.net.URLEncoder

sealed interface SendAction {
    data class OpenWhatsApp(val url: String) : SendAction
    data class ShareText(val text: String) : SendAction
}

object SendActionPlanner {
    fun plan(templateText: String, noWaWali: String?): SendAction {
        val nomor = noWaWali?.trim()
        return if (!nomor.isNullOrEmpty()) {
            val encoded = URLEncoder.encode(templateText, "UTF-8")
            SendAction.OpenWhatsApp("https://wa.me/$nomor?text=$encoded")
        } else {
            SendAction.ShareText(templateText)
        }
    }
}

