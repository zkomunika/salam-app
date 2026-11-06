package app.pantauizin.ui.pengaturan

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.TemplateVariant
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(
    viewModel: PengaturanViewModel,
    onOpenDataSantri: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showTimePicker by remember { mutableStateOf(false) }
    var draftTemplateA by remember { mutableStateOf(state.templateA) }
    var draftTemplateB by remember { mutableStateOf(state.templateB) }

    LaunchedEffect(state.templateA) {
        draftTemplateA = state.templateA
    }
    LaunchedEffect(state.templateB) {
        draftTemplateB = state.templateB
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Jam pengingat harian
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jam Pengingat Harian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sapuan otomatis dijalankan setiap hari pada pukul ${state.jamPengingat.format(TIME_FORMAT)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text("Ubah Jam (${state.jamPengingat.format(TIME_FORMAT)})")
                }
            }
        }

        // 2. Data santri
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Master Santri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kelola daftar santri, pindah kamar, atau nonaktifkan santri yang sudah lulus/boyong.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onOpenDataSantri) {
                    Text("Kelola Data Santri")
                }
            }
        }

        // 3. Template pesan A dan B
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Template Pesan Wali",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Penampung: {nama}, {kamar}, {batas_awal}, {batas_aktif}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Varian A (Belum Dikonfirmasi)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = draftTemplateA,
                    onValueChange = { draftTemplateA = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                state.pesanErrorTemplateA?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (viewModel.simpanTemplate(TemplateVariant.A, draftTemplateA)) {
                                    Toast.makeText(context, "Template A disimpan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Simpan Varian A")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.kembalikanTemplate(TemplateVariant.A)
                            }
                        }
                    ) {
                        Text("Kembalikan bawaan")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Varian B (Sudah Pernah Dikonfirmasi)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = draftTemplateB,
                    onValueChange = { draftTemplateB = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                state.pesanErrorTemplateB?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (viewModel.simpanTemplate(TemplateVariant.B, draftTemplateB)) {
                                    Toast.makeText(context, "Template B disimpan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Simpan Varian B")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.kembalikanTemplate(TemplateVariant.B)
                            }
                        }
                    ) {
                        Text("Kembalikan bawaan")
                    }
                }
            }
        }

        // 4. Petunjuk baterai Vivo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryAlert, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Petunjuk Baterai & Latar Belakang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "HP Vivo biasa menghentikan aplikasi di latar belakang. Agar pengingat harian selalu muncul tepat waktu, aktifkan izin \"mulai otomatis\" (Autostart) dan pilih \"tanpa pembatasan baterai\" (Konsumsi daya latar belakang tinggi) secara manual untuk aplikasi ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Abaikan bila intent tidak didukung perangkat
                        }
                    }
                ) {
                    Text("Buka pengaturan baterai")
                }
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.jamPengingat.hour,
            initialMinute = state.jamPengingat.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Jam Pengingat (24 Jam)") },
            text = {
                TimePicker(state = pickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                        scope.launch {
                            viewModel.ubahJam(newTime)
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

