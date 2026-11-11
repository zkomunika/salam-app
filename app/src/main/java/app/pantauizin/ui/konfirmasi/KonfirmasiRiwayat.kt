package app.pantauizin.ui.konfirmasi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.pantauizin.data.KonfirmasiView
import app.pantauizin.domain.BuktiJenis
import app.pantauizin.domain.BuktiType
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))
private val ID_DATETIME_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

/** Daftar riwayat konfirmasi wali lengkap dengan tombol lihat bukti. */
@Composable
fun KonfirmasiRiwayatSection(
    konfirmasi: List<KonfirmasiView>,
    modifier: Modifier = Modifier
) {
    var buktiTerbuka by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        konfirmasi.forEachIndexed { idx, konf ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = "Konfirmasi ${idx + 1}  •  dicatat ${
                        konf.dibuatPada.atZone(ZoneId.systemDefault()).format(ID_DATETIME_FORMAT)
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Kembali paling lambat: " +
                        (konf.tanggalKembaliBaru?.format(ID_DATE_FORMAT) ?: "tanpa tanggal baru"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!konf.catatan.isNullOrBlank()) {
                    Text(
                        text = "Keterangan: ${konf.catatan}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val path = konf.buktiPath
                if (path != null) {
                    OutlinedButton(
                        onClick = { buktiTerbuka = path },
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lihat bukti")
                    }
                } else {
                    Text(
                        text = "Tanpa bukti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    buktiTerbuka?.let { path ->
        BuktiViewerDialog(path = path, onDismiss = { buktiTerbuka = null })
    }
}

/** Dialog riwayat konfirmasi (dipakai dari tab Izin). */
@Composable
fun KonfirmasiRiwayatDialog(
    namaSantri: String,
    konfirmasi: List<KonfirmasiView>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi wali — $namaSantri") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                KonfirmasiRiwayatSection(konfirmasi = konfirmasi)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

private sealed interface BuktiState {
    data object Memuat : BuktiState
    data object TidakAda : BuktiState
    data class Gambar(val bitmap: Bitmap) : BuktiState
    data object Selesai : BuktiState
}

@Composable
fun BuktiViewerDialog(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var state by remember(path) { mutableStateOf<BuktiState>(BuktiState.Memuat) }

    LaunchedEffect(path) {
        val file = File(context.filesDir, path)
        if (!file.exists()) {
            state = BuktiState.TidakAda
            return@LaunchedEffect
        }
        val header = runCatching {
            file.inputStream().use { s -> ByteArray(12).also { s.read(it) } }
        }.getOrNull()
        when (BuktiType.detect(path, header)) {
            BuktiJenis.GAMBAR -> {
                val bmp = decodeSampled(file, 1600)
                state = if (bmp != null) BuktiState.Gambar(bmp) else BuktiState.TidakAda
            }
            BuktiJenis.PDF -> {
                bukaDenganAplikasiLain(context, file, "application/pdf")
                state = BuktiState.Selesai
                onDismiss()
            }
            BuktiJenis.LAINNYA -> {
                bukaDenganAplikasiLain(context, file, "*/*")
                state = BuktiState.Selesai
                onDismiss()
            }
        }
    }

    val current = state
    if (current is BuktiState.Selesai) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bukti konfirmasi") },
        text = {
            when (current) {
                is BuktiState.Gambar -> ZoomableImage(current.bitmap)
                BuktiState.TidakAda -> Text("File bukti tidak ditemukan atau tidak bisa dibuka.")
                else -> Text("Memuat…")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun ZoomableImage(bitmap: Bitmap) {
    var scale by remember { mutableFloatStateOf(1f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Bukti konfirmasi",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                    }
                }
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
    }
}

private fun decodeSampled(file: File, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}

private fun bukaDenganAplikasiLain(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_LONG).show()
    }
}

