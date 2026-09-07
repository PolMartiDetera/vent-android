# Vent (Wind & Wave Forecast) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Vent, a native Android wind & wave forecasting app for Mediterranean sailors/fishermen, with wind-first UI, compass, map, forecast, warnings, settings, widgets, and wearables exposure.

**Architecture:** Single-module, single-activity Jetpack Compose app. MVVM + Kotlin Coroutines/Flow. Koin DI. Ktor Client networking against Open-Meteo (weather + marine). Room offline cache. MapLibre GL for the map. Glance widgets. WorkManager background sync. Wind exposed via ContentProvider.

**Tech Stack:** Kotlin 2.4.0, AGP 9.4.0, Gradle 9.7.1, Compose BOM 2026.08.00, Material 3, Navigation Compose 2.10.0, Koin 4.2.2, Ktor 3.5.2, Room 2.8.4, Glance 1.2.0, WorkManager 2.11.2, DataStore 1.2.1, Vico 3.3.1, MapLibre 13.6.0, kotlinx-serialization 1.11.0, coroutines 1.11.0, min SDK 36, target 36.

**Spec:** `docs/superpowers/specs/2026-09-07-vent-design.md`

## Global Constraints

- **minSdk = 36** (Android 16), compileSdk = 36, targetSdk = 36.
- **Wind is primary** everywhere: shown first, largest, above waves/temp.
- **Material 3**: always use `MaterialTheme.colorScheme/typography/shapes` tokens — never hardcode colors/fonts/spacing (4dp grid).
- **Dynamic color**: `dynamicLightColorScheme`/`dynamicDarkColorScheme` on API 31+ (all devices here since min 36), fallback palette handled in theme file.
- **No ads, no tracking**, no analytics.
- **All data sources free/open**: Open-Meteo (primary), WorldTides (optional, behind interface), OSM tiles.
- **Haptics** gated behind a Settings toggle; used on compass cardinal tick, fishing reveal, pull-to-refresh, warnings, threshold sliders.
- **Valid Kotlin everywhere**: named args use `=` not `:`; `by remember`/`var` vs `val` correct; every `contentDescription`/`onClick`/`modifier` correct.
- **Accessibility**: contentDescriptions, 48dp touch targets, scalable text, locale-aware units.
- Bottom nav = **4 tabs: Now · Marine · Map · Settings**.
- Marine page = one scrollable screen: pinned warnings banner, scroll-spying chips (Compass/Warnings/Forecast), then Compass section, Warnings section, Forecast section.
- Package: `com.vent.app`.

---

### Task 1: Project Scaffold — Gradle, Theme, Application, MainActivity

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (+ adaptive foreground/background)
- Create: `app/src/main/java/com/vent/app/VentApplication.kt`
- Create: `app/src/main/java/com/vent/app/MainActivity.kt`
- Create: `app/src/main/java/com/vent/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/vent/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/vent/app/ui/theme/Type.kt`

**Interfaces:**
- Produces: `VentApplication` (Application subclass with Koin), `MainActivity` (single activity), `VentTheme` composable, `AppNavHost`.

