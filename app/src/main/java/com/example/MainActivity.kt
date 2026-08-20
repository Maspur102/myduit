package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableStateOf("DASBOR") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Catatan Keuangan",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == "DASBOR",
                                onClick = { selectedTab = "DASBOR" },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == "DASBOR") Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Dasbor"
                                    )
                                },
                                label = { Text("Dasbor") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == "TRANSAKSI",
                                onClick = { selectedTab = "TRANSAKSI" },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == "TRANSAKSI") Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                        contentDescription = "Ledger"
                                    )
                                },
                                label = { Text("Transaksi") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == "LAPORAN",
                                onClick = { selectedTab = "LAPORAN" },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == "LAPORAN") Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                        contentDescription = "Laporan"
                                    )
                                },
                                label = { Text("Laporan") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == "DEBT_CREDIT",
                                onClick = { selectedTab = "DEBT_CREDIT" },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == "DEBT_CREDIT") Icons.Filled.Handshake else Icons.Outlined.Handshake,
                                        contentDescription = "Hutang-Piutang"
                                    )
                                },
                                label = { Text("Hutang") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { tabState ->
                            when (tabState) {
                                "DASBOR" -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToDebts = { selectedTab = "DEBT_CREDIT" }
                                )
                                "TRANSAKSI" -> TransactionsScreen(
                                    viewModel = viewModel
                                )
                                "LAPORAN" -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                "DEBT_CREDIT" -> DebtScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
