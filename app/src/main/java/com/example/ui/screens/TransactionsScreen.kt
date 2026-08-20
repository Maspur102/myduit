package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.Transaction
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val bankAccounts by viewModel.allBankAccounts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "PEMASUKAN", "PENGELUARAN"
    val showAddDialog = remember { mutableStateOf(false) }

    // Memfilter transaksi
    val filteredTransactions = remember(transactions, searchQuery, selectedTypeFilter) {
        transactions.filter { t ->
            val matchQuery = t.description.contains(searchQuery, ignoreCase = true) ||
                    t.category.contains(searchQuery, ignoreCase = true)
            val matchType = when (selectedTypeFilter) {
                "PEMASUKAN" -> t.type == "PEMASUKAN" || t.type == "HUTANG_MASUK"
                "PENGELUARAN" -> t.type == "PENGELUARAN" || t.type == "PINJAMAN_DILUAR"
                else -> true
            }
            matchQuery && matchType
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog.value = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Pencatatan Baru") },
                text = { Text("Catat Manual") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Search & Filter
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari deskripsi, kategori...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SEMUA" to "Semua", "PEMASUKAN" to "Pemasukan", "PENGELUARAN" to "Pengeluaran").forEach { (value, label) ->
                            FilterChip(
                                selected = selectedTypeFilter == value,
                                onClick = { selectedTypeFilter = value },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Ledger List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Tidak Ada Histori Transaksi",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Mulailah membuat pencatatan transaksi manual atau gunakan fitur sinkronisasi bank otomatis via AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredTransactions) { trans ->
                        val isIncomeText = trans.type in listOf("PEMASUKAN", "HUTANG_MASUK")
                        val amountColor = if (isIncomeText) Color(0xFF2E7D32) else Color(0xFFC62828)
                        val iconBackground = if (isIncomeText) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val iconColor = if (isIncomeText) Color(0xFF2E7D32) else Color(0xFFC62828)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icons Bubble
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(iconBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (trans.category) {
                                        "Makanan" -> Icons.Default.Restaurant
                                        "Transportasi" -> Icons.Default.DirectionsCar
                                        "Belanja" -> Icons.Default.ShoppingBag
                                        "Kesehatan" -> Icons.Default.MedicalServices
                                        "Pendidikan" -> Icons.Default.School
                                        "Gaji" -> Icons.Default.MonetizationOn
                                        "Hutang-Piutang" -> Icons.Default.Handshake
                                        else -> Icons.Default.Payments
                                    }
                                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trans.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${trans.category} • ${formatShortDate(trans.timestamp)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (isIncomeText) "+" else "-") + formatCurrency(trans.amount, trans.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = amountColor
                                )
                                // Associated account name
                                val bankAcc = bankAccounts.find { it.id == trans.bankAccountId }
                                if (bankAcc != null) {
                                    Text(
                                        text = bankAcc.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // Modal dialog untuk manual add
    if (showAddDialog.value) {
        var transType by remember { mutableStateOf("PENGELUARAN") }
        var transAmountStr by remember { mutableStateOf("") }
        var transCurrency by remember { mutableStateOf("IDR") }
        var transCategory by remember { mutableStateOf("Makanan") }
        var transDesc by remember { mutableStateOf("") }
        var selectedBankId by remember { mutableStateOf<Int?>(null) }

        val categoriesList = listOf("Makanan", "Transportasi", "Belanja", "Kesehatan", "Pendidikan", "Gaji", "Hutang-Piutang", "Lainnya")

        AlertDialog(
            onDismissRequest = { showAddDialog.value = false },
            title = { Text("Registrasi Transaksi Baru (Manual)", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("Tipe Arus Dana:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("PENGELUARAN" to "Pengeluaran", "PEMASUKAN" to "Pemasukan").forEach { (tp, lbl) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { transType = tp }
                                ) {
                                    RadioButton(selected = transType == tp, onClick = { transType = tp })
                                    Text(lbl, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = transAmountStr,
                            onValueChange = { transAmountStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nominal Transaksi") }
                        )
                    }

                    item {
                        Text("Mata Uang:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("IDR", "USD", "EUR", "SGD").forEach { c ->
                                FilterChip(
                                    selected = transCurrency == c,
                                    onClick = { transCurrency = c },
                                    label = { Text(c) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Kategori:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Simple category slider or list
                            Column {
                                val chunked = categoriesList.chunked(4)
                                chunked.forEach { chunk ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        chunk.forEach { cat ->
                                            InputChip(
                                                selected = transCategory == cat,
                                                onClick = { transCategory = cat },
                                                label = { Text(cat, fontSize = 10.sp) },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = transDesc,
                            onValueChange = { transDesc = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Deskripsi / Nama Aktivitas") }
                        )
                    }

                    item {
                        Text("Simpan ke Dompet / Rekening Bank:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        if (bankAccounts.isEmpty()) {
                            Text("Tidak ada rekening bank.", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bankAccounts.forEach { acc ->
                                    FilterChip(
                                        selected = selectedBankId == acc.id,
                                        onClick = { selectedBankId = if (selectedBankId == acc.id) null else acc.id },
                                        label = { Text("${acc.name} (${acc.currency})", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountDouble = transAmountStr.toDoubleOrNull() ?: 0.0
                        viewModel.addManualTransaction(
                            type = transType,
                            amount = amountDouble,
                            currency = transCurrency,
                            category = transCategory,
                            description = transDesc,
                            bankAccountId = selectedBankId,
                            date = System.currentTimeMillis()
                        )
                        showAddDialog.value = false
                    },
                    enabled = transAmountStr.isNotBlank() && transDesc.isNotBlank()
                ) {
                    Text("Catat Transaksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog.value = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