- [ ] **Step 1: Write root Gradle files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Vent"
include(":app")
```

`build.gradle.kts` (root):
```kotlin
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.4.0-2.0.2" apply false
}
```

`gradle.properties`:
```
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.configuration-cache=true
```

- [ ] **Step 2: Write `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.vent.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.vent.app"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("io.insert-koin:koin-androidx-compose:4.2.2")
    implementation("io.insert-koin:koin-androidx-viewmodel:4.2.2")

    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-okhttp:3.5.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")

    implementation("com.patrykandpatrick.vico:compose-m3:3.3.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 3: Manifest, resources**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".VentApplication"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.Vent"
        android:supportsRtl="true"
        android:allowBackup="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Vent">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`res/values/strings.xml`: `app_name` = `Vent`.
`res/values/themes.xml`: `Theme.Vent` parent `android:Theme.Material.Light.NoActionBar` (launch theme; Compose draws the real theme).

- [ ] **Step 4: Theme files**

`Color.kt`: `val Spindrift = Color(0xFF1B6A8A)` etc. — a sail/spirit-blue-tinted fallback palette (primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, surface, background...).

`Type.kt`: default `Typography()`.

`Theme.kt`:
```kotlin
@Composable
fun VentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

- [ ] **Step 5: Application + MainActivity**

`VentApplication.kt`:
```kotlin
class VentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VentApplication)
            modules(appModule)
        }
    }
}
```

`MainActivity.kt`: single `ComponentActivity`, `enableEdgeToEdge()`, `setContent { VentTheme { AppNavHost() } }`. The `AppNavHost` composable is introduced later (Task 8); for this task provide a placeholder `AppNavHost()` in `ui/navigation/AppNavHost.kt` showing a `Scaffold` with an empty bottom nav of 4 placeholder items and `Text` placeholders for each screen.

- [ ] **Step 6: Verify the scaffold builds**

Run: `gradle :app:assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "chore: scaffold Vent Android project (Gradle, theme, Koin, single activity)"
```

---

### Task 2: Data Layer — Domain Model, Open-Meteo DTOs, Providers

**Files:**
- Create: `app/src/main/java/com/vent/app/data/model/WeatherPoint.kt`
- Create: `app/src/main/java/com/vent/app/data/model/WindData.kt`, `WaveData.kt`, `TideData.kt`, `ForecastHour.kt`, `ForecastDay.kt`
- Create: `app/src/main/java/com/vent/app/data/remote/OpenMeteoClient.kt`
- Create: `app/src/main/java/com/vent/app/data/remote/dto/OpenMeteoDto.kt`
- Create: `app/src/main/java/com/vent/app/data/provider/WindProvider.kt`, `WaveProvider.kt`, `TideProvider.kt` (interfaces)
- Create: `app/src/main/java/com/vent/app/data/provider/OpenMeteoWindProvider.kt`, `OpenMeteoWaveProvider.kt`
- Test: `app/src/test/java/com/vent/app/data/remote/OpenMeteoParsingTest.kt`

**Interfaces:**
- Produces: domain models `WeatherPoint`, `WindData`, `WaveData`, `TideData`, `ForecastHour`, `ForecastDay`; provider interfaces `WindProvider`, `WaveProvider`, `TideProvider`; `OpenMeteoWindProvider(lat, lon): OpenMeteoWeather` with fields `windSpeed10m`, `windDirection10m`, `windGusts10m`, ... per-hour + current.

- [ ] **Step 1: Define domain models (data classes, in `model/`)**

```kotlin
data class WindData(val speedKn: Double, val gustKn: Double, val directionDeg: Int, val cardinal: String)
data class WaveData(val heightM: Double, val periodSec: Double, val directionDeg: Int)
data class TideData(val highTideAt: Long?, val lowTideAt: Long?, val heightM: Double)
data class ForecastHour(val time: Long, val windSpeedKn: Double, val windGustKn: Double,
    val windDirDeg: Int, val waveHeightM: Double, val wavePeriodSec: Double,
    val tempC: Double, val precipMm: Double, val pressureHpa: Double, val cloudCoverPct: Int)
data class ForecastDay(val date: Long, val windMaxKn: Double, val gustMaxKn: Double,
    val waveMaxM: Double, val wavePeriodSec: Double, val tempMinC: Double, val tempMaxC: Double,
    val precipMm: Double, val windDirDeg: Int, val hours: List<ForecastHour>)
data class WeatherPoint(val lat: Double, val lon: Double, val time: Long,
    val wind: WindData, val wave: WaveData?, val tempC: Double, val pressureHpa: Double,
    val cloudCoverPct: Int, val sunrise: Long, val sunset: Long,
    val moonrise: Long, val moonset: Long, val moonPhase: Double,
    val day: ForecastDay?, val nextHours: List<ForecastHour>)
