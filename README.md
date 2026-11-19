# NavigationLab

Aplikasi Android multi-screen berbasis **Jetpack Compose** yang dikembangkan bertahap pada praktikum mata kuliah **Pemrograman Aplikasi Perangkat Bergerak (CIF62041)**, S1 Teknik Informatika, Fakultas Ilmu Komputer, Universitas Brawijaya.

Repository ini memuat hasil tiga modul pada project yang sama:

| Modul | Topik | Inti perubahan |
|---|---|---|
| **06** | Arsitektur Aplikasi Android | Memisahkan UI layer dan data layer (ViewModel, repository, data source) |
| **07** | REST API dan Retrofit | Mengganti data in-memory dengan data dari REST API memakai Retrofit callback |
| **08** | Kotlin Coroutines dan Flow | Mengganti callback dengan `suspend`, `StateFlow`, dan pencarian reaktif berbasis Flow |
| **09** | Persistensi Data Lokal | Mengimplementasikan Room Database untuk fitur Favorites dan DataStore Preferences untuk menyimpan preferensi pengguna |

---

## Identitas

| Keterangan | Isi                                                |
|---|----------------------------------------------------|
| Nama | Aniza Helwa                                        |
| NIM | *245150201111043*                                  |
| Kelas | *D*                                                |
| Mata kuliah | Pemrograman Aplikasi Perangkat Bergerak (CIF62041) |
| Modul | 06, 07, dan 08                                     |
| Repository | https://github.com/AnizaHelwa/NavigationLab        |

---

## Daftar Isi

