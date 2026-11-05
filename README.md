# NavigationLab

Aplikasi Android multi-screen berbasis **Jetpack Compose** yang dikembangkan bertahap pada praktikum mata kuliah **Pemrograman Aplikasi Perangkat Bergerak (CIF62041)**, S1 Teknik Informatika, Fakultas Ilmu Komputer, Universitas Brawijaya.

Repository ini memuat hasil **Modul Praktikum 06 (Arsitektur Aplikasi Android)** dan **Modul Praktikum 07 (REST API & Retrofit)** pada project yang sama. Modul 06 merapikan struktur aplikasi menjadi berlapis, lalu Modul 07 mengganti sumber data lokal menjadi data dari REST API tanpa mengubah banyak bagian UI.

---

## Identitas

| Keterangan | Isi |
|---|---|
| Nama | Aniza Helwa |
| NIM | *(isi NIM)* |
| Kelas | *(isi kelas)* |
| Mata kuliah | Pemrograman Aplikasi Perangkat Bergerak (CIF62041) |
| Modul | 06 (Arsitektur Android) dan 07 (REST API & Retrofit) |
| Repository | https://github.com/AnizaHelwa/NavigationLab |

---

## Daftar Isi

1. [Fitur](#fitur)
2. [Teknologi](#teknologi)
3. [Arsitektur](#arsitektur)
4. [Struktur Project](#struktur-project)
5. [Alur Data](#alur-data)
6. [REST API yang Digunakan](#rest-api-yang-digunakan)
7. [Cara Menjalankan](#cara-menjalankan)
8. [Penjelasan Per Modul](#penjelasan-per-modul)
9. [Pengujian](#pengujian)
10. [Screenshot](#screenshot)
11. [Troubleshooting](#troubleshooting)
12. [Riwayat Commit](#riwayat-commit)
13. [Referensi](#referensi)

---

## Fitur

- **Home**: menampilkan daftar profil mahasiswa dari REST API (DummyJSON), lengkap dengan nama dan ID.
- **Pencarian**: kolom pencarian dan tombol **Cari**. Pencarian hanya berjalan saat tombol ditekan (tanpa debounce). Jika kolom kosong, tombol Cari memuat kembali daftar awal.
- **Detail**: menampilkan nama, ID, email, dan university dari profil yang dipilih.
- **Loading state**: indikator `CircularProgressIndicator` saat data diminta.
- **Error state**: pesan error dan tombol **Coba Lagi** ketika request gagal (misalnya internet mati), tanpa crash.
- **Empty state**: teks "Tidak ada hasil" ketika pencarian tidak menemukan data.
- **Navigasi**: Home, Detail, Profile, dan About dengan Navigation Compose, termasuk argument `studentId` dan tombol Kembali.

---

## Teknologi

| Komponen | Keterangan |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigasi | Navigation Compose |
| State holder | `ViewModel` dengan `mutableStateOf` |
| HTTP client | Retrofit 3.0.0 |
| JSON converter | Gson (`converter-gson` 3.0.0) |
| Pola asinkron | Retrofit callback (`Call.enqueue`) |
| Sumber data | [DummyJSON](https://dummyjson.com/docs/users) |

> Modul ini sengaja memakai callback agar fokus pada REST, JSON, dan arsitektur data. Pada materi berikutnya implementasi network akan direfactor dengan Kotlin Coroutines dan Flow.

---

## Arsitektur

Aplikasi memakai arsitektur berlapis dengan prinsip **separation of concerns**. UI tidak pernah mengakses sumber data secara langsung.

```
UI Layer              State Holder         Data Layer
-----------------     ----------------     -----------------------------------------------
HomeScreen       -->  HomeViewModel   -->  StudentRepository (interface)
DetailScreen     -->  DetailViewModel      DefaultStudentRepository
                                              |
                                              v
                                           StudentRemoteDataSource
                                              |
                                              v
                                           StudentApiService (Retrofit)
                                              |
                                              v
                                           REST API (DummyJSON)
```

Tanggung jawab tiap komponen:

| Komponen | Tanggung jawab |
|---|---|
| **Composable screen** | Menampilkan state dan mengirim event. Tidak memuat data langsung. |
| **ViewModel** | Memegang UI state tingkat layar, menangani event, dan memanggil repository. |
| **Repository** | Satu pintu akses data. Memetakan DTO menjadi model aplikasi. |
| **Remote data source** | Berkomunikasi dengan API dan membungkus hasil dalam `Result`. |
| **Retrofit service** | Mendefinisikan endpoint HTTP sebagai interface Kotlin. |
| **DTO** | Bentuk data yang mengikuti struktur JSON dari API. |
| **Model** | Bentuk data yang dipakai aplikasi (`Student`), tidak bergantung pada UI maupun API. |

### Aturan dependency

- `HomeScreen` dan `DetailScreen` hanya mengenal `UiState` dan callback. Keduanya **tidak mengimpor** Retrofit, `StudentApiService`, `RetrofitClient`, maupun remote data source.
- ViewModel hanya bergantung pada interface `StudentRepository`, bukan implementasi konkretnya. Ia juga tidak mengimpor `NavController` atau composable UI.
- Perakitan dependency dilakukan manual di `NavigationLabApp` (tanpa framework dependency injection): `RetrofitClient` → `StudentRemoteDataSource` → `DefaultStudentRepository` → ViewModel.

---

## Struktur Project

```
com.example.navigationlab/
├── data/
│   ├── model/
│   │   └── Student.kt                  # model aplikasi: id, name, email, university
│   ├── remote/
│   │   ├── dto/
│   │   │   ├── UserDto.kt              # DTO + mapper toStudent()
│   │   │   └── UserResponse.kt         # wrapper response: users, total, skip, limit
│   │   ├── StudentApiService.kt        # endpoint Retrofit (@GET, @Query, @Path)
│   │   ├── RetrofitClient.kt           # singleton Retrofit + Gson converter
│   │   └── StudentRemoteDataSource.kt  # request via enqueue, hasil dalam Result
│   └── repository/
│       ├── StudentRepository.kt        # interface kontrak akses data
│       └── DefaultStudentRepository.kt # implementasi, memetakan DTO ke Student
├── ui/
│   ├── home/
│   │   ├── HomeUiState.kt
│   │   └── HomeViewModel.kt
│   ├── detail/
│   │   ├── DetailUiState.kt
│   │   └── DetailViewModel.kt
│   └── screen/
│       ├── HomeScreen.kt
│       ├── DetailScreen.kt
│       ├── ProfileScreen.kt
│       └── AboutScreen.kt
├── navigation/                         # NavigationLabApp dan Routes
└── MainActivity.kt
```

---

## Alur Data

Contoh: pengguna membuka Home.

1. `HomeViewModel` dibuat dan `init` memanggil `loadStudents()`.
2. ViewModel mengubah state menjadi `isLoading = true`, lalu memanggil `repository.getStudents(...)`.
3. `DefaultStudentRepository` meneruskan permintaan ke `StudentRemoteDataSource`.
4. `StudentRemoteDataSource` menjalankan `StudentApiService.getUsers()` dengan `enqueue`.
5. Response JSON diubah Gson menjadi `UserResponse`, lalu list `UserDto` dibungkus `Result.success`.
6. Repository memetakan `UserDto` menjadi `Student` lewat `toStudent()`.
7. ViewModel memperbarui `uiState`, dan `HomeScreen` merender daftar mahasiswa.

Jika request gagal (tidak ada internet atau HTTP error), `Result.failure` diteruskan sampai ViewModel dan UI menampilkan pesan error beserta tombol **Coba Lagi**.

Prinsipnya: **event bergerak ke dalam, state bergerak ke luar**.

---

## REST API yang Digunakan

Base URL: `https://dummyjson.com/`

| Method | Endpoint | Kegunaan | Dipakai oleh |
|---|---|---|---|
| GET | `users?limit=10` | Daftar 10 profil awal | `getUsers()` |
| GET | `users/{id}` | Detail satu profil | `getUser(id)` |
| GET | `users/search?q={kata}` | Pencarian profil | `searchUsers(query)` |

Field JSON yang dipetakan ke DTO: `id`, `firstName`, `lastName`, `email`, `university`. Field lain diabaikan. Model aplikasi menggabungkan `firstName` dan `lastName` menjadi `name`.

Contoh uji di browser:

```
https://dummyjson.com/users?limit=10
https://dummyjson.com/users/search?q=John
```

> Data DummyJSON berisi nama berbahasa Inggris. Untuk mencoba pencarian, gunakan kata seperti `John`, `Emily`, atau `Michael`.

---

## Cara Menjalankan

### Prasyarat

- Android Studio versi terbaru
- JDK sesuai bawaan Android Studio
- Emulator atau perangkat Android dengan **koneksi internet**

### Langkah

1. Clone repository:
   ```bash
   git clone https://github.com/AnizaHelwa/NavigationLab.git
   ```
2. Buka folder project di Android Studio.
3. Tunggu **Gradle Sync** selesai. Library Retrofit dan Gson diunduh otomatis, jadi komputer harus terhubung internet.
4. Pilih emulator atau perangkat, lalu klik **Run**.

### Konfigurasi penting

Permission internet sudah ditambahkan di `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Dependency pada `app/build.gradle.kts`:

```kotlin
implementation("com.squareup.retrofit2:retrofit:3.0.0")
implementation("com.squareup.retrofit2:converter-gson:3.0.0")
```

---

## Penjelasan Per Modul

### Modul 06: Arsitektur Aplikasi Android

Tujuan: merapikan struktur aplikasi agar tanggung jawab UI, state, akses data, dan sumber data tidak bercampur.

Yang dikerjakan:

- Membuat struktur package `data` dan `ui`.
- Membuat model `Student` dan `StudentLocalDataSource` (data in-memory).
- Membuat `StudentRepository` (interface) dan `DefaultStudentRepository`.
- Membuat `DetailUiState` dan `DetailViewModel` dengan factory.
- Mengubah `DetailScreen` menjadi UI murni yang hanya menerima `uiState` dan callback.
- Merakit dependency secara manual di `NavigationLabApp`.
- **Tugas**: daftar mahasiswa di Home melalui `getStudents()`, `HomeUiState`, dan `HomeViewModel`. Klik item membuka Detail berdasarkan ID, dan `HomeScreen` tidak mengakses data source atau repository secara langsung.

### Modul 07: REST API dan Retrofit

Tujuan: mengganti sumber data in-memory dengan REST API eksternal sambil mempertahankan arsitektur Modul 06.

Yang dikerjakan:

- Menambahkan permission INTERNET dan dependency Retrofit + Gson.
- Membuat DTO (`UserDto`, `UserResponse`) dan mapper `toStudent()`.
- Mengubah model `Student` dari `program` dan `year` menjadi `email` dan `university`.
- Membuat `StudentApiService`, `RetrofitClient`, dan `StudentRemoteDataSource`.
- Mengubah repository menjadi asinkron berbasis callback.
- Menambahkan loading dan error state pada ViewModel dan UI.
- Menghapus `StudentLocalDataSource` lama karena tidak dipakai lagi.
- **Tugas**: fitur pencarian profil lewat endpoint `users/search` (TextField, tombol Cari, dan state `searchQuery`), dengan loading dan error state yang sama.

---

## Pengujian

Pengujian dilakukan secara manual pada emulator.

| No | Skenario | Hasil yang diharapkan |
|---|---|---|
| 1 | Gradle Sync | Berhasil tanpa error |
| 2 | Membuka Home | Loading muncul, lalu 10 profil dari API tampil |
| 3 | Klik salah satu profil | Detail menampilkan name, email, dan university |
| 4 | Tombol Kembali | Kembali ke Home |
| 5 | Cari `John` | Hanya profil yang cocok tampil |
| 6 | Kosongkan kolom lalu Cari | Daftar awal kembali |
| 7 | Cari kata yang tidak ada (`zzzz`) | Muncul "Tidak ada hasil" |
| 8 | Internet dimatikan | Error state tampil tanpa crash |
| 9 | Internet dinyalakan lalu Coba Lagi | Request diulang dan data tampil |
| 10 | Profile dan About | Tetap dapat dibuka dan kembali |
| 11 | Cek batas layer | `HomeScreen` dan `DetailScreen` tidak mengimpor Retrofit, `StudentApiService`, atau `RetrofitClient` |

---

## Screenshot

> Ganti nama file di bawah dengan screenshot milikmu, lalu simpan di folder `docs/screenshots/`.

### Modul 06

| Keterangan | Gambar |
|---|---|
| Struktur package `data` dan `ui` | ![Struktur package](docs/screenshots/m06_struktur_package.png) |
| Detail menampilkan data Student | ![Detail](docs/screenshots/m06_detail.png) |
| `DetailViewModel` memakai `StudentRepository` | ![DetailViewModel](docs/screenshots/m06_detail_viewmodel.png) |
| `DefaultStudentRepository` dan `StudentLocalDataSource` | ![Repository](docs/screenshots/m06_repository_datasource.png) |
| Kembali ke Home setelah Back | ![Home setelah Back](docs/screenshots/m06_back_home.png) |
| Home dengan daftar mahasiswa | ![Home](docs/screenshots/m06_home_list.png) |

### Modul 07

| Keterangan | Gambar |
|---|---|
| Struktur package `data/remote`, `data/repository`, `ui` | ![Struktur package](docs/screenshots/m07_struktur_package.png) |
| JSON endpoint `/users` di browser | ![JSON](docs/screenshots/m07_json_browser.png) |
| Home menampilkan data dari REST API | ![Home API](docs/screenshots/m07_home_api.png) |
| Detail menampilkan name, email, university | ![Detail API](docs/screenshots/m07_detail_api.png) |
| Error state saat internet mati | ![Error state](docs/screenshots/m07_error_state.png) |
| Kembali ke Home setelah Detail | ![Back](docs/screenshots/m07_back_home.png) |
| Hasil pencarian | ![Pencarian](docs/screenshots/m07_pencarian.png) |
| Bukti UI tidak memanggil `StudentApiService` / `RetrofitClient` | ![Bukti layer](docs/screenshots/m07_bukti_layer.png) |

---

## Troubleshooting

| Gejala | Penyebab dan perbaikan |
|---|---|
| `UnknownHostException` | Internet emulator/perangkat mati atau permission `INTERNET` belum ada. |
| `HTTP 404` | Periksa base URL, path `@GET`, `@Path`, dan ID yang diminta. |
| `Expected BEGIN_OBJECT` / parsing error | DTO tidak sesuai struktur JSON. Periksa nama property. |
| Crash `baseUrl must end in /` | `BASE_URL` di `RetrofitClient` harus diakhiri `/`. |
| Layar kosong | Periksa response body, parameter `limit` atau `q`. |
| `Unresolved reference: viewModel` | Tambahkan dependency `lifecycle-viewmodel-compose` dan import `androidx.lifecycle.viewmodel.compose.viewModel`. |
| `Unresolved reference: Student` di ViewModel | Tambahkan import `com.example.navigationlab.data.model.Student`. |
| `Argument type mismatch` di data source lama | Model `Student` sudah berubah. Hapus `StudentLocalDataSource.kt`. |
| UI tidak berubah | Pastikan ViewModel mengubah `uiState` (Compose state) setelah callback selesai. |
| Request terkirim dua kali | Jangan panggil `loadStudents()` di `LaunchedEffect` jika sudah ada di `init` ViewModel. |

---

## Riwayat Commit

| Commit | Isi |
|---|---|
| `Selesai Modul 06` | Arsitektur berlapis dengan data in-memory dan daftar mahasiswa di Home |
| `Selesai Modul 07: REST API dan Retrofit` | Integrasi Retrofit, DTO, remote data source, loading dan error state |
| `Tugas Modul 07: fitur pencarian` | Pencarian profil lewat endpoint `users/search` |

Untuk melihat kode Modul 06, buka commit `Selesai Modul 06` di tab **Commits** lalu pilih **Browse files** (atau gunakan tag `modul-06` jika sudah dibuat).

---

## Referensi

1. Android Developers. *Guide to app architecture*. https://developer.android.com/topic/architecture
2. Android Developers. *ViewModel overview*. https://developer.android.com/topic/libraries/architecture/viewmodel
3. Android Developers. *Data layer*. https://developer.android.com/topic/architecture/data-layer
4. Android Developers. *Get data from the internet*. https://developer.android.com/develop/connectivity/network-ops/connecting
5. Square. *Retrofit, a type-safe HTTP client for Android and Java*. https://square.github.io/retrofit/
6. DummyJSON. *Users REST API documentation*. https://dummyjson.com/docs/users
7. Modul Praktikum 06 dan 07, Pemrograman Aplikasi Perangkat Bergerak (CIF62041), FILKOM Universitas Brawijaya, 2026.
