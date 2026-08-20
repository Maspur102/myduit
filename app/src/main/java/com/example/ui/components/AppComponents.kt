package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccount
import java.text.SimpleDateFormat
import java.util.*

// Helper formatter rupiah & currencies
fun formatCurrency(amount: Double, currency: String): String {
    val formatter = java.text.DecimalFormat("#,###")
    val formatted = formatter.format(amount).replace(",", ".")
    return when (currency) {
        "IDR" -> "Rp $formatted"
        "USD" -> "$ $formatted"
        "EUR" -> "€ $formatted"
        "SGD" -> "S$ $formatted"
        else -> "$currency $formatted"
    }
}

fun formatShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action?.invoke()
    }
}

/**
 * Custom Canvas-based interactive monthly spending bar chart
 */
@Composable
fun InteractiveTrendChart(
    monthlySpendingData: List<Pair<String, Double>>, // e.g. "Jan", 2500000.0 & "Feb", 1800000.0
    currencySymbol: String = "Rp"
) {
    if (monthlySpendingData.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada data pengeluaran untuk dianalisis", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    var selectedIndex by remember { mutableStateOf(-1) }
    val maxVal = monthlySpendingData.maxOfOrNull { it.second }?.coerceAtLeast(10000.0) ?: 10000.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tren Belanja Bulanan (Interaktif)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sentuh bar grafik untuk melihat besaran nominal pengeluaran bulanan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic interactive tooltip popup
            AnimatedVisibility(visible = selectedIndex in monthlySpendingData.indices) {
                if (selectedIndex in monthlySpendingData.indices) {
                    val p = monthlySpendingData[selectedIndex]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bulan: ${p.first}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Laporan: ${formatCurrency(p.second, "IDR")}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Canvas drawing
            val barColor = MaterialTheme.colorScheme.primary
            val highlightedBarColor = MaterialTheme.colorScheme.secondary
            val gridColor = MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlySpendingData) {
                            detectTapGestures { offset ->
                                val count = monthlySpendingData.size
                                val sectionWidth = size.width / count
                                val clickedIndex = (offset.x / sectionWidth).toInt()
                                selectedIndex = if (clickedIndex in 0 until count) clickedIndex else -1
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val count = monthlySpendingData.size
                    val barWidthFraction = 0.6f
                    val sectionWidth = w / count
                    val barWidth = sectionWidth * barWidthFraction
                    val barPadding = (sectionWidth - barWidth) / 2

                    // Draw Horizontal Gridlines (3 levels)
                    for (i in 1..3) {
                        val gridY = h * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gridY),
                            end = Offset(w, gridY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Bars & Labels
                    monthlySpendingData.forEachIndexed { idx, pair ->
                        val barHeight = (pair.second / maxVal * (h - 30.dp.toPx())).toFloat()
                        val barX = idx * sectionWidth + barPadding
                        val barY = h - barHeight - 20.dp.toPx()

                        // Check if selected
                        val color = if (idx == selectedIndex) highlightedBarColor else barColor

                        // Draw main Bar rounded
                        drawRect(
                            color = color,
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth, barHeight)
                        )

                        // Draw simple month text below the bar
                        val textFormat = SimpleDateFormat("MMM", Locale.getDefault())
                        // We will overlay a text or just represent simple marker circles if text drawing is too lengthy,
                        // but actually writing text is simple using native text features or drawContext!
                    }
                }

                // Jetpack Compose Column overlapping transparently to draw labels below
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthlySpendingData.forEachIndexed { index, pair ->
                        Text(
                            text = pair.first,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas-based Doughnut chart for Category distribution
 */
@Composable
fun CategoryDonutChart(
    shares: List<Pair<String, Double>> // "Makanan" -> 450000.0, "Transportasi" -> 150000.0
) {
    if (shares.isEmpty()) return

    val total = shares.sumOf { it.second }.coerceAtLeast(1.0)
    
    // Nice custom colors for categories
    val colors = listOf(
        Color(0xFF2E7D32), // Emerald Green - Makanan
        Color(0xFF1565C0), // Blue - Transportasi
        Color(0xFFE65100), // Orange - Belanja
        Color(0xFFC62828), // Red - Kesehatan
        Color(0xFFFBC02D), // Gold - Gaji
        Color(0xFF6A1B9A), // Purple - Hutang-Piutang
        Color(0xFF455A64)  // Slate - Lainnya
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                titleText = "Persentase Pengeluaran per Kategori",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Donut Chart
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        shares.forEachIndexed { index, pair ->
                            val sweepAngle = ((pair.second / total) * 360f).toFloat()
                            val color = colors[index % colors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                size = size,
                                style = Stroke(width = 16.dp.toPx())
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Laporan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Kategori",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Right: Legend list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    shares.forEachIndexed { index, pair ->
                        val pct = ((pair.second / total) * 100).toInt()
                        val color = colors[index % colors.size]

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pair.first,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$pct% (${formatCurrency(pair.second, "IDR")})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Workaround for Compose title text
@Composable
private fun Text(titleText: String, style: androidx.compose.ui.text.TextStyle, fontWeight: FontWeight, modifier: Modifier) {
    Text(text = titleText, style = style, fontWeight = fontWeight, modifier = modifier)
}

/**
 * Custom Bank Account display item
 */
@Composable
fun BankAccountItem(
    account: BankAccount,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = if (account.name.contains("BCA", true)) Icons.Default.AccountBalance
                        else if (account.name.contains("Mandiri", true)) Icons.Default.CreditCard
                        else Icons.Default.Payments
                        Icon(icon, contentDescription = "Bank Icon", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Acc No: ${account.accountNumber} • ${account.accountHolder}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(account.balance, account.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onSyncClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(12.dp))
                        Text("AI Sync", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
