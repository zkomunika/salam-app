package app.pantauizin.ui.terlambat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import app.pantauizin.domain.SendAction

object TemplateSender {
    fun execute(context: Context, action: SendAction) {
        when (action) {
            is SendAction.OpenWhatsApp -> {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(viewIntent)
                } catch (_: Exception) {
                    val text = Uri.parse(action.url).getQueryParameter("text").orEmpty()
                    shareAndCopy(context, text)
                }
            }
            is SendAction.ShareText -> {
                shareAndCopy(context, action.text)
            }
        }
    }

    private fun shareAndCopy(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Pesan Wali", text))
        Toast.makeText(context, "Teks disalin", Toast.LENGTH_SHORT).show()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Kirim pesan ke wali").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Tidak ada aplikasi pembagi teks
        }
    }
}