```

Unit helpers in `model/Units.kt`: `cardsToKnots`, `metersToKn` (open-meteo marine wave speed → kn), etc. Keep constants: `KNOT_CONVERSIONS` for Settings later.

- [ ] **Step 2: Open-Meteo DTOs + Ktor client**

`dto/OpenMeteoDto.kt` (kotlinx-serialization):
```kotlin
@Serializable data class OmCurrent(val time: String, val temperature_2m: Double, @SerialName("wind_speed_10m") val windSpeed: Double, @SerialName("wind_gusts_10m") val windGusts: Double, @SerialName("wind_direction_10m") val windDirection: Int, val pressure_msl: Double, val cloud_cover: Int)
@Serializable data class OmHourly(val time: List<String>, @SerialName("wind_speed_10m") val windSpeed: List<Double>, @SerialName("wind_gusts_10m") val windGusts: List<Double>, @SerialName("wind_direction_10m") val windDirection: List<Int>, val temperature_2m: List<Double>, val precipitation: List<Double>, val pressure_msl: List<Double>, val cloud_cover: List<Int>)
@Serializable data class OmDaily(val time: List<String>, @SerialName("sunrise") val sunrise: List<String>, @SerialName("sunset") val sunset: List<String>, @SerialName("moonrise") val moonrise: List<String>, @SerialName("moonset") val moonset: List<String>, @SerialName("moon_phase") val moonPhase: List<Double>, @SerialName("wind_speed_10m_max") val windMax: List<Double>, @SerialName("wind_gusts_10m_max") val gustMax: List<Double>, @SerialName("temperature_2m_max") val tempMax: List<Double>, @SerialName("temperature_2m_min") val tempMin: List<Double>, val precipitation_sum: List<Double>)
@Serializable data class OmWeather(@SerialName("current") val current: OmCurrent, @SerialName("hourly") val hourly: OmHourly, @SerialName("daily") val daily: OmDaily)
@Serializable data class OmWaveHourly(val time: List<String>, @SerialName("wave_height") val waveHeight: List<Double>, @SerialName("wave_period") val wavePeriod: List<Double>, @SerialName("wave_direction") val waveDirection: List<Int>)
@Serializable data class OmWave(@SerialName("hourly") val hourly: OmWaveHourly)
```

`OpenMeteoClient.kt`:
```kotlin
class OpenMeteoClient(httpClient: HttpClient) {
    val weather = httpClient
    suspend fun weather(lat: Double, lon: Double): OmWeather = weather.get("https://api.open-meteo.com/v1/forecast") {
        parameter("latitude", lat); parameter("longitude", lon)
        parameter("current", "temperature_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,pressure_msl,cloud_cover")
        parameter("hourly", "temperature_2m,wind_speed_10m,wind_gusts_10m,wind_direction_10m,precipitation,pressure_msl,cloud_cover")
        parameter("daily", "sunrise,sunset,moonrise,moonset,moon_phase,wind_speed_10m_max,wind_gusts_10m_max,temperature_2m_max,temperature_2m_min,precipitation_sum")
        parameter("timezone", "auto")
    }.body()
    suspend fun wave(lat: Double, lon: Double): OmWave = ...
}
```
(Ktor winds are in km/h; wave_height is m, wave_period is s.) Use a helper to convert km/h → knots (`/1.852`).

- [ ] **Step 3: Provider interfaces + Open-Meteo implementations**

```kotlin
interface WindProvider { suspend fun wind(lat: Double, lon: Double, time: Long): List<ForecastHour> }
interface WaveProvider { suspend fun wave(lat: Double, lon: Double, time: Long): List<ForecastHour> }
interface TideProvider { suspend fun tide(lat: Double, lon: Double): TideData }
```

`OpenMeteoWindProvider(client: OpenMeteoClient)` implements `WindProvider`:
parses `OmWeather.hourly` into `List<ForecastHour>` (time epoch, km/h→knots, cardinal via `WindRose.cardinal(deg)`). `OpenMeteoWaveProvider` parses `OmWave.hourly` into wave-bearing `ForecastHour`s.

Wind cardinals in `data/model/WindRose.kt`: `fun cardinal(deg: Int): String` returning 16-point names (N, NNE, NE, ...).

- [ ] **Step 4: Write + run parsing tests**

`OpenMeteoParsingTest.kt` uses a fixed `OmWeather` JSON string with `Json { ignoreUnknownKeys = true }.decodeFromString<OmWeather>(fixture)`; assert `current.windSpeed` value, hourly length, and that `WindRose.cardinal(22)` == `"NNE"`. Run `gradle :app:testDebugUnitTest --console=plain`. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add domain models, Open-Meteo client, and wind/wave/tide providers"
```

---

### Task 3: Persistence — Room Database + DataStore Settings + Repositories

