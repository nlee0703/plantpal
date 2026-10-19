# PlantPal – Smart Plant Care Reminder

COMP3074 Mobile Application Development I, Final Project **prototype**
Nicholas Lee · 101303788 · Group 73

PlantPal is an Android app that keeps track of when each houseplant or balcony plant needs water.
It works out each plant's next watering date, flags plants that are due or overdue, and uses live
weather from [Open-Meteo](https://open-meteo.com/) to tell you when rain means outdoor plants can
skip watering.

## What works in this prototype

| Screen | What you can do | Requirements |
|---|---|---|
| **My Plants** | Weather banner, filter chips (All / Due today / Indoor / Outdoor), search by name, plant cards with status badges, one-tap **Water**, + Add plant | FR3, FR5, FR10 |
| **Add / Edit Plant** | Name (required, validated), species, Indoor/Outdoor toggle, watering interval stepper (1–60 days), last-watered date picker, notes, photo from camera or gallery | FR1, FR2, FR4 |
| **Plant Detail** | Photo header, next-watering card, **Mark as watered**, care history (newest first), Edit, Delete with confirmation | FR4, FR5, FR8 |
| **Weather Tips** | Current conditions, 5-day forecast with rainy days highlighted, a tip for each outdoor plant, **Log rain skip**, offline mode using the last saved forecast | FR7 |
| **Settings** | City search (Open-Meteo Geocoding), reminder time, notification switch (asks for permission on Android 13+), rain-aware tips switch, rain threshold | FR9 |

On first launch the app adds five sample plants so every status can be shown right away.
The default city is Toronto.

**Planned for the final build:** the daily WorkManager notification (FR6). Its settings are
already saved in this prototype.

## Architecture

* **Single activity + Navigation Component.** `MainActivity` hosts a `NavHostFragment`. My Plants,
  Weather Tips and Settings are top-level destinations in the bottom navigation bar.
* **MVVM.** Each screen is a Fragment (view binding) backed by a ViewModel that exposes
  `StateFlow`s. The Fragments only render state and forward clicks.
* **Room** (`plants` and `care_logs` tables, cascade delete) behind `PlantRepository`.
* **SharedPreferences** (`SettingsStore`) for settings and the cached forecast.
* **Open-Meteo** forecast and geocoding APIs (`WeatherRepository`) using `HttpURLConnection` and
  coroutines. No API key is needed.

```
app/src/main/java/ca/gbc/comp3074/nicholas/plantpal/
├── MainActivity.kt, PlantPalApp.kt
├── data/      Plant, CareLog, DAOs, AppDatabase, PlantRepository, SettingsStore
├── weather/   WeatherRepository, Forecast models, weather-code mapping
├── ui/        plants/, edit/, detail/, weather/, settings/  (Fragment + ViewModel each)
└── util/      DateUtils, PlantStatus, PhotoStorage
```

## Build & run

1. Open the folder in Android Studio and let Gradle sync.
   It uses AGP 9.4.1, Gradle 9.6 and KSP for Room.
2. Run on an emulator or phone with API 24 or higher. Weather needs an internet connection.
3. Unit tests: `./gradlew test`
