package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

data class ParsedTransaction(
    val type: String,        // "PEMASUKAN" or "PENGELUARAN"
    val amount: Double,
    val currency: String,    // "IDR", "USD", etc.
    val category: String,    // "Makanan", "Transportasi", "Belanja", etc.
    val description: String
)

object GeminiParser {
    private const val TAG = "GeminiParser"
    private const val MODEL = "gemini-3.5-flash"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Parse text SMS / notifikasi bank menggunakan Gemini API (REST)
     * Menggunakan fallback cerdas berbasis lisan bahasa Indonesia jika API gagal.
     */
    suspend fun parseStatement(text: String): ParsedTransaction = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            Log.w(TAG, "Gemini API Key is placeholder. Running fallback local parser.")
            return@withContext runLocalFallback(text)
        }

        try {
            // Membangun skema JSON untuk Gemini
            val schemaJson = JSONObject().apply {
                put("type", "OBJECT")
                val properties = JSONObject().apply {
                    put("type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Tipe aliran dana. Harus bernilai 'PEMASUKAN' atau 'PENGELUARAN'")
                    })
                    put("amount", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Nominal transaksi dalam angka riil tanpa separator ribuan")
                    })
                    put("currency", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Mata uang 3 huruf kapital, default 'IDR'")
                    })
                    put("category", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Kategori transaksi. Pilih dari: Makanan, Transportasi, Belanja, Kesehatan, Gaji, Hutang-Piutang, Lainnya")
                    })
                    put("description", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Deskripsi ringkas aktivitas transaksi yang dibaca")
                    })
                }
                put("properties", properties)
                put("required", JSONArray().apply {
                    put("type")
                    put("amount")
                    put("currency")
                    put("category")
                    put("description")
                })
            }

            // Membangun payload request
            val partPrompt = JSONObject().apply {
                put("text", "Ekstrak teks notifikasi transaksi bank ini: \"$text\"")
            }
            val contentObj = JSONObject().apply {
                put("parts", JSONArray().apply { put(partPrompt) })
            }
            val contentsArr = JSONArray().apply { put(contentObj) }

            val responseFormatText = JSONObject().apply {
                put("mimeType", "application/json")
                put("schema", schemaJson)
            }
            val responseFormatObj = JSONObject().apply {
                put("text", responseFormatText)
            }
            val generationConfigObj = JSONObject().apply {
                put("responseFormat", responseFormatObj)
                put("temperature", 0.1)
            }

            val systemInstructionPart = JSONObject().apply {
                put("text", "Anda adalah asisten keuangan cerdas Indonesia. Tugas Anda mengekstrak info notifikasi mutasi rekening / bank.")
            }
            val systemInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply { put(systemInstructionPart) })
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArr)
                put("generationConfig", generationConfigObj)
                put("systemInstruction", systemInstructionObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API call unsuccessful: HTTP ${response.code}. Details: $errorBody")
                    return@withContext runLocalFallback(text)
                }

                val responseStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val responseText = parts.getJSONObject(0).getString("text")

                Log.d(TAG, "Gemini structured output: $responseText")
                val parsedJson = JSONObject(responseText)
                
                ParsedTransaction(
                    type = parsedJson.optString("type", "PENGELUARAN").uppercase(),
                    amount = parsedJson.optDouble("amount", 0.0),
                    currency = parsedJson.optString("currency", "IDR").uppercase(),
                    category = parsedJson.optString("category", "Lainnya"),
                    description = parsedJson.optString("description", "Transaksi Terurai AI")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in GeminiParser, running fallback", e)
            runLocalFallback(text)
        }
    }

    /**
     * Fallback cerdas lokal berbasis pola regex bahasa Indonesia
     */
    private fun runLocalFallback(text: String): ParsedTransaction {
        // Tentukan type
        val isIncome = text.contains("masuk", ignoreCase = true) ||
                text.contains("terima", ignoreCase = true) ||
                text.contains("ditransfer dari", ignoreCase = true) ||
                text.contains("pemasukan", ignoreCase = true) ||
                text.contains("gaji", ignoreCase = true) ||
                text.contains("kredit", ignoreCase = true) ||
                text.contains("cr", ignoreCase = true)

        val type = if (isIncome) "PEMASUKAN" else "PENGELUARAN"

        // Ekstrak angka / nominal
        var amount = 0.0
        // Cari pola Rp 150.000, IDR 10.000, 50,000 dsb.
        val amountPattern = Pattern.compile("(?:rp|idr|usd)\\s*([\\d.,]+)|([\\d.,]+)\\s*(?:rupiah|dollar)", Pattern.CASE_INSENSITIVE)
        val matcher = amountPattern.matcher(text)
        if (matcher.find()) {
            val numStr = matcher.group(1) ?: matcher.group(2)
            if (numStr != null) {
                // Bersihkan string numerik dari titik ribuan, atau koma pecahan
                // Untuk IDR biasanya titik adalah ribuan '150.000' -> 150000
                // Mari kita tangani secara cerdas
                val cleaned = if (numStr.contains('.') && numStr.contains(',')) {
                    // misal 1,500.50
                    numStr.replace(",", "")
                } else if (numStr.contains('.')) {
                    // misal 150.000 (ribuan) atau 1.5 (pecahan)
                    // Jika titik berada di posisi 3 digit terakhir, kemungkinan ribuan
                    val lastDotIndex = numStr.lastIndexOf('.')
                    if (numStr.length - lastDotIndex == 4) {
                        numStr.replace(".", "")
                    } else {
                        numStr
                    }
                } else if (numStr.contains(',')) {
                    // misal 150,000 (ribuan) atau 1,5 (pecahan)
                    val lastCommaIndex = numStr.lastIndexOf(',')
                    if (numStr.length - lastCommaIndex == 4) {
                        numStr.replace(",", "")
                    } else {
                        numStr.replace(",", ".")
                    }
                } else {
                    numStr
                }
                amount = cleaned.toDoubleOrNull() ?: 0.0
            }
        } else {
            // Pola angka murni
            val rawNumPattern = Pattern.compile("(\\d{4,9})")
            val rawMatcher = rawNumPattern.matcher(text)
            if (rawMatcher.find()) {
                amount = rawMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            }
        }

        // Tentukan currency
        val currency = when {
            text.contains("usd", ignoreCase = true) || text.contains("$") -> "USD"
            text.contains("eur", ignoreCase = true) || text.contains("€") -> "EUR"
            text.contains("sgd", ignoreCase = true) || text.contains("s$") -> "SGD"
            else -> "IDR"
        }

        // Tentukan kategori & deskripsi
        var category = "Lainnya"
        var description = "Transaksi Otomatis"

        val lowerText = text.lowercase()
        when {
            lowerText.contains("makan") || lowerText.contains("kopi") || lowerText.contains("resto") || lowerText.contains("kuliner") -> {
                category = "Makanan"
                description = "Konsumsi / Kuliner"
            }
            lowerText.contains("gofood") || lowerText.contains("grabfood") || lowerText.contains("shopeefood") -> {
                category = "Makanan"
                description = "Pesan Antar Makanan"
            }
            lowerText.contains("ojek") || lowerText.contains("gojek") || lowerText.contains("grab") || lowerText.contains("bensin") || lowerText.contains("toll") || lowerText.contains("parkir") -> {
                category = "Transportasi"
                description = "Biaya Perjalanan"
            }
            lowerText.contains("belanja") || lowerText.contains("tokopedia") || lowerText.contains("shopee") || lowerText.contains("supermarket") || lowerText.contains("indomaret") || lowerText.contains("alfamart") -> {
                category = "Belanja"
                description = "Belanja Kebutuhan"
            }
            lowerText.contains("sakit") || lowerText.contains("obat") || lowerText.contains("dokter") || lowerText.contains("apotek") || lowerText.contains("klinik") -> {
                category = "Kesehatan"
                description = "Biaya Kesehatan"
            }
            lowerText.contains("gaji") || lowerText.contains("salary") || lowerText.contains("upah") || lowerText.contains("fee") -> {
                category = "Gaji"
                description = "Pemasukan Gaji"
            }
            lowerText.contains("pinjam") || lowerText.contains("hutang") || lowerText.contains("piutang") || lowerText.contains("talangan") -> {
                category = "Hutang-Piutang"
                description = "Transaksi Hutang-Piutang"
            }
        }

        if (text.length > 5) {
            description = text.take(60) + if (text.length > 60) "..." else ""
        }

        return ParsedTransaction(
            type = type,
            amount = amount,
            currency = currency,
            category = category,
            description = description
        )
    }
}