**Files:**
- Create: `app/src/main/java/com/vent/app/data/db/VentDatabase.kt`
- Create: `app/src/main/java/com/vent/app/data/db/dao/LocationDao.kt`, `WeatherCacheDao.kt`, `ThresholdAlertDao.kt`
- Create: `app/src/main/java/com/vent/app/data/db/entity/LocationEntity.kt`, `WeatherCacheEntity.kt`, `ThresholdAlertEntity.kt`
- Create: `app/src/main/java/com/vent/app/data/repo/WeatherRepository.kt`
- Create: `app/src/main/java/com/vent/app/data/repo/LocationRepository.kt`
- Create: `app/src/main/java/com/vent/app/data/repo/SettingsRepository.kt`
- Create: `app/src/main/java/com/vent/app/data/settings/UserSettings.kt` + `SettingsDataStore.kt`
- Test: `app/src/test/java/com/vent/app/data/repo/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces: `VentDatabase`, `weatherCacheDao`, `locationDao`, `thresholdAlertDao`; `WeatherRepository.weatherFor(lat, lon, cached: Boolean): Flow<WeatherPoint>`; `LocationRepository.locations: Flow<List<LocationEntity>>`, `save`, `delete`, `reorder`, `setDefault`; `SettingsRepository.settings: Flow<UserSettings>`, `update(transform)`; `UserSettings` data class (units, sources, updateFreq, notifications, theme, dynamicColor, haptics, language).
- Consumes: providers from Task 2.

- [ ] **Step 1: Entities + DAOs + database**

Entities with Room annotations. Cache: `id = lat_lon_time`, store serialized full JSON of a `WeatherPoint` (use a `TypeConverter` via kotlinx-serialization to `String`). `WeatherCacheEntity(lat, lon, time, json, updatedAt)`.

- [ ] **Step 2: Settings DataStore**

`UserSettings`:
```kotlin
data class UserSettings(
  val windUnit: WindUnit = WindUnit.KNOTS, val waveUnit: WaveUnit = WaveUnit.METERS,
  val tempUnit: TempUnit = TempUnit.CELSIUS, val updateFreqMin: Int = 60,
  val windSource: String = "openmeteo", val waveSource: String = "openmeteo", val tideSource: String = "none",
  val dailyBrief: Boolean = true, val thresholdAlerts: Boolean = true,
  val darkTheme: Boolean = false, val useSystemTheme: Boolean = true, val dynamicColor: Boolean = true,
  val haptics: Boolean = true, val language: String = "system", val locationId: Long? = null
)
```
Enums `WindUnit { KNOTS, KMH, MS, MPH }`, etc. `SettingsDataStore` exposes `settings: Flow<UserSettings>` synced to DataStore preferences.

- [ ] **Step 3: Repositories**

`WeatherRepository(client, windProvider, waveProvider, tideProvider, weatherCacheDao)`:
- `weatherFor(lat, lon): Flow<WeatherPoint>` — fetch from providers, assemble `WeatherPoint`, cache JSON; on network error fall back to DB cache row.
- `cached(lat, lon): WeatherPoint?` — read DB.

`LocationRepository(locationDao)`: CRUD + reorder + default.
`SettingsRepository(settingsDataStore)`: `settings` flow + `update`.

- [ ] **Step 4: Write + pass SettingsRepositoryTest** (bare: default settings returned when no pref).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add Room persistence, DataStore settings, and repositories"
```

---

### Task 4: Plain-Language Generators (Day Brief + Fishing Conditions)

**Files:**
- Create: `app/src/main/java/com/vent/app/domain/brief/DayBriefGenerator.kt`
- Create: `app/src/main/java/com/vent/app/domain/brief/FishingConditionsGenerator.kt`
- Test: `app/src/test/java/com/vent/app/domain/brief/BriefGeneratorsTest.kt`

**Interfaces:**
- Produces: `DayBriefGenerator.brief(day: ForecastDay, wind: WindData): String`; `FishingConditionsGenerator.conditions(day, wind, wave, pressureTrend, moonPhase, tide): FishingConditions` where `FishingConditions(summary: String, effects: String, method: String)`.

- [ ] **Step 1: DayBriefGenerator**

Take the day's prevailing wind + max gust + wave max + pressure trend; emit a single natural-language line. e.g. "Moderate NE wind easing by afternoon, calm seas." Use Beaufort-style descriptors from a lookup table keyed on mean wind knots.

- [ ] **Step 2: FishingConditionsGenerator**

Plain text (NOT a score). Inputs: wind, wave height, pressure trend (computed from first/last hourly), moon phase, optional tide. Output a `FishingConditions` with:
- `summary`: conditions in plain words
- `effects`: how they affect fishing (wind + wave + light/moisture)
- `method`: suggested technique chosen by rules: heavy wind + chop → shore casting / bottom fishing; calm + rising pressure → trolling, etc.

