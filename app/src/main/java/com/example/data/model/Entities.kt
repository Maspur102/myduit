package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,         // e.g. "BCA", "Bank Mandiri", "Dompet Tunai"
    val accountNumber: String, // e.g. "8023-1112-99"
    val accountHolder: String, // e.g. "Budi Santoso"
    val balance: Double,      // Initial / Current balance
    val currency: String = "IDR"
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,          // "PEMASUKAN", "PENGELUARAN", "PINJAMAN_DILUAR", "HUTANG_MASUK"
    val amount: Double,
    val currency: String,      // "IDR", "USD", "EUR", "SGD"
    val category: String,      // "Makanan", "Transportasi", "Kesehatan", "Pendidikan", "Gaji", "Hutang-Piutang", "Lainnya"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val bankAccountId: Int? = null,
    val debtId: Int? = null    // Linked to Debt if it's a debt creation or installment
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,     // Nama peminjam / pemberi pinjaman
    val type: String,            // "PIUTANG" (dipinjamkan ke orang lain), "HUTANG" (pinjaman dari orang lain)
    val totalAmount: Double,
    val remainingAmount: Double,
    val currency: String = "IDR",
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dueDate: Long,           // Tanggal jatuh tempo pelunasan
    val status: String = "BELUM_LUNAS" // "BELUM_LUNAS", "LUNAS"
)
