# 🎵 ترانیک — TarAnik

**پلیر موسیقی فارسی با ویرایش تگ، همگام‌سازی لیریک، ترجمه AI و خروجی زیرنویس**

---

## ✨ ویژگی‌ها

- 🎵 **پخش موسیقی** — با کنترل کامل (شافل، تکرار، سرعت پخش، صف پخش)
- 🏷️ **ویرایش تگ ID3** — خواندن و نوشتن تگ‌های واقعی فایل MP3
- 🧹 **تشخیص تگ کثیف** — پیدا کردن URL و HTML در تگ‌ها و پاک‌سازی خودکار
- 📝 **ویرایش لیریک** — پشتیبانی از متن ساده و فرمت LRC
- ⏱️ **همگام‌سازی لیریک** — ثبت زمان هر خط همزمان با پخش
- 🌐 **ترجمه AI** — ترجمه خط به خط لیریک با API
- 📤 **خروجی زیرنویس** — ۵ فرمت (SRT, LRC, ASS, VTT, TXT)
- 🎨 **تم تاریک/روشن** — پشتیبانی از تم سیستم
- 📁 **اسکن خودکار** — یافتن فولدرهای موسیقی از حافظه
- 🔔 **نوتیفیکیشن لیریک** — نمایش خط فعلی + ترجمه در نوتیفیکیشن
- 🔤 **فونت فارسی** — Vazirmatn
- 💾 **بکاپ/ریستور** تنظیمات

---

## 📱 نیازمندی‌ها

- Android 8.0 (API 26) یا بالاتر
- اجازه خواندن فایل‌های صوتی

---

## 🏗️ معماری

```
MVVM + Clean Architecture

┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│   UI Layer   │────▶│  ViewModel   │────▶│  Repository  │
│  (Compose)   │◀────│  (StateFlow) │◀────│  (Data)      │
└─────────────┘     └──────────────┘     └──────────────┘
                                                │
                                    ┌───────────┴───────────┐
                                    │                       │
                              ┌─────▼─────┐         ┌──────▼──────┐
                              │  Local    │         │  Remote    │
                              │ MediaStore│         │  Translate  │
                              │ Files     │         │  API       │
                              └───────────┘         └────────────┘
```

---

## 📂 ساختار پروژه

```
com.tranik.app/
├── TarAnikApp.kt                    ← @HiltAndroidApp
├── MainActivity.kt                  ← Permission + Navigation + Error Handler
├── data/
│   ├── model/Models.kt             ← Track, Folder, LyricLine, DirtyTag
│   ├── source/MediaStoreDataSource ← خواندن آهنگ‌ها از MediaStore
│   ├── repository/
│   │   ├── TrackRepository         ← کش + جستجو
│   │   ├── Id3TagRepository        ← خواندن/نوشتن ID3 واقعی
│   │   ├── LyricsRepository        ← خواندن/نوشتن LRC
│   │   └── SettingsRepository      ← DataStore پایدار
│   └── backup/BackupManager        ← بکاپ/ریستور
├── di/AppModule                     ← Hilt DI
├── service/PlayerService            ← Foreground Service + Notification + Lyrics
└── ui/
    ├── theme/ (Theme, Type, Color)
    ├── components/ (QueueBottomSheet)
    ├── screens/ (8 screens)
    └── viewmodel/ (8 viewmodels + BaseViewModel)
```

---

## 🛠️ تکنولوژی‌ها

| تکنولوژی | کاربرد |
|----------|--------|
| Kotlin | زبان اصلی |
| Jetpack Compose | UI |
| Hilt | Dependency Injection |
| Coroutines + Flow | Async + State |
| MediaStore | خواندن فایل‌های صوتی |
| mp3agic | ویرایش تگ ID3 |
| Coil | بارگذاری کاور آلبوم |
| DataStore | ذخیره تنظیمات |
| MyMemory API | ترجمه رایگان |
| Foreground Service | پخش پس‌زمینه |

---

## 🔧 بیلد

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# تست واحد
./gradlew test

# Android تست
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

---

## 📄 لایسنس

این پروژه خصوصی است و کپی یا توزیع آن بدون اجازه ممنوع می‌باشد.