- [ ] **Step 3: Unit tests with fixed inputs** (calm day → "trolling"; choppy + strong wind → "shore casting"); assert strings contain expected keywords. Run `gradle :app:testDebugUnitTest`. PASS.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: add plain-language day brief and fishing conditions generators"
```

---

### Task 5: Threshold Alert Engine + Notifications

**Files:**
- Create: `app/src/main/java/com/vent/app/domain/alerts/ThresholdAlertEngine.kt`
- Create: `app/src/main/java/com/vent/app/data/notify/NotificationHelper.kt`
- Create: `app/src/main/java/com/vent/app/data/notify/NotificationChannels.kt`
- Test: `app/src/test/java/com/vent/app/domain/alerts/ThresholdAlertEngineTest.kt`

**Interfaces:**
- Produces: `ThresholdAlertEngine.evaluate(rules: List<ThresholdAlertEntity>, point: WeatherPoint): List<ThresholdAlertResult>`; `NotificationHelper.postThreshold(id, alert)`, `postDailyBrief(text)`; `NotificationChannels.createChannels(context)` (channels: "daily_brief", "threshold", "severe").

- [ ] **Step 1: Engine** — iterate rules (wind>X kn, wave>X m, gust>X kn), compare against point forecasts over the next 24h, return triggered results with severity.
- [ ] **Step 2: NotificationHelper** uses API 36 NotificationManager; distinct channels.
- [ ] **Step 3: Unit tests** — a rule that crosses returns triggered; a rule that never crosses returns empty. PASS.
- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: add threshold alert engine and notification helper"
```

---

### Task 6: Koin DI Module (appModule)

**Files:**
- Create: `app/src/main/java/com/vent/app/di/AppModule.kt`
- Modify: `app/src/main/java/com/vent/app/VentApplication.kt` (already references `appModule`)

**Interfaces:**
- Produces: Koin `module` named `appModule` wiring: `single { OpenMeteoClient(httpClient) }`, `single<WindProvider> { OpenMeteoWindProvider(get()) }`, `single<WaveProvider> { OpenMeteoWaveProvider(get()) }`, `single<TideProvider> { NoTideProvider() }` (default, returns empty), `single { VentDatabase }`, daos, `single { WeatherRepository(...) }`, `single { LocationRepository(...) }`, `single { SettingsRepository(...) }`, `single { NotificationHelper(context) }`, `viewModel { NowViewModel(...) }`, etc. (ViewModels added per-screen — add them in their tasks; keep `appModule` growing as screens land).

- [ ] **Step 1: Write AppModule.kt** with a `KtorClient` factory (ContentNegotiation + kotlinx-json).
- [ ] **Step 2: Verify** `gradle :app:compileDebugKotlin --console=plain` compiles (module may reference ViewModels created in later tasks — to keep it compiling, only reference what exists now; add ViewModel wiring in each screen task).
- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add Koin DI module"
```

---

### Task 7: Now (Home) Screen

**Files:**
- Create: `app/src/main/java/com/vent/app/ui/now/NowViewModel.kt`
- Create: `app/src/main/java/com/vent/app/ui/now/NowScreen.kt`
- Create: `app/src/main/java/com/vent/app/ui/components/WeatherCards.kt` (HeroCard, BriefCard, FishingCard, EphemerisCard, NowcastCard, HourlyStrip)
- Create: `app/src/main/java/com/vent/app/ui/now/CompassThumbnail.kt`

**Interfaces:**
- Produces: `NowScreen(onOpenCompass: () -> Unit)`, `NowViewModel` (state: `NowUiState(loading, weather: WeatherPoint?, brief: String, fishing: FishingConditions?, error)`, methods `refresh()`, `nowcast()`, `load()`), `HeroCard`, `FishingCard`, `BriefCard`, `EphemerisCard`, `NowcastCard`, `HourlyStrip`.
- Consumes: `WeatherRepository`, `LocationRepository`, `SettingsRepository`, `DayBriefGenerator`, `FishingConditionsGenerator`.

- [ ] **Step 1: NowViewModel** — `class NowViewModel(repo, locationRepo, settingsRepo, briefGen, fishingGen, alertEngine)` exposing `StateFlow<NowUiState>`; refreshes on location + settings change.
- [ ] **Step 2: NowScreen** — `LazyColumn` with: `HeroCard` (wind-first: huge `displayMedium` speed, unit, cardinal + arrow, gusts subtext; tap → `onOpenCompass`), `BriefCard`, `FishingCard` (plain text, haptic on first reveal), `NowcastCard` (next-hour trend), `EphemerisCard`, `HourlyStrip`. Edge-to-edge insets via `contentPadding` from `WindowInsets.safeDrawing`.
- [ ] **Step 3: Compose UI test** — NowScreen shows wind speed text before wave/temp (assert hero is first in semantics order). `gradle :app:connectedDebugAndroidTest` (emulator) OR keep as instrumented-only note.
- [ ] **Step 4: Wire `viewModel { NowViewModel(...) }` in AppModule.**
- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add Now (Home) screen with wind-first hero and weather cards"
```

