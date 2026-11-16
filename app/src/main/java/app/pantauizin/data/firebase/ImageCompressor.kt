package app.pantauizin.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object ImageCompressor {

    /**
     * Mengompres berkas gambar lokal menjadi string Base64 JPEG.
     * Ukuran maksimum dimensi dibatasi 1280px dan kualitas JPEG ~75%
     * sehingga ukuran string Base64 umumnya di bawah 300 KB (sangat aman di bawah limit 1 MiB Firestore).
     */
    suspend fun compressFileToBase64(file: File, maxDimension: Int = 1280, quality: Int = 75): String? =
        withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() == 0L) return@withContext null

            try {
                // Decode bounds first to calculate inSampleSize
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimension * 2 ||
                    options.outHeight / sampleSize > maxDimension * 2
                ) {
                    sampleSize *= 2
                }

                options.inJustDecodeBounds = false
                options.inSampleSize = sampleSize

                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext null

                // Resize bitmap precisely if still larger than maxDimension
                val width = bitmap.width
                val height = bitmap.height
                val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                    val ratio = width.toFloat() / height.toFloat()
                    val (targetWidth, targetHeight) = if (ratio > 1f) {
                        maxDimension to (maxDimension / ratio).toInt()
                    } else {
                        (maxDimension * ratio).toInt() to maxDimension
                    }
                    Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                        if (it != bitmap) bitmap.recycle()
                    }
                } else {
                    bitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                scaledBitmap.recycle()

                val bytes = outputStream.toByteArray()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Mengubah string Base64 menjadi berkas JPEG di penyimpanan internal lokal `bukti/`
     * agar komponen UI (seperti BuktiViewerDialog) dapat menampilkannya seperti berkas lokal biasa.
     */
    suspend fun saveBase64ToLocalFile(
        context: Context,
        base64String: String,
        customFileName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (base64String.isBlank()) return@withContext null
        try {
            val bytes = Base64.decode(base64String, Base64.NO_WRAP)
            val dir = File(context.filesDir, "bukti")
            if (!dir.exists()) dir.mkdirs()

            val fileName = customFileName ?: "${UUID.randomUUID()}.jpg"
            val relativePath = "bukti/$fileName"
            val targetFile = File(context.filesDir, relativePath)

            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            relativePath
        } catch (e: Exception) {
            null
        }
    }
}
