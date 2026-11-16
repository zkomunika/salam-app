package app.pantauizin.ui.pengaturan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.SantriItem
import app.pantauizin.ui.components.EmptyStateBox
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DataSantriScreen(
    viewModel: DataSantriViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingSantri by remember { mutableStateOf<SantriItem?>(null) }
    var formNama by remember { mutableStateOf("") }
    var formKamar by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Data Santri") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSantri = null
                    formNama = ""
                    formKamar = ""
                    viewModel.hapusPesanError()
                    showFormDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah santri")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.ubahQuery(it) },
                label = { Text("Cari nama atau kamar santri") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.ubahQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            val totalSemua = state.totalAktif + state.totalNonaktif
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    SantriStatusFilter.SEMUA to "Semua ($totalSemua)",
                    SantriStatusFilter.AKTIF to "Aktif (${state.totalAktif})",
                    SantriStatusFilter.NONAKTIF to "Nonaktif (${state.totalNonaktif})"
                )
                for ((f, label) in filters) {
                    FilterChip(
                        selected = state.statusFilter == f,
                        onClick = { viewModel.pilihStatusFilter(f) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.items.isEmpty()) {
                val hasActiveFilter = state.query.isNotBlank() || state.statusFilter != SantriStatusFilter.SEMUA
                EmptyStateBox(
                    icon = Icons.Default.People,
                    title = if (hasActiveFilter) "Tidak ada santri yang cocok" else "Belum ada data santri",
                    subtitle = if (hasActiveFilter) {
                        "Coba ubah kata kunci pencarian atau pilih filter lain."
                    } else {
                        "Ketuk tombol + di kanan bawah untuk menambah data santri."
                    },
                    actionLabel = if (hasActiveFilter) "Reset Filter" else null,
                    onAction = if (hasActiveFilter) {
                        {
                            viewModel.ubahQuery("")
                            viewModel.pilihStatusFilter(SantriStatusFilter.SEMUA)
                        }
                    } else {
                        null
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.items, key = { it.id }) { santri ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = santri.nama,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!santri.aktif) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Nonaktif",
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Kamar ${santri.kamar}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingSantri = santri
                                            formNama = santri.nama
                                            formKamar = santri.kamar
                                            viewModel.hapusPesanError()
                                            showFormDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Ubah")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.setAktif(santri.id, !santri.aktif)
                                            }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(if (santri.aktif) "Nonaktifkan" else "Aktifkan")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        val isEdit = editingSantri != null
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = { Text(if (isEdit) "Ubah Data Santri" else "Tambah Santri Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = formNama,
                        onValueChange = {
                            formNama = it
                            viewModel.hapusPesanError()
                        },
                        label = { Text("Nama Santri") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formKamar,
                        onValueChange = {
                            formKamar = it
                            viewModel.hapusPesanError()
                        },
                        label = { Text("Kamar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val target = editingSantri
                            val ok = if (target == null) {
                                viewModel.tambah(formNama, formKamar)
                            } else {
                                viewModel.ubah(target.id, formNama, formKamar)
                            }
                            if (ok) {
                                showFormDialog = false
                            }
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

