package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.model.BankAccount
import com.example.data.model.Debt
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.lang.Math.max

class FinanceRepository(private val financeDao: FinanceDao) {

    // --- Flows ---
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allBankAccounts: Flow<List<BankAccount>> = financeDao.getAllBankAccounts()
    val allDebts: Flow<List<Debt>> = financeDao.getAllDebts()

    // --- Basic CRUD ---
    suspend fun insertTransaction(transaction: Transaction): Long = financeDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = financeDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = financeDao.deleteTransaction(transaction)
    suspend fun deleteTransactionById(id: Int) = financeDao.deleteTransactionById(id)

    suspend fun insertBankAccount(bankAccount: BankAccount): Long = financeDao.insertBankAccount(bankAccount)
    suspend fun updateBankAccount(bankAccount: BankAccount) = financeDao.updateBankAccount(bankAccount)
    suspend fun deleteBankAccount(bankAccount: BankAccount) = financeDao.deleteBankAccount(bankAccount)
    suspend fun getBankAccountById(id: Int): BankAccount? = financeDao.getBankAccountById(id)

    suspend fun insertDebt(debt: Debt): Long = financeDao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = financeDao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = financeDao.deleteDebt(debt)
    suspend fun getDebtById(id: Int): Debt? = financeDao.getDebtById(id)

    // --- Complex Business Workflows ---

    /**
     * Workflow 1: Melakukan pencatatan awal hutang/piutang (Lending/Borrowing)
     * - Memasukkan data hutang/piutang ke database.
     * - Memperbarui saldo rekening bank yang bersangkutan.
     * - Memasukkan transaksi terkait yang saling berhubungan.
     */
    suspend fun createDebtWithTransaction(
        contactName: String,
        type: String, // "PIUTANG" (dipinjamkan) atau "HUTANG" (meminjam)
        amount: Double,
        currency: String,
        description: String,
        dueDate: Long,
        bankAccountId: Int?
    ) {
        // 1. Buat records Debt
        val debt = Debt(
            contactName = contactName,
            type = type,
            totalAmount = amount,
            remainingAmount = amount,
            currency = currency,
            description = description,
            dueDate = dueDate,
            status = "BELUM_LUNAS"
        )
        val debtId = financeDao.insertDebt(debt).toInt()

        // 2. Transaksi nominal & penyesuaian saldo rekening
        if (bankAccountId != null) {
            val account = financeDao.getBankAccountById(bankAccountId)
            if (account != null) {
                // Diperlukan konversi jika mata uang berbeda, tapi untuk kenyamanan mari asumsikan
                // mata uang rekening sama, atau kita sesuaikan langsung nominal saldonya.
                val balanceDelta = if (type == "PIUTANG") -amount else amount
                val updatedAccount = account.copy(balance = account.balance + balanceDelta)
                financeDao.updateBankAccount(updatedAccount)
            }
        }

        // 3. Catat transaksi terkait
        val transactionType = if (type == "PIUTANG") "PENGELUARAN" else "PEMASUKAN"
        val category = "Hutang-Piutang"
        val transactionDescription = if (type == "PIUTANG") {
            "Meminjamkan kepada $contactName: $description"
        } else {
            "Menerima pinjaman dari $contactName: $description"
        }

        val transaction = Transaction(
            type = transactionType,
            amount = amount,
            currency = currency,
            category = category,
            description = transactionDescription,
            bankAccountId = bankAccountId,
            debtId = debtId
        )
        financeDao.insertTransaction(transaction)
    }

    /**
     * Workflow 2: Pembayaran cicilan pelunasan hutang/piutang (Debt/Receivable Repayment)
     * - Memperbarui sisa pelunasan hutang.
     * - Menandai LUNAS jika sisa = 0.
     * - Memperbarui saldo rekening bank terkait.
     * - Membuat transaksi pencatatan pemasukan/pengeluaran baru yang dikaitkan ke debtId.
     */
    suspend fun payDebtInstallment(
        debtId: Int,
        installmentAmount: Double,
        bankAccountId: Int?,
        notes: String
    ) {
        val debt = financeDao.getDebtById(debtId) ?: return
        if (debt.status == "LUNAS") return // Sudah lunas tidak perlu bayar lagi

        // 1. Hitung sisa hutang
        val newRemaining = max(0.0, debt.remainingAmount - installmentAmount)
        val newStatus = if (newRemaining <= 0.0) "LUNAS" else "BELUM_LUNAS"

        val updatedDebt = debt.copy(
            remainingAmount = newRemaining,
            status = newStatus
        )
        financeDao.updateDebt(updatedDebt)

        // 2. Sesuaikan saldo rekening bank
        if (bankAccountId != null) {
            val account = financeDao.getBankAccountById(bankAccountId)
            if (account != null) {
                // Sisi Piutang -> kita meminjamkan -> jika mereka mencicil -> saldo bank BERTAMBAH
                // Sisi Hutang -> kita meminjam -> jika kita mencicil ke mereka -> saldo bank BERKURANG
                val balanceDelta = if (debt.type == "PIUTANG") installmentAmount else -installmentAmount
                val updatedAccount = account.copy(balance = account.balance + balanceDelta)
                financeDao.updateBankAccount(updatedAccount)
            }
        }

        // 3. Masukkan transaksi pembayaran cicilan
        val transactionType = if (debt.type == "PIUTANG") "PEMASUKAN" else "PENGELUARAN"
        val category = "Hutang-Piutang"
        val transactionDescription = if (debt.type == "PIUTANG") {
            "Terima cicilan piutang dari ${debt.contactName}: $notes"
        } else {
            "Bayar cicilan hutang ke ${debt.contactName}: $notes"
        }

        val transaction = Transaction(
            type = transactionType,
            amount = installmentAmount,
            currency = debt.currency,
            category = category,
            description = transactionDescription,
            bankAccountId = bankAccountId,
            debtId = debtId
        )
        financeDao.insertTransaction(transaction)
    }
}
