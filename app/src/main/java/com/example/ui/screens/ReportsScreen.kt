package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.InteractiveTrendChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrency
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val baseCurr by viewModel.baseCurrency.collectAsState()
    val rates by viewModel.exchangeRates.collectAsState()

    // 1. Hitung total pengeluaran kumulatif bulan ini
    val totalExpenseThisMonth = remember(transactions, baseCurr, rates) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        transactions.filter { t ->
            val isExpense = t.type == "PENGELUARAN" || t.type == "PINJAMAN_DILUAR"
            if (!isExpense) return@filter false

            val transCal = Calendar.getInstance().apply { timeInMillis = t.timestamp }
            transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
        }.sumOf { t ->
            viewModel.convertValue(t.amount, t.currency, baseCurr)
        }
    }

    // 2. Agregasi pengeluaran per kategori (untuk diagram donat)
    val categoryShares = remember(transactions, baseCurr, rates) {
        transactions.filter { t -> t.type == "PENGELUARAN" || t.type == "PINJAMAN_DILUAR" }
            .groupBy { it.category }
            .mapValues { (_, list) ->
                list.sumOf { t -> viewModel.convertValue(t.amount, t.currency, "IDR") } // base IDR for chart
            }
            .toList()
            .sortedByDescending { it.second }
    }

    // 3. Agregasi tren belanja bulanan interaktif (mengelompokkan pengeluaran 5 bulan terakhir)
    val monthlyTrendData = remember(transactions, baseCurr, rates) {
        val sdf = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
        val cal = Calendar.getInstance()
        
        // Buat list 5 bulan terakhir
        val last5Months = (0..4).map { i ->
            val clone = cal.clone() as Calendar
            clone.add(Calendar.MONTH, -i)
            sdf.format(clone.time)
        }.reversed()

        // Kelompokkan pengeluaran riil
        val grouped = transactions.filter { t -> t.type == "PENGELUARAN" || t.type == "PINJAMAN_DILUAR" }
            .groupBy { t ->
                val transCal = Calendar.getInstance().apply { timeInMillis = t.timestamp }
                sdf.format(transCal.time)
            }
            .mapValues { (_, list) ->
                list.sumOf { t -> viewModel.convertValue(t.amount, t.currency, "IDR") } // IDR base
            }

        // Gabungkan untuk menjamin semua 5 bulan terisi angka (0.0 jika tidak ada)
        last5Months.map { month ->
            // Ambil nama pendek bulan saja misal "Mei", "Jun"
            val shortMonth = if (month.length > 3) month.take(3) else month
            val total = grouped[month] ?: 0.0
            shortMonth to total
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Core KPI Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Total Pengeluaran Bulan Ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = formatCurrency(totalExpenseThisMonth, baseCurr),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Konversi otomatis real-time dari multi-valas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Section Chart 1: Interactive trend
        item {
            InteractiveTrendChart(
                monthlySpendingData = monthlyTrendData,
                currencySymbol = "Rp"
            )
        }

        // Section Chart 2: Category Pie Donut Chart
        if (categoryShares.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada pengeluaran yang tercatat untuk dianalisis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                SectionHeader(
                    title = "Distribusi Pengeluaran",
                    subtitle = "Laporan porsi belanja otomatis kumulatif Anda"
                )
                CategoryDonutChart(shares = categoryShares)
            }
        }
    }
}