1. [Fitur](#fitur)
2. [Teknologi](#teknologi)
3. [Arsitektur](#arsitektur)
4. [Struktur Project](#struktur-project)
5. [Alur Data](#alur-data)
6. [Konsep Coroutines dan Flow yang Dipakai](#konsep-coroutines-dan-flow-yang-dipakai)
7. [REST API yang Digunakan](#rest-api-yang-digunakan)
8. [Cara Menjalankan](#cara-menjalankan)
9. [Penjelasan Per Modul](#penjelasan-per-modul)
10. [Tugas Praktikum Modul 08: Refresh](#tugas-praktikum-modul-08-refresh)
11. [Pengujian](#pengujian)
12. [Screenshot](#screenshot)
13. [Troubleshooting](#troubleshooting)
14. [Riwayat Commit](#riwayat-commit)
15. [Referensi](#referensi)

---

## Fitur

- **Home**: daftar profil dari REST API (DummyJSON) dengan nama dan ID.
- **Pencarian reaktif**: mengetik di kolom pencarian langsung memicu pencarian setelah pengguna berhenti mengetik sekitar 500 ms, tanpa tombol Cari.
- **Refresh**: tombol untuk memuat ulang data. Jika kolom kosong, daftar awal dimuat; jika berisi teks, pencarian diulang dengan query aktif.
- **Detail**: menampilkan nama, ID, email, dan university dari profil yang dipilih.
- **Loading state**: indikator putar saat data diminta.
- **Error state**: pesan error dan tombol **Coba Lagi** ketika request gagal (misalnya internet mati), tanpa crash.
- **Empty state**: teks "Tidak ada hasil" ketika pencarian tidak menemukan data.
- **Navigasi**: Home, Detail, Profile, dan About dengan Navigation Compose, termasuk argument `studentId` dan tombol Kembali.
- **Favorites**: pengguna dapat menyimpan dan menghapus mahasiswa favorit melalui DetailScreen.
- **FavoritesScreen**: menampilkan daftar mahasiswa favorit yang tersimpan di Room Database, meliputi nama dan email.
- **Sinkronisasi data favorit**: daftar favorit diperbarui secara otomatis melalui Flow ketika data ditambahkan atau dihapus, tanpa refresh manual.
- **Preferensi pengurutan**: pengguna dapat mengatur urutan nama mahasiswa secara ascending (A–Z) atau descending (Z–A).
- **Persistensi preferensi**: riwayat pencarian terakhir dan preferensi pengurutan disimpan menggunakan DataStore sehingga tetap tersedia setelah aplikasi dimulai ulang.

---

## Teknologi

| Komponen | Keterangan |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigasi | Navigation Compose |
| State holder | `ViewModel` dengan `MutableStateFlow` / `StateFlow` |
| Pola asinkron | Kotlin Coroutines (`suspend`, `viewModelScope`) dan Flow |
| Koleksi state di UI | `collectAsStateWithLifecycle()` |
| HTTP client | Retrofit 3.0.0 (suspend function) |
| JSON converter | Gson (`converter-gson` 3.0.0) |
| Coroutines | `kotlinx-coroutines-android` 1.11.0 |
| Lifecycle | `lifecycle-viewmodel-ktx`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` 2.9.4 |
| Sumber data | [DummyJSON](https://dummyjson.com/docs/users) |
| Database lokal | Room Persistence Library |
| Penyimpanan preferensi | DataStore Preferences |
| Pemrosesan kode database | Kotlin Symbol Processing (KSP) |

---

## Arsitektur

Aplikasi memakai arsitektur berlapis dengan prinsip **separation of concerns**. UI tidak pernah mengakses sumber data secara langsung.

```
UI Layer                State Holder              Data Layer
---------------------   -----------------------   ------------------------------------------
HomeScreen         <--  HomeViewModel        -->  StudentRepository (interface)
DetailScreen       <--  DetailViewModel           DefaultStudentRepository
 (collectAsState-       (viewModelScope,              |  suspend
  WithLifecycle)         StateFlow)                   v
                                                  StudentRemoteDataSource
                                                      |  suspend
                                                      v
                                                  StudentApiService (Retrofit)
                                                      |
                                                      v
                                                  REST API (DummyJSON)
```

**Event bergerak ke dalam, state bergerak ke luar.** UI mengirim event (ketik, klik, refresh) ke ViewModel, dan ViewModel mengembalikan state yang dikoleksi UI.

| Komponen | Tanggung jawab |
|---|---|
| **Composable screen** | Menampilkan state dan mengirim event. Tidak memuat data dan tidak membuat coroutine. |
| **ViewModel** | Memegang UI state sebagai `StateFlow`, menjalankan coroutine lewat `viewModelScope`, dan memanggil repository. |
| **Repository** | Satu pintu akses data. Memetakan DTO menjadi model aplikasi. |
| **Remote data source** | Memanggil API dan mengembalikan DTO. Kegagalan dilempar sebagai exception. |
| **Retrofit service** | Mendefinisikan endpoint sebagai `suspend fun`. |
| **DTO** | Bentuk data yang mengikuti struktur JSON dari API. |
| **Model** | `Student`, bentuk data yang dipakai aplikasi. |

### Aturan dependency

- `HomeScreen` dan `DetailScreen` hanya mengenal `UiState` dan callback. Keduanya **tidak mengimpor** Retrofit, `StudentApiService`, `RetrofitClient`, repository, maupun `kotlinx.coroutines`, dan tidak membuat `CoroutineScope`.
- ViewModel hanya bergantung pada interface `StudentRepository`. Ia tidak mengimpor `NavController` atau composable.
- Seluruh coroutine memakai `viewModelScope`. Tidak ada `GlobalScope`.
- Dependency dirakit manual di `NavigationLabApp`: `RetrofitClient` → `StudentRemoteDataSource` → `DefaultStudentRepository` → ViewModel (lewat factory).

---

## Struktur Project

```
com.example.navigationlab/
├── data/
│   ├── model/
│   │   └── Student.kt                  # id, name, email, university
│   ├── remote/
│   │   ├── dto/
│   │   │   ├── UserDto.kt              # DTO + mapper toStudent()
│   │   │   └── UserResponse.kt         # users, total, skip, limit
│   │   ├── StudentApiService.kt        # endpoint Retrofit (suspend)
│   │   ├── RetrofitClient.kt           # singleton Retrofit + Gson
│   │   └── StudentRemoteDataSource.kt  # suspend fun getStudents/getStudent/searchStudents
│   └── repository/
│       ├── StudentRepository.kt        # interface (suspend)
│       └── DefaultStudentRepository.kt # implementasi, memetakan DTO ke Student
├── ui/
│   ├── home/
│   │   ├── HomeUiState.kt
│   │   └── HomeViewModel.kt            # searchQuery, refresh, retry, observeRequests
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

### Memuat dan mencari data

Contoh: pengguna mengetik `john`.

1. Kolom pencarian memanggil `onSearchQueryChange`, yang diteruskan ke `HomeViewModel.onSearchQueryChange()`.
2. ViewModel mengubah `searchQuery` (`MutableStateFlow`).
3. Di `observeRequests()`, aliran query melewati `debounce(500)` dan `distinctUntilChanged()`, lalu masuk ke `collectLatest`.
4. `fetchStudents(query)` menyalakan `isLoading`, lalu memanggil `repository.searchStudents("john")` (atau `getStudents()` jika query kosong).
5. Repository memanggil remote data source, lalu Retrofit. Coroutine ditangguhkan sampai response datang.
6. Response diubah Gson menjadi DTO, lalu dipetakan menjadi `Student`.
7. ViewModel mengisi `uiState`. `HomeScreen` mengoleksinya lewat `collectAsStateWithLifecycle()` dan digambar ulang.
8. Jika gagal, exception ditangkap dan `uiState` berisi `errorMessage`, sehingga UI menampilkan pesan dan tombol **Coba Lagi**.

### Refresh dan Coba Lagi

Tombol **Refresh** dan **Coba Lagi** tidak memulai request sendiri. Keduanya hanya mengirim sinyal ke `refreshTrigger`. Sinyal itu digabung (`merge`) dengan aliran query pencarian dan diproses oleh **satu** `collectLatest`. Akibatnya pencarian otomatis, Coba Lagi, dan Refresh memakai jalur yang sama, dan hanya satu pekerjaan yang aktif pada satu waktu.

---

## Konsep Coroutines dan Flow yang Dipakai

| Konsep | Penjelasan singkat | Dipakai di |
|---|---|---|
| `suspend` | Menandai fungsi yang boleh ditangguhkan tanpa memblokir thread. Hanya bisa dipanggil dari coroutine atau fungsi suspend lain. | `StudentApiService`, remote data source, repository |
| `viewModelScope` | Scope bawaan ViewModel. Coroutine dibatalkan otomatis saat ViewModel dibersihkan. | `HomeViewModel`, `DetailViewModel` |
| `MutableStateFlow` / `StateFlow` | State yang selalu punya nilai terbaru. Versi mutable disimpan private, UI menerima versi read-only lewat `asStateFlow()`. | `uiState`, `searchQuery` |
| `collectAsStateWithLifecycle()` | Mengubah StateFlow menjadi State Compose dan berhenti mengoleksi saat UI tidak aktif. | `NavigationLabApp` |
| `debounce(500)` | Meneruskan query hanya setelah pengguna berhenti mengetik 500 ms. | `observeRequests()` |
| `distinctUntilChanged()` | Mengabaikan query yang sama dengan sebelumnya. | `observeRequests()` |
| `collectLatest` | Membatalkan pekerjaan lama jika nilai baru datang, sehingga hanya hasil terbaru yang diproses. | `observeRequests()` |
| `merge` + `MutableSharedFlow` | Menggabungkan sinyal tombol (Refresh, Coba Lagi) dengan aliran query ke satu pipeline. | `observeRequests()` |
| `CancellationException` | Sinyal pembatalan coroutine, bukan error. Ditangkap terpisah dan dilempar kembali. | `fetchStudents()`, `DetailViewModel` |

### Mencegah race condition

Pada pencarian yang mengirim request bersamaan, response yang selesai paling akhir bisa menimpa hasil query yang lebih baru. Pada project ini pencegahannya berlapis:

1. `debounce` mengurangi jumlah request yang dikirim.
2. `distinctUntilChanged` menghindari request identik.
3. `collectLatest` membatalkan request lama saat permintaan baru datang, sehingga hasil lama tidak pernah sempat menimpa hasil baru.
4. Refresh dan Coba Lagi dialirkan ke `collectLatest` yang sama, sehingga mereka juga saling membatalkan dengan pencarian otomatis.
5. `CancellationException` dilempar kembali agar pembatalan benar-benar efektif dan tidak tampil sebagai error palsu.

---

## REST API yang Digunakan

Base URL: `https://dummyjson.com/`

| Method | Endpoint | Kegunaan | Fungsi |
|---|---|---|---|
| GET | `users?limit=10` | Daftar 10 profil awal | `getUsers()` |
| GET | `users/{id}` | Detail satu profil | `getUser(id)` |
| GET | `users/search?q={kata}` | Pencarian profil | `searchUsers(query)` |

Field JSON yang dipetakan ke DTO: `id`, `firstName`, `lastName`, `email`, `university`. Model aplikasi menggabungkan `firstName` dan `lastName` menjadi `name`.

Contoh uji di browser:

```
https://dummyjson.com/users?limit=10
https://dummyjson.com/users/search?q=John
```

> Data DummyJSON berisi nama berbahasa Inggris. Untuk mencoba pencarian, gunakan kata seperti `john`, `emily`, atau `michael`.

---

## Cara Menjalankan

### Prasyarat

- Android Studio versi terbaru
- Emulator atau perangkat Android dengan **koneksi internet**

### Langkah

1. Clone repository:
   ```bash
   git clone https://github.com/AnizaHelwa/NavigationLab.git
   ```
2. Buka folder project di Android Studio.
3. Tunggu **Gradle Sync** selesai (library diunduh otomatis, jadi komputer harus terhubung internet).
4. Pilih emulator atau perangkat, lalu klik **Run**.

### Konfigurasi penting

Permission internet di `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Dependency pada `app/build.gradle.kts`:

```kotlin
implementation("com.squareup.retrofit2:retrofit:3.0.0")
implementation("com.squareup.retrofit2:converter-gson:3.0.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
```

Gunakan **satu** versi Lifecycle yang konsisten. Jangan menduplikasi versi berbeda dalam satu project.

---

## Penjelasan Per Modul

### Modul 06: Arsitektur Aplikasi Android

Tujuan: merapikan struktur aplikasi agar tanggung jawab UI, state, akses data, dan sumber data tidak bercampur.

- Membuat package `data` dan `ui`.
- Membuat model `Student` dan `StudentLocalDataSource` (data in-memory).
- Membuat `StudentRepository` (interface) dan `DefaultStudentRepository`.
- Membuat `DetailUiState` dan `DetailViewModel` dengan factory.
- Mengubah `DetailScreen` menjadi UI murni yang hanya menerima `uiState` dan callback.
- Merakit dependency manual di `NavigationLabApp`.
- **Tugas**: daftar mahasiswa di Home lewat `getStudents()`, `HomeUiState`, dan `HomeViewModel`.

### Modul 07: REST API dan Retrofit

Tujuan: mengganti sumber data in-memory dengan REST API sambil mempertahankan arsitektur Modul 06.

- Menambahkan permission INTERNET dan dependency Retrofit + Gson.
- Membuat DTO (`UserDto`, `UserResponse`) dan mapper `toStudent()`.
- Mengubah model `Student` menjadi `id`, `name`, `email`, `university`.
- Membuat `StudentApiService`, `RetrofitClient`, dan `StudentRemoteDataSource` (callback `enqueue`).
- Mengubah repository menjadi asinkron berbasis callback.
- Menambahkan loading dan error state, lalu menghapus `StudentLocalDataSource`.
- **Tugas**: pencarian profil lewat endpoint `users/search` dengan tombol Cari.

### Modul 08: Kotlin Coroutines dan Flow

Tujuan: mengubah pola callback menjadi coroutine agar kode lebih linear, mudah dibaca, dan mengikuti lifecycle.

- **Dependency**: menambahkan `kotlinx-coroutines-android` dan Lifecycle 2.9.4, serta menyamakan versi Lifecycle.
- **Retrofit service**: seluruh endpoint menjadi `suspend` dan return type `Call<T>` diganti tipe data langsung.
- **Remote data source**: kode `enqueue`, `onResponse`, dan `onFailure` dihapus, tersisa tiga fungsi `suspend` pendek.
- **Repository**: semua fungsi menjadi `suspend` tanpa callback dan tanpa pembungkus `Result`.
- **ViewModel**: `mutableStateOf` diganti `MutableStateFlow` private yang diekspos sebagai `StateFlow`. Pekerjaan dijalankan lewat `viewModelScope.launch`, error ditangani `try/catch`.
- **Compose**: state dikoleksi dengan `collectAsStateWithLifecycle()`.
- **Pencarian reaktif**: `searchQuery` menjadi `StateFlow` dan diproses `debounce`, `distinctUntilChanged`, dan `collectLatest`. Tombol Cari dihapus.
- **Retry**: tombol Coba Lagi mengulang request sesuai query aktif.

### Modul 09: Persistensi Data Lokal dengan Room dan DataStore

Tujuan: mengimplementasikan penyimpanan data lokal secara persisten menggunakan Room Database dan DataStore Preferences.

- **Room Database**: menambahkan `FavoriteStudentEntity` sebagai representasi tabel mahasiswa favorit.
- **DAO**: membuat `FavoriteStudentDao` untuk menyediakan operasi penyimpanan, penghapusan, dan observasi data favorit.
- **Database**: membuat `AppDatabase` sebagai konfigurasi database Room dengan nama `navigation_lab.db`.
- **Repository**: membuat `FavoriteRepository` sebagai perantara antara ViewModel dan DAO.
- **FavoritesViewModel**: mengamati daftar mahasiswa favorit melalui Flow dan mengubahnya menjadi StateFlow.
- **FavoritesScreen**: menampilkan mahasiswa favorit beserta nama dan email. Item yang dipilih dapat membuka DetailScreen.
- **Sinkronisasi otomatis**: perubahan data favorit pada DetailScreen langsung tercermin di FavoritesScreen melalui Flow dari Room.
- **DataStore Preferences**: menambahkan preferensi `sortAscending` untuk mengatur urutan nama mahasiswa secara A–Z atau Z–A.
- **Persistensi preferensi**: menyimpan pilihan pengurutan dan riwayat pencarian agar tetap tersedia setelah aplikasi ditutup dan dibuka kembali.
---

## Tugas Praktikum Modul 08: Refresh

| No | Ketentuan | Implementasi |
|---|---|---|
| 1 | Tombol Refresh di HomeScreen | `OutlinedButton` "Refresh" di bawah kolom pencarian, nonaktif saat loading |
| 2 | Tombol memanggil `refresh()` | `onRefresh = homeViewModel::refresh` di `NavigationLabApp` |
| 3 | `refresh()` memakai `viewModelScope.launch` | `viewModelScope.launch { refreshTrigger.emit(Unit) }` |
| 4 | Query kosong memuat daftar awal | `fetchStudents()`: `if (query.isBlank()) getStudents()` |
| 5 | Query berisi mengulang pencarian | Sinyal refresh membawa `searchQuery.value`, lalu `searchStudents(query)` |
| 6 | `isLoading` dan `errorMessage` benar | Diatur pada satu fungsi, `fetchStudents()` |
| 7 | Tanpa `CoroutineScope` baru di HomeScreen | HomeScreen hanya memanggil callback `onRefresh` |
| 8 | Tanpa `GlobalScope` | Semua coroutine memakai `viewModelScope` |
| 9 | Detail dan Back tetap berjalan | Destination Detail tidak diubah, `HomeViewModel` bertahan di back stack |
| 10 | Screenshot sebelum dan sesudah Refresh | Lihat bagian [Screenshot](#screenshot) |

Rancangan penting: `refresh()` hanya mengirim sinyal, sedangkan request dijalankan di `observeRequests()` lewat `collectLatest`. Dengan begitu Refresh, Coba Lagi, dan pencarian otomatis tidak bisa saling menimpa hasilnya.

---

## Pengujian

Pengujian dilakukan manual pada emulator.

| No | Skenario | Hasil yang diharapkan |
|---|---|---|
| 1 | Gradle Sync | Berhasil tanpa error |
| 2 | Membuka Home | Loading muncul, lalu 10 profil dari API tampil |
| 3 | Klik salah satu profil | Detail menampilkan name, email, dan university |
| 4 | Tombol Kembali | Kembali ke Home, daftar dan teks pencarian tetap |
| 5 | Ketik `john` lalu diam sekitar 1 detik | Hasil terfilter otomatis tanpa tombol Cari |
| 6 | Ketik cepat `j`, `jo`, `joh`, `john` | Hanya query terakhir yang diproses |
| 7 | Kosongkan kolom pencarian | Daftar awal dimuat kembali |
| 8 | Cari kata yang tidak ada (`zzzz`) | Muncul "Tidak ada hasil" |
| 9 | Tekan Refresh, kolom kosong | Daftar awal dimuat ulang |
| 10 | Tekan Refresh, kolom berisi `john` | Pencarian `john` diulang |
| 11 | Internet dimatikan lalu Refresh | Error state tampil tanpa crash |
| 12 | Internet dinyalakan lalu Coba Lagi | Request diulang sesuai query aktif dan data tampil |
| 13 | Refresh lalu buka Detail dan Kembali | Home tetap normal tanpa crash |
| 14 | Profile dan About | Tetap dapat dibuka dan kembali |
| 15 | Cek batas layer | Tidak ada `GlobalScope`, `enqueue`, `Callback`, atau `Call<` di project, dan `HomeScreen` tidak mengimpor Retrofit, repository, atau coroutine |
| 16 | Menyimpan mahasiswa sebagai favorit | Data mahasiswa berhasil disimpan ke Room Database |
| 17 | Membuka FavoritesScreen | Daftar mahasiswa favorit menampilkan nama dan email |
| 18 | Menghapus mahasiswa favorit dari DetailScreen | Data pada FavoritesScreen otomatis diperbarui |
| 19 | Menekan item mahasiswa pada FavoritesScreen | Aplikasi membuka DetailScreen mahasiswa terkait |
| 20 | Mengubah urutan menjadi A–Z atau Z–A | Daftar mahasiswa diurutkan sesuai preferensi |
| 21 | Menutup dan membuka kembali aplikasi | Preferensi pengurutan tetap tersimpan |
| 22 | Menguji riwayat pencarian setelah restart | Query pencarian terakhir berhasil dipulihkan |

---

## Screenshot

> Ganti nama file di bawah dengan screenshot milikmu, lalu simpan di folder `docs/screenshots/`. Hapus bagian ini jika tidak memasang gambar.

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

### Modul 08

| Keterangan | Gambar |
|---|---|
| `StudentApiService` berisi suspend function | ![API service](docs/screenshots/m08_01_api_service_suspend.png) |
| `HomeViewModel` dengan StateFlow dan viewModelScope | ![HomeViewModel](docs/screenshots/m08_02_homeviewmodel_stateflow.png) |
| Home menampilkan data awal API | ![Home](docs/screenshots/m08_03_home_data_awal.png) |
| Pencarian reaktif setelah debounce | ![Pencarian](docs/screenshots/m08_04_pencarian_reaktif.png) |
| Detail hasil REST API | ![Detail](docs/screenshots/m08_05_detail_api.png) |
| Error state saat internet dimatikan | ![Error](docs/screenshots/m08_06_error_state.png) |
| Aplikasi tetap berjalan setelah retry | ![Retry](docs/screenshots/m08_07_setelah_retry.png) |
| Sebelum Refresh | ![Sebelum Refresh](docs/screenshots/m08_08_sebelum_refresh.png) |
| Sesudah Refresh | ![Sesudah Refresh](docs/screenshots/m08_09_sesudah_refresh.png) |
| Loading saat Refresh (opsional) | ![Loading](docs/screenshots/m08_10_loading_saat_refresh.png) |

---

## Troubleshooting

| Gejala | Penyebab dan perbaikan |
|---|---|
| `UnknownHostException` | Internet emulator/perangkat mati atau permission `INTERNET` belum ada. |
| `HTTP 404` | Periksa base URL, path `@GET`, `@Path`, dan ID yang diminta. |
| `Expected BEGIN_OBJECT` / parsing error | DTO tidak sesuai struktur JSON. Periksa nama property. |
| Crash `baseUrl must end in /` | `BASE_URL` di `RetrofitClient` harus diakhiri `/`. |
| `Unresolved reference: collectAsStateWithLifecycle` | Tambahkan `lifecycle-runtime-compose` dan import `androidx.lifecycle.compose.collectAsStateWithLifecycle`. |
| `Unresolved reference: viewModelScope` | Pastikan `lifecycle-viewmodel-ktx` tersedia dan import `androidx.lifecycle.viewModelScope`. |
| `Suspend function can be called only within coroutine` | Panggil fungsi suspend dari `viewModelScope.launch` atau fungsi suspend lain. |
| Request terkirim dua kali saat dibuka | Pastikan `init` hanya memanggil `observeRequests()` dan tidak ada pemanggilan pemuatan data lain. |
| `Unresolved reference: merge` / `map` | Impor dari `kotlinx.coroutines.flow`. |
| Peringatan `FlowPreview` | Tambahkan `@OptIn(FlowPreview::class)` di atas fungsi yang memakai `debounce`. |
| Pesan "error" muncul saat request diganti | `CancellationException` harus ditangkap terpisah dan dilempar kembali (`throw e`) sebelum `catch (e: Exception)`. |
| Tombol Refresh tidak muncul | Pastikan `OutlinedButton` ada di `HomeScreen.kt` di luar blok `when`, lalu jalankan ulang aplikasi (Run, bukan Apply Changes). |
| `No value passed for parameter 'onRefresh'` | Tambahkan `onRefresh = homeViewModel::refresh` pada pemanggilan `HomeScreen`. |
| UI tidak berubah | Pastikan ViewModel mengubah `_uiState.value` dan UI mengoleksi dengan `collectAsStateWithLifecycle`. |
| `git push` ditolak (`fetch first`) | Remote punya commit yang belum ada lokal. Jalankan `git pull --rebase origin main`, lalu `git push`. |

---

## Riwayat Commit

| Commit | Isi |
|---|---|
| `Selesai Modul 06` | Arsitektur berlapis dengan data in-memory dan daftar mahasiswa di Home |
| `Selesai Modul 07: REST API dan Retrofit` | Retrofit callback, DTO, remote data source, loading dan error state |
| `Tugas Modul 07: fitur pencarian` | Pencarian profil dengan tombol Cari |
| `Modul 08: coroutines, flow, dan fitur refresh` | Suspend function, StateFlow, pencarian reaktif, dan fitur Refresh |
| `Modul 09: Room Database dan DataStore` | Implementasi FavoritesScreen, penyimpanan mahasiswa favorit menggunakan Room, serta preferensi pengurutan dan riwayat pencarian menggunakan DataStore |

Untuk melihat kode modul sebelumnya, buka commit terkait di tab **Commits** lalu pilih **Browse files**.

---

## Referensi

1. Android Developers. *Guide to app architecture*. https://developer.android.com/topic/architecture
2. Android Developers. *ViewModel overview*. https://developer.android.com/topic/libraries/architecture/viewmodel
3. Android Developers. *Data layer*. https://developer.android.com/topic/architecture/data-layer
4. Android Developers. *Best practices for coroutines in Android*. https://developer.android.com/kotlin/coroutines/coroutines-best-practices
5. Android Developers. *Use Kotlin coroutines with lifecycle-aware components*. https://developer.android.com/topic/libraries/architecture/coroutines
6. Android Developers. *StateFlow and SharedFlow*. https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
7. Square. *Retrofit, a type-safe HTTP client for Android and Java*. https://square.github.io/retrofit/
8. DummyJSON. *Users REST API documentation*. https://dummyjson.com/docs/users
9. Modul Praktikum 06, 07, dan 08, Pemrograman Aplikasi Perangkat Bergerak (CIF62041), FILKOM Universitas Brawijaya, 2026.
10. Android Developers. *Save data in a local database using Room*. https://developer.android.com/training/data-storage/room
11. Android Developers. *DataStore*. https://developer.android.com/topic/libraries/architecture/datastore