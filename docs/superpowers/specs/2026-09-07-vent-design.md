# Vent — Wind & Wave Forecasting App Design

**Status:** Approved (design in sections, in-chat)
**Date:** 2026-09-07
**Type:** Architectural — new native Android app

A native Android wind & wave forecasting app (Kotlin + Jetpack Compose + Material 3 Expressive) for sailors and fishermen on the Mediterranean coast. Visual/UX inspiration: Breezy Weather (F-Droid) — minimal Material You cards, generous whitespace, smooth motion, zero ads/tracking.

## Core Principle

**Wind is the primary metric everywhere** — always shown first and largest, above waves/temperature/other data.

---

## Key Decisions (from clarifying Q&A)

| Decision | Choice |
|----------|--------|
| Minimum SDK | **API 36 (Android 16)** |
| Map library | **MapLibre GL** |
| Fishing meter | **Plain-language conditions card** (not a 0–100 score) |
| Tides | **Included but background/secondary** (WorldTides free tier behind interface) |
| Locations | **Fully dynamic**, GPS-first + search |
| Offline | **Standard cache** (forecast for saved locations + downloaded tiles) |
| DI | **Koin** |
| Networking | **Ktor Client** |
| Background updates | **WorkManager periodic** |
| Module structure | **Single module**, unified weather repository |
| Charts | **Vico** |

---

## Architecture

Single-activity, single-module Compose app. MVVM, Kotlin Coroutines/Flow, Koin DI, Room for offline caching, Ktor Client for networking.

### Layers (within one module)

- **UI** — Compose screens, Glance widgets, Material 3 (dynamic color).
- **ViewModels** — per-destination, exposed via Koin (`viewModel { }`), observing Flows from the repository.
- **Domain** — unified `WeatherPoint` model + plain-language generators (day brief, fishing conditions).
- **Data** — `WeatherRepository` facade + per-datatype provider interfaces; Room; Ktor client; MapLibre offline store.

### Domain model

A single unified model all screens consume, normalizing per-source response shapes:

```
WeatherPoint(
  lat, lon, time,
  wind: { speed, gust, direction(deg), headingName(NE/SW/…) },
  wave: { height, period, direction },
  temp, precipitation, pressure,
  cloudCover, sunrise, sunset, moonrise, moonset, moonPhase,
  source: { wind, wave, temp, tide }  // which provider each came from
)
```

### Source-provider abstraction

Enables per-datatype source selection in Settings and a fallback chain:

```
interface WindProvider { suspend fun wind(lat, lon, time): WindData }
interface WaveProvider { suspend fun wave(lat, lon, time): WaveData }
interface TideProvider { suspend fun tide(lat, lon, time): TideData }
```

### Data sources

- **Open-Meteo (primary)** — Ktor client, two endpoints:
  - Weather API: `current`, `hourly` (wind speed/gust/direction, temp, precip, pressure, cloud cover), `daily` (sunrise/sunset, `moon_phase`).
  - Marine API: `wave_height`, `wave_period`, `wave_direction` at `sea` for the same lat/lon.
- **GFS fallback (optional slot)** — the provider interface makes room; Open-Meteo serves GFS via `model=gfs_seamless`. Interface + stub only in v1 unless requested.
- **Tides (background)** — WorldTides API (free tier, key) behind `TideProvider`. Background/secondary data point: tide times/heights for shore-fishing notes + moon phase cross-check. Optional in Settings; disabling is a one-tap toggle.

### Room cache (one DB)

- `locations` — saved spots, order, isDefault.
- `weather_cache` — `WeatherPoint` per location+time, JSON-serialized hourly arrays.
- `map_offline_packs` — metadata only; tile bytes go to MapLibre's native offline store.
- `threshold_alerts` — user-created rules (e.g. wind > 25kt, waves > 1.5m).

---

## Navigation & Screens

**Bottom nav (4 tabs):** Now · Marine · Map · Settings.

### Now (Home)
Scrolling column:
1. **Hero card** — current wind: huge speed number + units, direction name + compass icon, gusts smaller. Tap opens Compass section.
2. **Plain-language day brief** — one line, e.g. *"Moderate NE wind easing by afternoon, calm seas."*
3. **Fishing conditions card** — plain-text explainer (not a meter): describes conditions, how they affect fishing, ideal fishing method for the weather (e.g. trolling / bottom fishing / shore casting).
4. **Nowcast** — next-hour wind/gust mini-trend for go/no-go decisions.
5. **Ephemeris card** — sunrise/sunset, moonrise/moonset, moon phase.
6. **Hourly strip** — horizontally scrollable 24h chips.

### Marine (Compass + Warnings + Forecast — one scrollable page)
- **Pinned warnings banner** at the top whenever alerts are active (visually distinct, banner/notification style, not buried in a card).
- **Scroll-spying segmented control** (Compass · Warnings · Forecast) to jump between sections.
1. **Compass section** — a large (near-full-width) custom Canvas section within the Marine page (not its own navigation route): drawn compass rose + live smoothly-animated needle pointing *into* the wind (no snapping). Speed + gusts centered. Haptic tick at each cardinal point as the needle passes. The Home hero wind card links here by scrolling the Marine page to this section.
2. **Warnings section** — official marine/severe alerts (where a source provides them, behind an interface) + user threshold alerts. Banner/notification style, visually distinct from cards.
3. **Forecast section** — 7-day list (wind, gust, wave h/p/dir, temp, precip); per-day detail view with hourly breakdown; 24h + 7-day wind/gust and wave-height line charts (Vico).