---

### Task 8: Navigation Shell (AppNavHost) — 4 tabs

**Files:**
- Create: `app/src/main/java/com/vent/app/ui/navigation/AppNavHost.kt`
- Create: `app/src/main/java/com/vent/app/ui/navigation/Destinations.kt`
- Modify: `MainActivity.kt` to call real `AppNavHost()`

**Interfaces:**
- Produces: `AppNavHost()`, sealed `Destination(route, label, icon)`: Now, Marine, Map, Settings; a `Material3 NavigationBar` with 4 items; `NavHost` with routes. Home Hero tap → navigate to Marine route (scroll to compass — pass a nav arg `focus`).
- Consumes: `NowScreen`, and placeholders `MarineScreen`, `MapScreen`, `SettingsScreen` (added in later tasks; create stubs now).

- [ ] **Step 1: Destinations.kt** sealed class with 4 tabs.
- [ ] **Step 2: AppNavHost.kt** — `Scaffold(bottomBar = { NavigationBar { ... } }) { NavHost(...) }`; `NowScreen` wired; other three routes render simple placeholder `Text`.
- [ ] **Step 3: Replace MainActivity placeholder with AppNavHost().**
- [ ] **Step 4: Build** `gradle :app:assembleDebug`. PASS.
- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add 4-tab navigation shell"
```

---

### Task 9: Marine Screen — Compass + Warnings + Forecast

**Files:**
- Create: `app/src/main/java/com/vent/app/ui/marine/MarineViewModel.kt`
- Create: `app/src/main/java/com/vent/app/ui/marine/MarineScreen.kt`
- Create: `app/src/main/java/com/vent/app/ui/marine/CompassRose.kt`
- Create: `app/src/main/java/com/vent/app/ui/marine/WarningsSection.kt`
- Create: `app/src/main/java/com/vent/app/ui/marine/ForecastSection.kt`
- Create: `app/src/main/java/com/vent/app/ui/marine/WindCharts.kt`
- Create: `app/src/main/java/com/vent/app/haptic/Haptic.kt`

**Interfaces:**
- Produces: `MarineScreen(focus: String?)` (focus = "compass"/"warnings"/"forecast" triggers auto-scroll), `MarineViewModel`, `CompassRose(windDirDeg, speedKn, gustKn, modifier)`, `WarningsSection(alerts)`, `ForecastSection(day: List<ForecastDay>, onDayClick)`, `WindCharts(day, week)`.
- Consumes: `WeatherRepository`, `LocationRepository`, `ThresholdAlertEngine`, `NotificationHelper`, haptic controller.

**Haptics.** `com/vent/app/haptic/Haptic.kt`:
```kotlin
class HapticController(context: Context) {
  private val vibrator = context.getSystemService(Vibrator::class.java)
  fun cardinalTick() { gated by settings; vibrator.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE)) }
  fun reveal() { /* longer pattern */ }
  fun warning() { /* distinctive pattern */ }
  fun refresh() { /* short */ }
}
```
Inject settings flow; no-op when haptics off.

- [ ] **Step 1: HapticController** + Koin `single`.
- [ ] **Step 2: CompassRose** — `Canvas` draws rose (360 tick marks, N/E/S/W labels), an animated needle via `Animatable` rotating to `windDirDeg` from current (smooth, spring; no snapping); haptic tick as needle passes each cardinal. Center shows `speedKn` + units and gust. Reduced-motion: jump instantly.
- [ ] **Step 3: MarineViewModel** — composes compass wind, warnings (official + threshold via engine + daily), 7-day + hourly forecast, 24h & 7-day series.
- [ ] **Step 4: MarineScreen** — pinned warnings banner at top (if alerts, `Material3` `Card` in error/tertiary tonal, visually distinct; tap opens Warnings section), then a scroll-spying `ScrollableTabRow`/segmented chips for Compass/Warnings/Forecast (using `LazyListState` firstVisibleItemIndex). Sections.
- [ ] **Step 5: WarningsSection** — list of alerts, banner-style.
- [ ] **Step 6: ForecastSection + WindCharts** — 7-day list rows (wind/gust/wave/temp/precip), per-day detail via a sub-`Dialog`/detail panel with hourly breakdown; Vico line chart for 24h wind+gust and wave height, plus 7-day.
- [ ] **Step 7: wire `viewModel { MarineViewModel(...) }` in AppModule; replace Marine placeholder.**
- [ ] **Step 8: Build** `gradle :app:assembleDebug`. PASS. (Manual QA on emulator for compass animation.)
- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "feat: add Marine screen (compass, warnings, forecast, charts)"
```

