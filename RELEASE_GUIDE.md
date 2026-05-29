# 🚀 Beş Taş - Play Store Yayın Rehberi

## 1. Ön Hazırlık

### Keystore Oluşturma (Bir kez yapılır)
```bash
keytool -genkey -v \
  -keystore app/bestas-release.keystore \
  -alias bestas \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass SENIN_SIFREN \
  -keypass SENIN_SIFREN \
  -dname "CN=Harita Sismik, OU=Development, O=Harita Sismik, L=Istanbul, ST=Istanbul, C=TR"
```

⚠️ **ÖNEMLİ:** Keystore dosyasını GIT'e EKLEME! `.gitignore`'da zaten var.

### Firebase Kurulumu
1. [Firebase Console](https://console.firebase.google.com) → Yeni proje oluştur → "bes-tas"
2. Android uygulaması ekle → Package: `com.haritasismik.bestas`
3. `google-services.json` dosyasını indir → `app/` klasörüne koy
4. Firebase Authentication → Google Sign-In aktif et
5. Firestore Database oluştur
6. Realtime Database oluştur

### Ortam Değişkenleri
```bash
export KEYSTORE_PASSWORD=senin_sifren
export KEY_ALIAS=bestas
export KEY_PASSWORD=senin_sifren
```

## 2. Build

### Debug Build
```bash
./gradlew assembleDebug
# Çıktı: app/build/outputs/apk/debug/app-debug.apk
```

### Release APK
```bash
./gradlew assembleRelease
# Çıktı: app/build/outputs/apk/release/app-release.apk
```

### App Bundle (Play Store için önerilen)
```bash
./gradlew bundleRelease
# Çıktı: app/build/outputs/bundle/release/app-release.aab
```

## 3. Play Store Bilgileri

### Uygulama Bilgileri
| Alan | Değer |
|------|-------|
| Uygulama Adı | Beş Taş |
| Paket Adı | com.haritasismik.bestas |
| Kategori | Oyun > Bulmaca |
| İçerik Derecelendirme | Herkes (E) |
| Varsayılan Dil | Türkçe |

### Açıklama (Kısa)
```
Geleneksel Türk oyunu Beş Taş! AI'a karşı oyna, arkadaşınla oyna veya online rakiplerle yarış!
```

### Açıklama (Uzun)
```
Beş Taş, nesillerden nesillere aktarılan geleneksel Türk çocuk oyununun dijital versiyonu!

🎮 OYUN MODLARI:
• Yapay Zekaya Karşı - 3 zorluk seviyesi
• Yerel İki Kişi - Aynı telefonda arkadaşınla
• Online - Dünya genelinden rakiplerle eşleş

✨ ÖZELLİKLER:
• Gerçekçi grafikler ve akıcı animasyonlar
• İki farklı taş stili: Gerçekçi Taş ve Fıstık
• Online liderboard ve sıralama sistemi
• Ses efektleri ve arka plan müziği

📋 KURALLAR:
Birler → İkiler → Üçler → Dörtler → Köprü
Tüm turları tamamlayan kazanır!

Hadi, çocukluğunun eğlencesini yeniden keşfet! 🪨
```

### Ekran Görüntüleri (Gerekli)
- En az 2 adet telefon ekran görüntüsü
- Önerilen: Ana menü, oyun ekranı, online eşleşme, sıralama
- Boyut: 1080x1920 px (Portrait)

### Simge
- Yüksek çözünürlüklü simge: 512x512 px
- Öne çıkan grafik: 1024x500 px

## 4. Versiyon Yönetimi

`app/build.gradle.kts` dosyasında:
```kotlin
versionCode = 1      // Her güncelleme için +1 artır
versionName = "1.0.0" // Semantic versioning (major.minor.patch)
```

### Versiyon Geçmişi:
| Versiyon | Kod | Tarih | Notlar |
|----------|-----|-------|--------|
| 1.0.0 | 1 | - | İlk yayın |

## 5. Kontrol Listesi (Yayın Öncesi)

- [ ] Keystore oluşturuldu ve güvenli yerde saklandı
- [ ] google-services.json eklendi
- [ ] Firebase kuralları (security rules) ayarlandı
- [ ] Gerçek ses dosyaları (.ogg) eklendi
- [ ] Uygulama simgesi oluşturuldu
- [ ] Ekran görüntüleri alındı
- [ ] App Bundle başarıyla oluşturuldu
- [ ] Release APK test cihazında denendi
- [ ] Tüm oyun modları test edildi
- [ ] Privacy Policy URL hazırlandı
- [ ] Google Play Console hesabı oluşturuldu (25$ tek seferlik)
- [ ] İçerik derecelendirme anketi dolduruldu

## 6. Firebase Security Rules

### Firestore Rules:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /leaderboard/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /players/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Realtime Database Rules:
```json
{
  "rules": {
    "rooms": {
      "$roomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "waiting_queue": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

## 7. Sonraki Güncellemeler İçin Fikirler

- [ ] Farklı taş stilleri (mermer, kristal, şeker)
- [ ] Turnuva modu
- [ ] Arkadaş listesi ve davet sistemi
- [ ] Başarımlar (achievements)
- [ ] Günlük görevler
- [ ] Tema marketplace
- [ ] Tablet desteği
- [ ] Çoklu dil desteği (İngilizce, Arapça)