### Map
MapLibre GL with a **wind layer** (animated arrows/barbs) over OSM tiles. Toggle to overlay general weather (cloud/precip). Tap a point for a mini-forecast popup. Offline tile downloads for a selected area before heading out (no-signal use at sea).

### Settings
Breezy-style sub-menus:
- **Units** — wind (knots/km/h/m/s/mph), wave (m/ft), temp (C/F), pressure (hPa/inHg/mmHg), distance (km/nm/mi).
- **Data sources** — per-datatype picker (wind/wave/tide source), each backed by provider interfaces.
- **Locations** — add via GPS/search, reorder (drag), set default, delete.
- **Background update** — frequency (Off / 30m / 1h / 3h / 6h) via WorkManager.
- **Notifications** — master toggle; daily brief; severe wind/wave threshold alerts (independently toggleable).
- **Theme** — light / dark / system + dynamic-color toggle.
- **Widgets** — link to the widget picker.
- **Language** — in-app locale selection (persisted, defaults to system).
- **Haptics** — master toggle gating all haptics.

---

## Widgets & Wearables

**Glance widgets** (Jetpack Compose for widgets), refreshed from Room-cached data via WorkManager periodic + `updateAppWidget`:
1. **Wind glance widget** — current wind speed, direction (arrow), gusts (small/medium).
2. **Compass + wind widget** — mini compass rose with wind direction (medium/large).
3. **Daily brief widget** — the plain-language one-liner (large).

**Wearables/other-apps exposure** — a **`ContentProvider`** (`content://com.vent/wind`) exposing current wind (speed, units, direction, gust, timestamp) via a documented `WindContract`; permission-light, queryable by watch-face complications and third-party apps without bundling notifications. Optional `BroadcastReceiver` for push-style updates.

---

## Design Language (Material 3 Expressive)

- `MaterialTheme` with `dynamicLightColorScheme` / `dynamicDarkColorScheme`, custom sail/spirit-blue-tinted fallback when dynamic color is off.
- **Wind is hero** — largest type, always above waves/temp.
- M3 type scale: `displayLarge/Medium` for current wind speed, `headlineMedium` for direction, `titleMedium` for labels, `bodyMedium` for detail.
- 4dp spacing grid; M3 `Shapes` (rounded cards; expressive radius on hero card where it earns its place).
- Edge-to-edge with `WindowInsets.safeDrawing` handled.
- Smooth motion — animated compass needle (no snapping), crossfade transitions. Respect reduced-motion setting.

---

## Background Updates & Notifications

**WorkManager periodic** for forecast refresh; expedited work for warning-critical pushes.

**Notifications** (API 36 NotificationManager):
1. **Daily brief** — scheduled morning summary: today's wind + brief + fishing note.
2. **Threshold alerts** — when a forecast for a saved location crosses a user rule (wind > threshold, waves > threshold), fire a high-importance notification. Rules in Room; checked each periodic sync. Distinct channel from the brief.
3. **Severe official alerts** — where a source provides them, surfaced via the same channel banner + notification, region-scoped.

---

## Accessibility

- **TalkBack**: `contentDescription` on all images/icons; wind hero number + direction exposed as semantic heading with combined spoken unit ("12 knots from the northeast, gusts 18"). Fishing card is pure text → reads naturally.
- **Scalable text**: dimensions from `MaterialTheme.typography`, layout uses `LocalDensity`; `autoSize` for the hero wind number so it fits very large font scales.
- **Locale-aware units/formatting**: units driven by Settings + `java.util.Locale`.
- **Touch targets**: minimum 48dp wherever tappable.
- **Contrast**: M3 tonal roles; verify in light + dark.
- **Reduced motion**: ease off compass needle + map arrows to instant under reduced motion.

---

## Haptics

Throughout, not just taps — gated behind a Settings toggle (API 36 `VibratorManager` / `HapticFeedbackConstants`):
- Compass passing cardinal points.
- Fishing conditions reveal.
- Pull-to-refresh.
- Warning triggers.
- Threshold sliders in Settings.

---

## Error Handling

- Provider failure → fallback chain (Open-Meteo → GFS slot) → cached weather → degrade gracefully with a clear "data may be stale" indicator rather than a blank screen.
- Network loss at sea → serve from Room cache; show last-updated timestamp + offline banner.
- No location permission / no search result → explicit empty states with guidance.

---

## Testing

- Unit: plain-language generators (day brief, fishing conditions) with fixed inputs; provider parsing; threshold-alert evaluation.
- ViewModel: repository fakes driving state.
- UI: Compose tests for screens (hero order, nav, warnings banner), screenshot/visual tests for the compass + hero card.
- Data: Room migration + cache hit/miss tests; Open-Meteo parsing against recorded fixtures.

---

## Implementation Order (incremental)

1. **Scaffold** — project, Gradle, theme (dynamic color), Koin graph, navigation shell, insets, base UI kit.
2. **Now (Home)** — hero, day brief, fishing card, nowcast, ephemeris, hourly strip.
3. **Marine** — compass, warnings banner + section, forecast 7-day + per-day + charts.
4. **Map** — MapLibre wind layer, weather overlay, popup, offline tiles.
5. **Settings** — units, sources, locations, updates, notifications, theme, language, haptics.
6. **Widgets + ContentProvider** — Glance widgets, wind exposure.
7. **Background sync + notifications + haptics polish.**

Check in with the user after each screen before moving to the next.