---

### Task 10: Map Screen (MapLibre)

**Files:**
- Create: `app/src/main/java/com/vent/app/ui/map/MapScreen.kt`
- Create: `app/src/main/java/com/vent/app/ui/map/MapViewModel.kt`
- Create: `app/src/main/java/com/vent/app/ui/map/WindLayer.kt`
- Create: `app/src/main/java/com/vent/app/data/map/OfflineTiles.kt`
- Create: `app/src/main/java/com/vent/app/data/map/MapOfflineDao.kt` + entity + DB migration
- Modify: `app/build.gradle.kts` — add MapLibre dependency; location permission handling.

**Interfaces:**
- Produces: `MapScreen(onRequestPerm)`, `MapViewModel`, `WindLayer(map: MapLibreMap, points)` draws wind arrow/barbs; `OfflineTiles.download(styleUrl, bounds, minZ, maxZ)` and `delete(packId)`.
- Consumes: `WeatherRepository`, `LocationRepository`.

- [ ] **Step 1: Add MapLibre dependency** `org.maplibre.gl:android-sdk:13.6.0` (may need `mavenCentral()`; verify. If 13.6.0 unavailable on Central, adjust to closest stable).
- [ ] **Step 2: Offline tiles** — `OfflineTiles` wraps MapLibre `OfflineManager`/`RegionDownloadState`. `MapOfflineDao` records pack metadata (id, name, bounds, status) in Room.
- [ ] **Step 3: MapScreen** — `AndroidView` hosting a `MapView` (OSM raster via a style URL or OSM tile source), wind layer overlay rendering animated arrows from `WeatherRepository` points, toggle for cloud/precip overlay, tap → mini-forecast popup (`AlertDialog`/popup with current + next hours), offline download UI (bounds selector + progress).
- [ ] **Step 4: MapViewModel** — holds map-center weather query + tile pack state.
- [ ] **Step 5: Wire in AppModule + replace Map placeholder.**
- [ ] **Step 6: Build** `gradle :app:assembleDebug`. PASS. (MapLibre requires a device; emulator has GPU. Verify it at least compiles and the Activity doesn't crash on `MapView` create.)
- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: add Map screen with wind layer and offline tiles"
```

---

### Task 11: Settings Screen

**Files:**
- Create: `app/src/main/java/com/vent/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/vent/app/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/vent/app/ui/settings/UnitSelection.kt`

**Interfaces:**
- Produces: `SettingsScreen()`, `SettingsViewModel(settingsRepo, locationRepo)` state mirroring `UserSettings`; navigation sections.
- Consumes: `SettingsRepository`, `LocationRepository`.

- [ ] **Step 1: SettingsViewModel**.
- [ ] **Step 2: SettingsScreen** — sections: Units (wind/wave/temp/pressure/distance), Data sources (wind/wave/tide pickers), Locations (list, add via search/GPS stub + GPS, reorder drag + haptic, default, delete), Background update (frequency radio, haptic on change), Notifications (daily brief, threshold alerts toggles), Theme (light/dark/system + dynamic color), Widgets (launch `getAppWidgetManager` picker intent), Language (in-app locale persisted), Haptics (master toggle).
- [ ] **Step 3: Wire in AppModule + replace Settings placeholder.**
- [ ] **Step 4: Build** PASS.
- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: add Settings screen"
```

---

### Task 12: Background Sync + Widgets + ContentProvider

**Files:**
- Create: `app/src/main/java/com/vent/app/data/sync/WeatherSyncWorker.kt`
- Create: `app/src/main/java/com/vent/app/data/sync/SyncScheduler.kt`
- Create: `app/src/main/java/com/vent/app/widget/WindGlanceWidget.kt`
- Create: `app/src/main/java/com/vent/app/widget/CompassGlanceWidget.kt`
- Create: `app/src/main/java/com/vent/app/widget/BriefGlanceWidget.kt`
- Create: `app/src/main/java/com/vent/app/data/expose/WindContentProvider.kt` + `WindContract.kt`
- Modify: `AndroidManifest.xml` (provider, receiver, service declarations)
- Modify: `gradle` (glance already added).

**Interfaces:**
- Produces: `WeatherSyncWorker.doWork()` (refresh default location, run alert engine, post notifications, update widgets); `SyncScheduler.schedule(context, freqMin)`; 3 `GlanceAppWidget` subclasses; `WindContentProvider` exposing `content://com.vent.app.wind/wind` with columns `speed, unit, direction, gust, timestamp`.

- [ ] **Step 1: WeatherSyncWorker** — `CoroutineWorker`; on success `Result.success()`; uses `WeatherRepository` for default location; triggers threshold alerts; updates Glance widgets.
- [ ] **Step 2: SyncScheduler** — `PeriodicWorkRequest` minimum 15min; rescheduled on settings change.
- [ ] **Step 3: Three Glance widgets** — `glanceAppWidget()` composables reading cached weather (via `provideGlance`/repository injected), `GlanceAppWidgetReceiver` registered in manifest with metadata for resize/description; render wind speed/dir/gust (Wind), compass rose+wind (Compass), daily brief (Brief).
- [ ] **Step 4: WindContentProvider + WindContract** — `query()` returns matrix cursor; documented schema; `permission` optional (public read). Register in manifest.
- [ ] **Step 5: Init** — `Application.onCreate` calls `SyncScheduler.schedule` with settings freq; workers request `POST_NOTIFICATIONS` at appropriate time.
- [ ] **Step 6: Build** PASS.
- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: add background sync, Glance widgets, and wind ContentProvider"
```

---

### Task 13: Accessibility + Haptics Pass + Full Verify

**Files:**
- Modify: per-file content descriptions, `Typography`/autoSize for hero number, 48dp targets, reduced-motion handling, haptics on all trigger points (compass cardinal, fishing reveal, pull-to-refresh, warnings, threshold sliders), locale formatting in Units.
- Test: `app/src/test/...` — a formatting test for units (knots vs m/s vs km/h output).

- [ ] **Step 1: Auditing pass** per the Android Design Guide checklist (theme tokens, contrast, insets, reachable targets, TalkBack text).
- [ ] **Step 2: Unit formatting test** for `Units.format(knots, WindUnit.KNOTS)` == `"12 kn"` etc. PASS.
- [ ] **Step 3: Full build** `gradle :app:assembleDebug :app:testDebugUnitTest --console=plain`. All should pass.
- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: accessibility and haptics polish"
```

---

## Self-Review Notes

- **Spec coverage:** All spec sections map to tasks — scaffold (1), data (2), persistence (3), plain-language (4), alerts/notify (5), DI (6), Now (7), nav (8), Marine/compass/warnings/forecast/charts/haptics (9), Map/offline (10), Settings (11), widgets+provider+sync (12), a11y+haptics (13). Widgets + ContentProvider + WorkManager + haptics all present.
- **Type consistency:** `WeatherPoint`, `ForecastHour`, `ForecastDay`, `WindData`, `WaveData`, `TideData`, `WindProvider/WaveProvider/TideProvider`, `WeatherRepository.weatherFor`, `SettingsRepository.settings/update`, `UserSettings`, `DayBriefGenerator.brief`, `FishingConditionsGenerator.conditions`, `ThresholdAlertEngine.evaluate`, `NotificationHelper`, `HapticController`, `AppNavHost`, `NowScreen`, `MarineScreen`, `MapScreen`, `SettingsScreen` — names reused consistently across tasks.
- **Dependency check:** AGP 9.4.0 + Gradle 9.7.1 + Kotlin 2.4.0 + KSP (KSP version must match Kotlin — verify `2.4.0-2.0.2` at build time; adjust if resolver complains). Compose BOM 2026.08.00. Glance 1.2.0, Vico 3.3.1, MapLibre 13.6.0 (verify artifact exists on Central; the plan flags this in Task 10).
