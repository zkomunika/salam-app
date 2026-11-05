package app.pantauizin.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

interface BuktiStorage {
    suspend fun salin(source: Uri): String
    fun hapus(path: String)
    fun file(path: String): File
}

class AndroidBuktiStorage(private val context: Context) : BuktiStorage {
    override suspend fun salin(source: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "bukti")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Gagal membuat direktori bukti")
        }
        val mime = context.contentResolver.getType(source)
        val ext = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
        val fileName = "${UUID.randomUUID()}.$ext"
        val relativePath = "bukti/$fileName"
        val target = File(context.filesDir, relativePath)

        val input = context.contentResolver.openInputStream(source)
            ?: throw IOException("Tidak dapat membuka berkas sumber")
        input.use { inputStream ->
            target.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        relativePath
    }

    override fun hapus(path: String) {
        val f = file(path)
        if (f.exists()) {
            f.delete()
        }
    }

    override fun file(path: String): File = File(context.filesDir, path)
}

