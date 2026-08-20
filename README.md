# Catatan Keuangan (Financial Tracker App)

Aplikasi pencatatan keuangan pribadi dan bisnis modern berbasis Android (Jetpack Compose & Material 3), dilengkapi fitur manajemen multi-rekening/valas, rekonsiliasi mutasi berbasis OCR/AI teks, pencatatan hutang-piutang cerdas, dan analitik laporan keuangan.

---

## 🚀 Cara Push ke GitHub & Download APK Otomatis

Proyek ini telah dikonfigurasi secara lengkap dengan **GitHub Actions CI/CD** agar APK Android langsung dibuat secara otomatis setiap kali Anda melakukan push atau trigger manual.

### 1. Inisialisasi & Push ke GitHub
Jalankan perintah berikut di terminal komputer Anda:

```bash
git init
git add .
git commit -m "Initial commit Catatan Keuangan"
git branch -M main
git remote add origin https://github.com/<USERNAME-ANDA>/<NAMA-REPO-ANDA>.git
git push -u origin main
```

---

### 2. Cara Download File APK di GitHub
Setelah Anda melakukan `git push`, GitHub Actions akan otomatis meng-compile dan menghasilkan file APK siap pasang:

1. Buka repositori Anda di GitHub di browser.
2. Klik tab **Actions** di menu atas repositori.
3. Klik alur kerja (workflow) terbaru bernama **"Android CI / Build APK"**.
4. Scroll ke bagian paling bawah ke bagian **Artifacts**.
5. Klik **`CatatanKeuangan-Debug-APK`** untuk mendownload file zip berisi file `.apk`.
6. Ekstrak file zip tersebut di HP Android Anda dan instal aplikasinya.

---

### 3. (Opsional) Mengisi API Key Gemini di GitHub Secrets
Jika Anda ingin fitur sinkronisasi parser AI menggunakan API Key Gemini milik Anda di GitHub Actions:
1. Masuk ke tab **Settings** repositori GitHub Anda.
2. Pilih **Secrets and variables** > **Actions**.
3. Klik **New repository secret**.
4. Isi Name: `GEMINI_API_KEY` dan Secret: `[API Key Gemini Anda]`.
5. Klik **Add secret**.
