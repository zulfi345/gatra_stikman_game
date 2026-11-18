# Gatra Stickman - Level 1 (v2)

## Isi
- Lobby (sesuai screenshot): profil, Combat Power, koin/kristal/energi, Battle Pass, Daily Quests, Events, Campaign, Arena PvP, Boss Raid, nav bawah, tombol BATTLE.
- Settings: bahasa ID/EN/JP/ES, transparansi joystick, joystick Fixed/Floating, volume, resolusi, FPS 30/60 (tersimpan otomatis).
- 10 hero (stat + skill unik, bisa dibuka dengan koin), 50 item (5 rarity: senjata 1-15, armor 16-30, helm 31-35, aksesoris 36-40, consumable 41-45, bahan 46-50), Equipment, Shop, Quests.
- Level 1 (Stage 1-5): 3 wave x 24 musuh (6 regu x 4: Stickman, Ninja, Tribal Mask), lalu boss Tribal Mask Chieftain (Ground Slam, Tribal Charge, panggil minion di 50% HP). Target ~9-10 menit.
- Arena (1v1 vs Shadow_Blade, 84 detik) dan Boss Raid (boss HP x2) memakai mesin pertarungan yang sama.
- Drop item dari musuh/boss, potion HP di battle (tombol HP / H).

## Kontrol
Keyboard: A/D gerak, J/Space serang, L skill hero, K dash, H potion. Sentuh: joystick kiri + tombol kanan.

## Menjalankan (JDK 17+)
Gradle wrapper belum disertakan. Buat proyek dari template Korge (korge.org), ganti `src/` dan `build.gradle.kts`, lalu:
  ./gradlew runJvm        (desktop)
  ./gradlew installDebug  (Android)
Versi Korge: 4.0.10 (package `korlibs.*`). Jika versi lain, sesuaikan import.

## Catatan jujur
- Kode BELUM pernah dikompilasi. Kirim log error jika ada, akan diperbaiki di v3.
- Visual berupa stickman prosedural (bentuk dasar + animasi + aura + partikel), bukan sprite final.
- Volume/Resolusi tersimpan tapi belum ada file audio/ efek resolusi. Font default tidak mendukung huruf Jepang, jadi JP memakai romaji.
- Guild, Events, Battle Pass (hadiah tier), Mail masih UI placeholder.
