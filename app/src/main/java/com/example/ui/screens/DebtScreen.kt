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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import com.example.data.model.Debt
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatShortDate
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel: FinanceViewModel
) {
    val debts by viewModel.allDebts.collectAsState()
    val bankAccounts by viewModel.allBankAccounts.collectAsState()

    var selectedTab by remember { mutableStateOf("PIUTANG") } // "PIUTANG" (dipinjamkan) vs "HUTANG" (kita pinjam)
    val showAddDebtDialog = remember { mutableStateOf(false) }

    var selectedDebtForRepayment by remember { mutableStateOf<Debt?>(null) }
    var showRepayDialog by remember { mutableStateOf(false) }

    // Memfilter data sesuai tab aktif
    val filteredDebts = remember(debts, selectedTab) {
        debts.filter { it.type == selectedTab }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDebtDialog.value = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Pemberian Pinjaman Baru") },
                text = { Text(if (selectedTab == "PIUTANG") "Pinjamkan Uang" else "Pinjam Uang") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Header
            TabRow(
                selectedTabIndex = if (selectedTab == "PIUTANG") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == "PIUTANG",
                    onClick = { selectedTab = "PIUTANG" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, size = 16.dp, color = Color(0xFF2E7D32))
                            Text("Piutang (Dipinjamkan)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == "HUTANG",
                    onClick = { selectedTab = "HUTANG" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CallReceived, contentDescription = null, size = 16.dp, color = Color(0xFFC62828))
                            Text("Hutang (Dipinjam)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (filteredDebts.isEmpty()) {
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
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (selectedTab == "PIUTANG") "Tidak Ada Piutang Aktif" else "Tidak Ada Hutang Aktif",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (selectedTab == "PIUTANG") {
                                "Catat uang keluar yang Anda pinjamkan ke kerabat atau pihak lain agar terekam jatuh temponya."
                            } else {
                                "Catat pinjaman masuk dari pihak lain lengkap dengan pengingat pelunasan tepat waktu."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDebts) { debt ->
                        val pctPaid = ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat()
                        val isOverdue = debt.dueDate < System.currentTimeMillis() && debt.status != "LUNAS"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (debt.status == "LUNAS") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = debt.contactName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = debt.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Status Badge
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = if (debt.status == "LUNAS") "LUNAS" else if (isOverdue) "TELAT TEMPO" else "BELUM LUNAS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = if (debt.status == "LUNAS") Color(0xFF2E7D32) else if (isOverdue) Color(0xFFC62828) else Color(0xFFE65100),
                                            containerColor = if (debt.status == "LUNAS") Color(0xFFE8F5E9) else if (isOverdue) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Progress Bar pelunasan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Progress Pelunasan:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(pctPaid * 100).toInt()}% Selesai",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = pctPaid,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = if (debt.status == "LUNAS") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Repayment balances info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Jatuh Tempo: ${formatShortDate(debt.dueDate)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOverdue) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "Total Pokok: ${formatCurrency(debt.totalAmount, debt.currency)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Sisa Pelunasan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatCurrency(debt.remainingAmount, debt.currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (debt.status == "LUNAS") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                // Repay/Install buttons
                                if (debt.status != "LUNAS") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            selectedDebtForRepayment = debt
                                            showRepayDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("Bayar Cicilan Kredit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Add Debt / Receivable Registry
    if (showAddDebtDialog.value) {
        var contactName by remember { mutableStateOf("") }
        var debtDesc by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf("IDR") }
        var selectedBankId by remember { mutableStateOf<Int?>(null) }
        var dueDateOffsetWeeks by remember { mutableStateOf(4) } // default 4 weeks

        AlertDialog(
            onDismissRequest = { showAddDebtDialog.value = false },
            title = {
                Text(
                    text = if (selectedTab == "PIUTANG") "Pencatatan Piutang Baru (Dipinjamkan)" 
                           else "Pencatatan Hutang Baru (Dipinjam)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nama Rekan / Pihak Lain") }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nominal Pinjaman") }
                        )
                    }

                    item {
                        Text("Mata Uang:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("IDR", "USD", "EUR", "SGD").forEach { c ->
                                FilterChip(
                                    selected = currency == c,
                                    onClick = { currency = c },
                                    label = { Text(c) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = debtDesc,
                            onValueChange = { debtDesc = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tujuan / Penjelasan Pendek") }
                        )
                    }

                    item {
                        Text("Tenggat Waktu Pelunasan:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "1 Mgg", 2 to "2 Mgg", 4 to "1 Bln", 12 to "3 Bln").forEach { (weeks, labels) ->
                                FilterChip(
                                    selected = dueDateOffsetWeeks == weeks,
                                    onClick = { dueDateOffsetWeeks = weeks },
                                    label = { Text(labels, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (selectedTab == "PIUTANG") "Potong dari Rekening/Dompet:" 
                                   else "Tambahkan ke Rekening/Dompet:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
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
                        val amtDouble = amountStr.toDoubleOrNull() ?: 0.0
                        // Hitung tenggat waktu ms
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.WEEK_OF_YEAR, dueDateOffsetWeeks)
                        val calculatedDueDateMs = cal.timeInMillis

                        viewModel.addManualDebt(
                            contactName = contactName,
                            type = selectedTab, // "PIUTANG" atau "HUTANG"
                            amount = amtDouble,
                            currency = currency,
                            description = debtDesc,
                            dueDate = calculatedDueDateMs,
                            bankAccountId = selectedBankId
                        )
                        showAddDebtDialog.value = false
                    },
                    enabled = contactName.isNotBlank() && amountStr.isNotBlank() && debtDesc.isNotBlank()
                ) {
                    Text("Catat ke Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog.value = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog 2: Installment Repayment Dialog
    if (showRepayDialog && selectedDebtForRepayment != null) {
        val activeDebt = selectedDebtForRepayment!!
        var repayAmountStr by remember { mutableStateOf("") }
        var repayNotes by remember { mutableStateOf("Cicilan Pelunasan") }
        var selectedRepayBankId by remember { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = {
                showRepayDialog = false
                selectedDebtForRepayment = null
            },
            title = { Text("Registrasi Pembayaran Cicilan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Kontak: ${activeDebt.contactName}\nTujuan: ${activeDebt.description}\nSisa yang harus dilunasi: ${formatCurrency(activeDebt.remainingAmount, activeDebt.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = repayAmountStr,
                        onValueChange = { repayAmountStr = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Jumlah Nominal Pembayaran (${activeDebt.currency})") }
                    )

                    OutlinedTextField(
                        value = repayNotes,
                        onValueChange = { repayNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Catatan / Keterangan Pembayaran") }
                    )

                    Text(
                        text = if (activeDebt.type == "PIUTANG") "Pemasukan dana cicilan akan menambah dompet:"
                               else "Pengambilan dana cicilan akan mengurangi dompet:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (bankAccounts.isEmpty()) {
                        Text("Tidak ada rekening bank.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            bankAccounts.forEach { acc ->
                                FilterChip(
                                    selected = selectedRepayBankId == acc.id,
                                    onClick = { selectedRepayBankId = if (selectedRepayBankId == acc.id) null else acc.id },
                                    label = { Text("${acc.name} (${acc.currency})", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rDouble = repayAmountStr.toDoubleOrNull() ?: 0.0
                        viewModel.payDebt(
                            debtId = activeDebt.id,
                            amount = rDouble,
                            bankAccountId = selectedRepayBankId,
                            notes = repayNotes
                        )
                        showRepayDialog = false
                        selectedDebtForRepayment = null
                    },
                    enabled = repayAmountStr.isNotBlank()
                ) {
                    Text("Proses Pembayaran")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRepayDialog = false
                        selectedDebtForRepayment = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
// Workaround helper to fix size parameter conflict
@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, color: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = color
    )
}
