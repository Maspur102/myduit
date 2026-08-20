package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiParser
import com.example.data.local.FinanceDatabase
import com.example.data.model.BankAccount
import com.example.data.model.Debt
import com.example.data.model.Transaction
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    val isLoading = MutableStateFlow(false)

    // Cohesive flows of data
    val allTransactions: StateFlow<List<Transaction>>
    val allBankAccounts: StateFlow<List<BankAccount>>
    val allDebts: StateFlow<List<Debt>>

    // Multi-mata uang: Nilai tukar dinamis (Conversion rates relative to IDR)
    private val _exchangeRates = MutableStateFlow(
        mapOf(
            "IDR" to 1.0,
            "USD" to 16250.0,
            "EUR" to 17600.0,
            "SGD" to 12100.0
        )
    )
    val exchangeRates = _exchangeRates.asStateFlow()

    // Base display currency (default: IDR)
    val baseCurrency = MutableStateFlow("IDR")

    init {
        val database = FinanceDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())

        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allBankAccounts = repository.allBankAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allDebts = repository.allDebts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial data if database is empty
        viewModelScope.launch {
            allBankAccounts.collectLatest { list ->
                if (list.isEmpty()) {
                    seedData()
                }
            }
        }
    }

    // --- Database Seeding ---
    private suspend fun seedData() {
        isLoading.value = true

        // 1. Simulasikan 3 Rekening Bank Utama
        val bcaId = repository.insertBankAccount(
            BankAccount(name = "BCA Saku", accountNumber = "8023-4412-11", accountHolder = "Budi Santoso", balance = 8450000.0, currency = "IDR")
        ).toInt()
        val mandiriId = repository.insertBankAccount(
            BankAccount(name = "Bank Mandiri Utama", accountNumber = "132-00-55219-0", accountHolder = "Budi Santoso", balance = 12500000.0, currency = "IDR")
        ).toInt()
        val pocketUsdId = repository.insertBankAccount(
            BankAccount(name = "Dompet Valas USD", accountNumber = "99-USD-8812", accountHolder = "Budi Santoso", balance = 800.0, currency = "USD")
        ).toInt()

        // 2. Simulasikan Beberapa Hutang-Piutang Aktif (Tenggat bervariasi)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7) // jatuh tempo 7 hari lagi
        val t7Days = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 14) // jatuh tempo 21 hari lagi
        val t21Days = cal.timeInMillis

        // Pihak lain berhutang ke kita (PIUTANG) - Detail pengeluaran saat dipinjamkan
        repository.createDebtWithTransaction(
            contactName = "Andi Wijaya",
            type = "PIUTANG",
            amount = 1200000.0,
            currency = "IDR",
            description = "Pinjam untuk beli ban motor baru",
            dueDate = t7Days,
            bankAccountId = bcaId
        )

        // Kita berhutang ke pihak lain (HUTANG)
        repository.createDebtWithTransaction(
            contactName = "Rina Malasari",
            type = "HUTANG",
            amount = 3500000.0,
            currency = "IDR",
            description = "Talangan sewa laptop kantor",
            dueDate = t21Days,
            bankAccountId = mandiriId
        )

        // 3. Simulasikan Histori Transaksi Bulanan (Untuk analisis tren belanja)
        val yesterday = System.currentTimeMillis() - 86400000L
        val threeDaysAgo = System.currentTimeMillis() - (86400000L * 3)
        val tenDaysAgo = System.currentTimeMillis() - (86400000L * 10)
        val fourtyDaysAgo = System.currentTimeMillis() - (86400000L * 40) // Bulan lalu

        // Makanan
        repository.insertTransaction(
            Transaction(type = "PENGELUARAN", amount = 150000.0, currency = "IDR", category = "Makanan", description = "Makan Siang Sate Khas Senayan", timestamp = yesterday, bankAccountId = bcaId)
        )
        // Transportasi (USD)
        repository.insertTransaction(
            Transaction(type = "PENGELUARAN", amount = 15.0, currency = "USD", category = "Transportasi", description = "Uber Ride Airport", timestamp = threeDaysAgo, bankAccountId = pocketUsdId)
        )
        // Belanja Bulanan
        repository.insertTransaction(
            Transaction(type = "PENGELUARAN", amount = 450000.0, currency = "IDR", category = "Belanja", description = "Bulanan di LuLu Hypermarket", timestamp = tenDaysAgo, bankAccountId = mandiriId)
        )
        // Kesehatan
        repository.insertTransaction(
            Transaction(type = "PENGELUARAN", amount = 230000.0, currency = "IDR", category = "Kesehatan", description = "Apotek Kimia Farma Vitamin", timestamp = tenDaysAgo, bankAccountId = bcaId)
        )
        // Pemasukan Gaji (Bulan lalu)
        repository.insertTransaction(
            Transaction(type = "PEMASUKAN", amount = 7500000.0, currency = "IDR", category = "Gaji", description = "Gaji Freelance UI Design", timestamp = fourtyDaysAgo, bankAccountId = mandiriId)
        )
        // Pengeluaran Belanja (Bulan lalu)
        repository.insertTransaction(
            Transaction(type = "PENGELUARAN", amount = 1200000.0, currency = "IDR", category = "Belanja", description = "Beli Sepatu Olahraga", timestamp = fourtyDaysAgo, bankAccountId = bcaId)
        )

        isLoading.value = false
    }

    // --- Currency Exchange Helpers ---
    fun convertValue(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val rates = _exchangeRates.value
        val rateFrom = rates[fromCurrency] ?: 1.0
        val rateTo = rates[toCurrency] ?: 1.0

        // Convert to IDR base, then convert to target
        val amountInIdr = amount * rateFrom
        return amountInIdr / rateTo
    }

    fun updateExchangeRate(currency: String, rateInIdr: Double) {
        val current = _exchangeRates.value.toMutableMap()
        current[currency] = rateInIdr
        _exchangeRates.value = current
    }

    // --- Interactive Operations ---

    fun addBankAccount(name: String, accountNumber: String, accountHolder: String, initialBalance: Double, currency: String) {
        viewModelScope.launch {
            repository.insertBankAccount(
                BankAccount(
                    name = name,
                    accountNumber = accountNumber,
                    accountHolder = accountHolder,
                    balance = initialBalance,
                    currency = currency
                )
            )
        }
    }

    fun addManualTransaction(type: String, amount: Double, currency: String, category: String, description: String, bankAccountId: Int?, date: Long) {
        viewModelScope.launch {
            val transId = repository.insertTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    currency = currency,
                    category = category,
                    description = description,
                    timestamp = date,
                    bankAccountId = bankAccountId
                )
            )

            // Update associated bank balance as well
            if (bankAccountId != null) {
                val account = repository.getBankAccountById(bankAccountId)
                if (account != null) {
                    val multiplier = if (type == "PEMASUKAN") 1 else -1
                    val newBalance = account.balance + (multiplier * amount)
                    repository.updateBankAccount(account.copy(balance = newBalance))
                }
            }
        }
    }

    fun addManualDebt(contactName: String, type: String, amount: Double, currency: String, description: String, dueDate: Long, bankAccountId: Int?) {
        viewModelScope.launch {
            repository.createDebtWithTransaction(
                contactName = contactName,
                type = type,
                amount = amount,
                currency = currency,
                description = description,
                dueDate = dueDate,
                bankAccountId = bankAccountId
            )
        }
    }

    fun payDebt(debtId: Int, amount: Double, bankAccountId: Int?, notes: String) {
        viewModelScope.launch {
            repository.payDebtInstallment(
                debtId = debtId,
                installmentAmount = amount,
                bankAccountId = bankAccountId,
                notes = notes
            )
        }
    }

    // --- BANK SYNC WITH AI PARSING ---
    fun parseBankStatementText(
        text: String,
        bankAccountId: Int?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Call Gemini via REST (with local fallback internally)
                val parsed = GeminiParser.parseStatement(text)
                
                if (parsed.amount > 0.0) {
                    // Simpan transaksi
                    val transaction = Transaction(
                        type = parsed.type,
                        amount = parsed.amount,
                        currency = parsed.currency,
                        category = parsed.category,
                        description = "[AI Sync] " + parsed.description,
                        timestamp = System.currentTimeMillis(),
                        bankAccountId = bankAccountId
                    )
                    repository.insertTransaction(transaction)

                    // Update balance bank
                    if (bankAccountId != null) {
                        val account = repository.getBankAccountById(bankAccountId)
                        if (account != null) {
                            val conversionRate = convertValue(1.0, parsed.currency, account.currency)
                            val convertedAmount = parsed.amount * conversionRate

                            val balanceDelta = if (parsed.type == "PEMASUKAN") convertedAmount else -convertedAmount
                            repository.updateBankAccount(account.copy(balance = account.balance + balanceDelta))
                        }
                    }

                    onResult(true, "AI berhasil mendeteksi transaksi: ${parsed.type} ${parsed.currency} ${String.format("%,.0f", parsed.amount)} untuk Kategori [${parsed.category}] (${parsed.description})")
                } else {
                    onResult(false, "Sistem tidak berhasil mendeteksi angka nominal transaksi dari teks tersebut.")
                }
            } catch (e: Exception) {
                onResult(false, "Terjadi kesalahan pemrosesan AI: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
}
