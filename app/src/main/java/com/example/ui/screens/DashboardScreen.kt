package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.Debt
import com.example.ui.components.BankAccountItem
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToDebts: () -> Unit
) {
    val bankAccounts by viewModel.allBankAccounts.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val baseCurr by viewModel.baseCurrency.collectAsState()
    val rates by viewModel.exchangeRates.collectAsState()

    var showSyncDialog by remember { mutableStateOf(false) }
    var selectedAccountToSync by remember { mutableStateOf<BankAccount?>(null) }
    val showAddAccountDialog = remember { mutableStateOf(false) }

    // Hitung total saldo yang dikonversikan ke mata uang dasar (base currency)
    val totalBalance = remember(bankAccounts, baseCurr, rates) {
        bankAccounts.sumOf { acc ->
            viewModel.convertValue(acc.balance, acc.currency, baseCurr)
        }
    }

    // Ekstrak pengingat pelunasan hutang (yang statusnya BELUM_LUNAS dan mendekati jatuh tempo)
    val outstandingDebts = remember(debts) {
        debts.filter { it.status == "BELUM_LUNAS" }
            .sortedBy { it.dueDate }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Total Balance Card Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Total Saldo Gabungan (Mata Uang Acuan)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalBalance, baseCurr),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Base Currency Selector pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ganti Acuan:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        listOf("IDR", "USD", "EUR", "SGD").forEach { curr ->
                            FilterChip(
                                selected = (baseCurr == curr),
                                onClick = { viewModel.baseCurrency.value = curr },
                                label = { Text(curr, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Pengingat Pelunasan Hutang (Urgent Alerts)
        if (outstandingDebts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "Pengingat Pelunasan Hutang/Piutang",
                    subtitle = "Selesaikan pembayaran sesuai tenggat demi reputasi finansial"
                )
            }

            items(outstandingDebts.take(2)) { debt ->
                val remainingDays = remember(debt.dueDate) {
                    val diff = debt.dueDate - System.currentTimeMillis()
                    (diff / 86400000L).toInt()
                }

                val alertColor = if (remainingDays < 3) MaterialTheme.colorScheme.errorContainer 
                                 else MaterialTheme.colorScheme.tertiaryContainer

                val typeLabel = if (debt.type == "PIUTANG") "Mereka meminjam dari kita" else "Kita berhutang ke mereka"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onNavigateToDebts() },
                    colors = CardDefaults.cardColors(containerColor = alertColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (debt.type == "PIUTANG") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = "Hutang Piutang",
                                    tint = if (remainingDays < 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = debt.contactName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (remainingDays < 0) "Terlambat!" else "$remainingDays hari lagi",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remainingDays < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = "$typeLabel (${debt.description})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Jatuh Tempo: ${formatShortDate(debt.dueDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Sisa: ${formatCurrency(debt.remainingAmount, debt.currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // 3. Rekening & Dompet Bank
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "Rekening & Dompet",
                subtitle = "Digunakan untuk sinkronisasi pengeluaran otomatis",
                action = {
                    IconButton(onClick = { showAddAccountDialog.value = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Bank Account")
                    }
                }
            )
        }

        if (bankAccounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada rekening, gunakan tombol + untuk menambah.", textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(bankAccounts) { acc ->
                BankAccountItem(
                    account = acc,
                    onSyncClick = {
                        selectedAccountToSync = acc
                        showSyncDialog = true
                    }
                )
            }
        }
    }

    // --- DIALOGS ---

    // 1. Sinkronisasi Mutasi Bank via AI
    if (showSyncDialog && selectedAccountToSync != null) {
        var syncInputText by remember { mutableStateOf("") }
        var resultText by remember { mutableStateOf<String?>(null) }
        var isResultSuccess by remember { mutableStateOf(false) }

        // Template examples
        val templates = listOf(
            "M-Banking: BERHASIL Transfer Rp 145000 ke GO-PAY untuk Makanan Makan Siang Bakso" to "M-Banking: BERHASIL Transfer sebesar Rp 145.000 ke GO-PAY untuk pembayaran Makan Siang.",
            "Mandiri SMS Kredit" to "Mandiri SMS: Kredit masuk dari PT MANDIRI SEJAHTERA sebesar Rp 4.500.000 saldo akhir Rp 16.500.000.",
            "BCA Tarik Tunai USD" to "BCA: Anda melakukan transaksi luar negeri sebesar USD 60 untuk Belanja Souvenir bandara.",
            "Receivable Lend" to "Telah dipinjamkan uang tunai cash sejumlah Rp 500.000 kepada Andi untuk kebutuhan darurat."
        )

        AlertDialog(
            onDismissRequest = {
                showSyncDialog = false
                selectedAccountToSync = null
                resultText = null
            },
            title = {
                Text(
                    text = "AI Bank Sync (${selectedAccountToSync?.name})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Fitur ini mensimulasikan pembacaan SMS mutasi atau teks notifikasi perbankan Indonesia menggunakan Gemini AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Gunakan template pengujian:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        templates.forEach { (name, content) ->
                            SuggestionChip(
                                onClick = { syncInputText = content },
                                label = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = syncInputText,
                        onValueChange = { syncInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("Rekatkan SMS mutasi, resi transfer, atau slip pembayaran di sini...", fontSize = 12.sp) },
                        label = { Text("Teks Notifikasi Bank") },
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    // Display sync progress / result
                    val isLoadingLocal by viewModel.isLoading.collectAsState()
                    if (isLoadingLocal) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    if (resultText != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isResultSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = resultText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = if (isResultSuccess) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (resultText == null) {
                    Button(
                        onClick = {
                            if (syncInputText.isNotBlank()) {
                                viewModel.parseBankStatementText(
                                    text = syncInputText,
                                    bankAccountId = selectedAccountToSync?.id
                                ) { success, msg ->
                                    isResultSuccess = success
                                    resultText = msg
                                }
                            }
                        },
                        enabled = syncInputText.isNotBlank()
                    ) {
                        Text("Mulai Analisis AI")
                    }
                } else {
                    Button(
                        onClick = {
                            showSyncDialog = false
                            selectedAccountToSync = null
                            resultText = null
                        }
                    ) {
                        Text("Selesai")
                    }
                }
            },
            dismissButton = {
                if (resultText == null) {
                    TextButton(onClick = { showSyncDialog = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    // 2. Add Bank Account Dialog
    if (showAddAccountDialog.value) {
        var accName by remember { mutableStateOf("") }
        var accNum by remember { mutableStateOf("") }
        var accHolder by remember { mutableStateOf("") }
        var optCurrency by remember { mutableStateOf("IDR") }
        var balanceStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog.value = false },
            title = { Text("Tambahkan Rekening / Dompet Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Rekening (Contoh: BNI Prioritas, Cash)") }
                    )
                    OutlinedTextField(
                        value = accNum,
                        onValueChange = { accNum = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("No Rekening (Contoh: 124-99-23)") }
                    )
                    OutlinedTextField(
                        value = accHolder,
                        onValueChange = { accHolder = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Pemilik") }
                    )
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Saldo Awal") }
                    )

                    Text("Mata Uang:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IDR", "USD", "EUR", "SGD").forEach { c ->
                            FilterChip(
                                selected = optCurrency == c,
                                onClick = { optCurrency = c },
                                label = { Text(c) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val balDouble = balanceStr.toDoubleOrNull() ?: 0.0
                        viewModel.addBankAccount(accName, accNum, accHolder, balDouble, optCurrency)
                        showAddAccountDialog.value = false
                    },
                    enabled = accName.isNotBlank() && balanceStr.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog.value = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
