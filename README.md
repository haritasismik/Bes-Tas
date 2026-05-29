# 🪨 Beş Taş

Geleneksel Türk çocuk oyunu "Beş Taş"ın Android mobil oyun versiyonu.

## 🎮 Oyun Modları

- **🤖 Yapay Zekaya Karşı** - 3 zorluk seviyesi (Kolay, Orta, Zor)
- **👥 Yerel İki Kişi** - Aynı telefonda sırayla
- **🌐 Online Oyna** - İnternetten rastgele eşleşme

## ✨ Özellikler

- Gerçekçi grafikler (ahşap zemin, 3D taşlar)
- İki taş stili: 🪨 Gerçekçi Taş ve 🥜 Fıstık
- Online liderboard / sıralama sistemi
- Akıcı animasyonlar
- Firebase altyapısı (Auth + Realtime DB)

## 🏗️ Teknik Yapı

- **Dil:** Kotlin
- **UI:** Jetpack Compose + Canvas
- **Mimari:** MVVM
- **Backend:** Firebase (Auth, Firestore, Realtime Database)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## 📋 Oyun Kuralları

1. **Birler** - Bir taşı havaya at, yerdeki taşlardan birini al, havadaki taşı yakala
2. **İkiler** - İkişer ikişer topla
3. **Üçler** - Üç + bir olarak topla
4. **Dörtler** - Dördünü birden topla
5. **Köprü** - İki taş arasından geçirme

## 🚀 Kurulum

1. Bu repoyu klonla
2. Android Studio'da aç
3. Firebase projesini oluştur ve `google-services.json` dosyasını `app/` dizinine ekle
4. Build ve çalıştır

## 📦 Build

```bash
./gradlew assembleRelease
```

## 📄 Lisans

MIT License
