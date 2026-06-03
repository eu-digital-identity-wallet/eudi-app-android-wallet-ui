# Theming and branding guide

This guide explains how an integrator rebrands and rethemes the reference application — changing
colors, fonts, shapes, logos, the launcher icon, the app name, and the splash screen — and how to
verify the result.

It expands the short [Theme configuration](CONFIGURATION.md#theme-configuration) summary in the main
configuration document.

## Table of contents

* [Architecture overview](#architecture-overview)
* [Quick-start rebrand checklist](#quick-start-rebrand-checklist)
* [Colors](#colors)
* [Typography and fonts](#typography-and-fonts)
* [Shapes](#shapes)
* [Dimensions and spacing](#dimensions-and-spacing)
* [Logos and in-app imagery](#logos-and-in-app-imagery)
* [Launcher icon](#launcher-icon)
* [App name and package identity](#app-name-and-package-identity)
* [Brand strings](#brand-strings)
* [Splash screen](#splash-screen)
* [Sub-SDK theming (RQES)](#sub-sdk-theming-rqes)
* [Advanced: runtime and white-label theming](#advanced-runtime-and-white-label-theming)
* [Verifying your rebrand](#verifying-your-rebrand)
* [Production and accessibility checklist](#production-and-accessibility-checklist)

## Architecture overview

Branding is split across **three layers** — make sure you address all three when rebranding. The
[Theme configuration](CONFIGURATION.md#theme-configuration) summary in the configuration document
links here for the full detail.

| Layer | What it covers | Where it lives |
| --- | --- | --- |
| 1. Compose theme | Colors, typography, fonts, shapes, dimensions | `resources-logic` module, package `eu.europa.ec.resourceslogic.theme` |
| 2. Branding assets | In-app logos, launcher icon, app display name, package id, brand strings, splash screen, system bars, host XML theme | `resources-logic` resources + `assembly-logic` + `app` + `build-logic` |
| 3. Sub-SDK theming | The RQES signing UI carries its own theme and translations | `business-logic` (`RQESConfigImpl`) |

### The Compose theme: `ThemeManager`

Every themeable aspect follows a **template + values** pattern:

* a **template** data class describes the *shape* of the data
  (e.g. [`ThemeColorsTemplate`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/templates/ThemeColorsTemplate.kt));
* a **values** object holds the *actual* reference values
  (e.g. [`ThemeColors`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeColors.kt)).

They are assembled by
[`ThemeManager`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/ThemeManager.kt)
and applied as a `MaterialTheme` in
[`EudiComponentActivity`](../ui-logic/src/main/java/eu/europa/ec/uilogic/container/EudiComponentActivity.kt):

```kotlin
ThemeManager.instance.Theme {
    // app content
}
```

> **Important — how the theme is actually initialized.**
> The application **never calls `ThemeManager.Builder()` itself.** It uses the lazily-created
> `ThemeManager.instance`, and that getter builds the theme from the hardcoded reference values
> (`ThemeColors.lightColors`, `ThemeColors.darkColors`, `ThemeTypography.typo`, `ThemeShapes.shapes`
> and a fixed `ThemeDimensTemplate`).
>
> So for a normal rebrand you **edit the `values/*.kt` files** described below. The `Builder` API is
> only needed for advanced/white-label scenarios — see
> [Advanced: runtime and white-label theming](#advanced-runtime-and-white-label-theming).

### Light, dark, and dynamic color

* The theme defines **separate light and dark palettes**. Dark mode follows the system setting
  (`isSystemInDarkTheme()`). Always rebrand **both** palettes.
* **Dynamic color (Material You) is disabled by default** (`disableDynamicTheming = true` in
  `ThemeManager.Theme`). Your brand colors are always used; the device wallpaper does not override
  them. Leave it disabled unless you explicitly want Android 12+ dynamic theming.

### Shared vs. per-flavor

The project ships two build flavors, `dev` and `demo` (see
[`AppFlavor`](../build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt)).

* Compose theme values and in-app logos live in **`resources-logic/src/main`** → shared by all
  flavors. Change once.
* The **launcher icon** is defined **per flavor** (`resources-logic/src/demo` and
  `resources-logic/src/dev`). Change it for every flavor you ship.

## Quick-start rebrand checklist

The minimum set of files to change for a full rebrand. Each row links to its detailed section below.

| Brand element | File(s) to change | Notes |
| --- | --- | --- |
| Brand colors (light) | [`theme/values/ThemeColors.kt`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeColors.kt) — `lightColors` + light constants | 48 Material 3 roles. See [Colors](#colors). |
| Brand colors (dark) | same file — `darkColors` + dark constants | Don't forget dark mode. |
| Extra semantic colors | same file — `success`, `warning`, `pending`, `divider` | Not part of Material 3. |
| Fonts | [`res/font/`](../resources-logic/src/main/res/font) + [`theme/values/ThemeTypography.kt`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeTypography.kt) | See [Typography and fonts](#typography-and-fonts). |
| Type scale | [`theme/values/ThemeTypography.kt`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeTypography.kt) | 15 Material 3 text styles. |
| Corner shapes | [`theme/values/ThemeShapes.kt`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeShapes.kt) | See [Shapes](#shapes). |
| In-app logos | [`res/drawable/ic_logo_full.xml`](../resources-logic/src/main/res/drawable/ic_logo_full.xml), `ic_logo_plain.xml`, `ic_logo_text.xml` | Brand colors are baked into the vectors. See [Logos](#logos-and-in-app-imagery). |
| Launcher icon | `resources-logic/src/demo/res/mipmap-*` **and** `resources-logic/src/dev/res/mipmap-*` (foreground per flavor) + shared [`ic_launcher_background.xml`](../resources-logic/src/main/res/values/ic_launcher_background.xml) | Icon bitmaps per flavor; background color is shared. See [Launcher icon](#launcher-icon). |
| App display name | [`assembly-logic/build.gradle.kts`](../assembly-logic/build.gradle.kts) (`appName`) + [`AppFlavor.kt`](../build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt) (suffix) | See [App name](#app-name-and-package-identity). |
| Package / application id | `app/build.gradle.kts` (`applicationId`) + `AppFlavor.kt` (`.dev` suffix) | Don't change after public release. |
| Splash (OS window) | [`res/values-v31/themes.xml`](../resources-logic/src/main/res/values-v31/themes.xml) | See [Splash screen](#splash-screen). |
| Brand strings (product name in UI) | [`res/values/strings.xml`](../resources-logic/src/main/res/values/strings.xml) (+ localized variants) | Search for `EUDI`/`Wallet`. See [Brand strings](#brand-strings). |
| System bars (status/navigation) | [`MainActivity.kt`](../assembly-logic/src/main/java/eu/europa/ec/assemblylogic/ui/MainActivity.kt) (`enableEdgeToEdge`) | Edge-to-edge — **not** the XML theme. See [Window chrome](#window-chrome-and-system-bars). |
| Host XML theme | [`res/values/themes.xml`](../resources-logic/src/main/res/values/themes.xml) (`Theme.EUDIWallet`) | Cold-start window background only (Compose app, no action bar). |
| RQES signing UI | Optional `themeManager` override in [`RQESConfigImpl`](../business-logic/src/demo/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt) (+ `dev`) | Separate SDK theme; **not** overridden by default. See [Sub-SDK theming](#sub-sdk-theming-rqes). |

## Colors

### Where colors are defined

* **Template:**
  [`ThemeColorsTemplate`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/templates/ThemeColorsTemplate.kt)
  declares the **48 Material 3 color roles** (`primary`, `onPrimary`, `surface`, `error`, the
  `*Fixed` roles, etc.) as `Long` ARGB values, and converts them to a Compose `ColorScheme` via
  `toColorScheme()`.
* **Values:**
  [`ThemeColors`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeColors.kt)
  holds the EUDI reference palette as private `0xAARRGGBB` constants and assembles them into two
  `ThemeColorsTemplate` instances: `lightColors` and `darkColors`.

### How to change a brand color

Edit the constant for the role in both palettes. For example, to change the primary brand color:

```kotlin
// resources-logic/.../theme/values/ThemeColors.kt
private const val eudiw_theme_light_primary: Long = 0xFF2A5FD9 // -> your light brand color
private const val eudiw_theme_dark_primary: Long  = 0xFFB4C5FF // -> your dark brand color
```

The colors use the **`0xAARRGGBB`** format (alpha first). Always include the alpha byte — a value
such as `0xF1D192B` has only 7 hex digits instead of 8, so it silently becomes a near-transparent
color (alpha `0x0F`).

### Extra semantic colors

Beyond the standard Material 3 roles, the theme defines four extra roles used across the UI:
`success`, `warning`, `pending`, and `divider`. They are exposed as `ColorScheme` extension
properties so you can use them like any Material role:

```kotlin
val ok = MaterialTheme.colorScheme.success
```

Each has a light and a dark value in `ThemeColors.kt` (e.g. `eudiw_theme_light_success` /
`eudiw_theme_dark_success`). Rebrand these too, or status indicators will keep the reference colors.

## Typography and fonts

### Replacing the font family

1. Drop your font files into
   [`resources-logic/src/main/res/font/`](../resources-logic/src/main/res/font) (the project
   currently ships `roboto_regular.ttf`, `roboto_medium.ttf`, `roboto_light.ttf`).
2. Declare a `ThemeFont` for each weight/style in
   [`ThemeTypography.kt`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeTypography.kt):

   ```kotlin
   internal val BrandRegular = ThemeFont(
       res = R.font.brand_regular,
       weight = ThemeFontWeight.W400,
       style = ThemeFontStyle.Normal,
   )
   ```

3. Reference your `ThemeFont` from the relevant text styles (see below). If a `fontFamily` is left
   `null`/empty, the style falls back to `FontFamily.Default` (the system font).

### The type scale

[`ThemeTypography.typo`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeTypography.kt)
builds a
[`ThemeTypographyTemplate`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/templates/ThemeTypographyTemplate.kt)
covering all 15 Material 3 styles (`displayLarge` … `labelSmall`). Each style is a `ThemeTextStyle`
where you can set `fontFamily`, `fontSize`, `fontWeight`, `fontStyle`, `letterSpacing`, `textAlign`,
and more:

```kotlin
titleMedium = ThemeTextStyle(
    fontFamily = listOf(BrandMedium),
    fontSize = 16,
    letterSpacing = 0.15f,
    textAlign = ThemeTextAlign.Start
),
```

## Shapes

Corner radii come from
[`ThemeShapes.shapes`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeShapes.kt),
which fills a
[`ThemeShapesTemplate`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/templates/ThemeShapesTemplate.kt)
with five sizes (`extraSmall` … `extraLarge`, in `dp`). The reference values are `16/16/16/32/32`:

```kotlin
const val EXTRA_SMALL = 16.0
const val SMALL = 16.0
const val MEDIUM = 16.0
const val LARGE = 32.0
const val EXTRA_LARGE = 32.0
```

The same file also exposes shape helpers used by components (`bottomCorneredShapeSmall`,
`topCorneredShapeSmall`, `allCorneredShapeSmall`, `allCorneredShapeLarge`). If you change the base
sizes, review those helpers too.

## Dimensions and spacing

The theme's dimension support is intentionally minimal, and this is easy to misjudge:

* The Compose theme carries a single value,
  [`ThemeDimensTemplate.screenPadding`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/templates/ThemeDimensTemplate.kt).
  It is stored on the theme set but, in the current code, is **not read anywhere** — no UI code
  consumes it, so changing it has no effect.
* The spacing and sizing actually used by the UI are compile-time constants in
  [`Constants.kt`](../ui-logic/src/main/java/eu/europa/ec/uilogic/component/utils/Constants.kt)
  (`SPACING_*`, `SIZE_*`, `ICON_SIZE_*`, …), and screen padding is computed in
  [`ScreenPadding.kt`](../ui-logic/src/main/java/eu/europa/ec/uilogic/component/utils/ScreenPadding.kt)
  from those constants.

**To adjust global spacing/sizing, edit `Constants.kt` (and `ScreenPadding.kt`)** rather than the
theme. Treat the theme's `screenPadding` as advisory only.

## Logos and in-app imagery

### The three brand logos

The wallet uses three vector logos in
[`resources-logic/src/main/res/drawable/`](../resources-logic/src/main/res/drawable):

| Drawable | `AppIcons` key | Where it appears |
| --- | --- | --- |
| `ic_logo_full.xml` | `AppIcons.LogoFull` | [Splash screen](../startup-feature/src/main/java/eu/europa/ec/startupfeature/ui/splash/SplashScreen.kt) |
| `ic_logo_plain.xml` | `AppIcons.LogoPlain` | Content header / [`AppIconAndText`](../ui-logic/src/main/java/eu/europa/ec/uilogic/component/AppIconAndText.kt) |
| `ic_logo_text.xml` | `AppIcons.LogoText` | Next to the plain logo in the header |

The mapping from key to drawable lives in
[`AppIcons.kt`](../ui-logic/src/main/java/eu/europa/ec/uilogic/component/AppIcons.kt).

### How to swap them

The simplest path is to **replace the contents of the three `ic_logo_*.xml`** drawables with your
own vectors (keep the file names so the `AppIcons` mapping and all call sites keep working).

> **Gotcha — logo colors are baked in.** The reference logos hardcode their brand colors inside the
> `<path android:fillColor="...">` entries (e.g. `#0048D2`). They do **not** follow the color theme.
> If you want a logo that adapts to light/dark or to `colorPrimary`, author a single-color vector and
> tint it (e.g. `android:tint="?attr/colorPrimary"` or `fillColor="@android:color/white"` combined
> with a Compose tint at the call site).

The `drawable/` folder also contains ~50 functional UI icons (`ic_*.xml`). These are generally not
brand-specific, but review them if your design system requires custom iconography.

## Launcher icon

The Android launcher icon is defined **per flavor** as an adaptive icon:

* Foreground/legacy bitmaps: `resources-logic/src/<flavor>/res/mipmap-*/ic_launcher*.webp` (per flavor)
* Adaptive icon definitions: `resources-logic/src/<flavor>/res/mipmap-anydpi*/ic_launcher.xml` and
  `ic_launcher_round.xml` (per flavor)
* Background color: [`res/values/ic_launcher_background.xml`](../resources-logic/src/main/res/values/ic_launcher_background.xml)
  — a single `#FFFFFF` color in `src/main`, **shared by all flavors**

Replace these for **every** flavor you ship (`demo` and `dev` by default). The easiest way to
generate a complete, correctly-sized set is **Android Studio → New → Image Asset (Launcher Icons)**,
targeting each flavor's `res` directory.

The manifest references them as `android:icon="@mipmap/ic_launcher"` and
`android:roundIcon="@mipmap/ic_launcher_round"` in
[`assembly-logic/src/main/AndroidManifest.xml`](../assembly-logic/src/main/AndroidManifest.xml).

## App name and package identity

### Display name

The app name shown under the icon is a manifest placeholder, **not** a string resource:

* Base name — [`assembly-logic/build.gradle.kts`](../assembly-logic/build.gradle.kts):

  ```kotlin
  manifestPlaceholders["appName"] = "EUDI Wallet"
  ```

* Per-flavor suffix —
  [`AppFlavor.kt`](../build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt)
  (`applicationNameSuffix`, empty by default for both flavors).

These combine in the manifest as `android:label="${appName}${appNameSuffix}"`. Change the base name
for your brand; keep suffixes (if any) for non-production builds only.

> For a **localized** app name, replace the placeholder with a string resource
> (`android:label="@string/app_name"`) and provide per-locale `app_name` values.

### Application id

The package / application id is set in `app/build.gradle.kts` (`applicationId`), with a `.dev`
suffix applied to the `dev` flavor via `AppFlavor.kt`. Use a reverse-DNS id you own, e.g.
`eu.example.wallet`. **Do not change it after public release** unless you intend to publish a
separate app. See the production table in
[CONFIGURATION.md](CONFIGURATION.md#production-configuration-reference).

## Brand strings

The product name appears in user-facing copy as well as the launcher label. These live in
[`strings.xml`](../resources-logic/src/main/res/values/strings.xml) (and any localized
`values-<lang>/strings.xml`), **not** in the theme. If you rename the product, search the string
resources for `EUDI`/`Wallet` and review at least:

* Onboarding/login copy — e.g. `quick_pin_create_title` ("Welcome to your Wallet") and
  `biometric_login_biometrics_enabled_subtitle` ("…access the EUDI Wallet.").
* The side-menu title `dashboard_side_menu_title` ("My EU Wallet").
* The NFC service description `nfc_engagement_service_desc`, shown by the OS in tap-to-pay/NFC
  settings (referenced from the manifest `<service android:label=...>`).

Keep the launcher label ([App name](#app-name-and-package-identity)) and these in-copy names
consistent.

## Splash screen

There are two distinct pieces:

1. **OS window splash (Android 12+)** — configured in
   [`res/values-v31/themes.xml`](../resources-logic/src/main/res/values-v31/themes.xml). The
   reference sets a transparent icon and `0ms` duration, effectively disabling the system splash
   animation:

   ```xml
   <item name="android:windowSplashScreenAnimatedIcon">@android:color/transparent</item>
   <item name="android:windowSplashScreenAnimationDuration">0</item>
   ```

   To show a branded system splash, point `windowSplashScreenAnimatedIcon` at your icon drawable and
   set `windowSplashScreenBackground` to your brand background color.

2. **In-app Compose splash** —
   [`SplashScreen.kt`](../startup-feature/src/main/java/eu/europa/ec/startupfeature/ui/splash/SplashScreen.kt)
   renders `AppIcons.LogoFull` (i.e. `ic_logo_full.xml`) on the theme `surface` color with a fade
   animation. Rebranding `ic_logo_full.xml` and your `surface` color updates it automatically.

### Window chrome and system bars

The host activity theme `Theme.EUDIWallet` — defined in
[`res/values/themes.xml`](../resources-logic/src/main/res/values/themes.xml) (via `Theme.Base`,
ultimately `Theme.MaterialComponents.DayNight.DarkActionBar`) — only governs the **cold-start window
background** shown before Compose draws. The app is Compose-only, so there is no platform action bar
to style.

The **status and navigation bars are edge-to-edge**:
[`MainActivity`](../assembly-logic/src/main/java/eu/europa/ec/assemblylogic/ui/MainActivity.kt) calls
`enableEdgeToEdge()`, which makes them transparent and auto-adapts their icon contrast to light/dark.
Setting `android:statusBarColor` on the XML theme has **no effect**. To brand the bars (e.g. apply a
scrim), pass a `SystemBarStyle` to `enableEdgeToEdge(...)` in `MainActivity`.

## Sub-SDK theming (RQES)

The remote qualified electronic signature (RQES) flow is provided by a separate UI SDK. It carries
its **own** theme and translations, configured on `EudiRQESUiConfig`:

```kotlin
interface EudiRQESUiConfig {
    // Optional. Default English translations are used if not set.
    val translations: Map<String, Map<LocalizableKey, String>>

    // Optional. The SDK's default theme is used if not set.
    val themeManager: ThemeManager
    // ...
}
```

The reference app leaves both `themeManager` and `translations` at their SDK defaults — the RQES
flow is **not** rethemed by the wallet. Note the consequences:

* The SDK ships its own copy of the EUDI reference palette, so out of the box the signing screens
  already match the unmodified wallet.
* Because the two themes are independent, **rebranding the wallet (e.g. editing `ThemeColors.kt`)
  does not restyle the RQES screens** — they keep the SDK default look.

If you want the signing flow to match your brand, override `themeManager` (and, if needed,
`translations`) in both `RQESConfigImpl` variants with values that mirror your wallet theme. The SDK
exposes its own `ThemeManager.Builder` (colors + typography) under
`eu.europa.ec.eudi.rqesui.infrastructure.theme`. This is optional and is intentionally left out of
the reference app.

See the RQES subsection under
[General configuration](CONFIGURATION.md#general-configuration) for where `RQESConfigImpl` is wired
per flavor.

## Advanced: runtime and white-label theming

For most rebrands you edit the reference `values/*.kt` files. If instead you need to keep those files
untouched (e.g. a white-label build that selects a theme at runtime, or themes fetched remotely), you
can build the static instance yourself with
[`ThemeManager.Builder`](../resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/ThemeManager.kt).

Call it **once at startup, before any UI is shown** (e.g. in `Application.onCreate()`), so it
replaces the lazily-built default instance:

```kotlin
ThemeManager.Builder()
    .withLightColors(myLightColors)   // ThemeColorsTemplate (required)
    .withDarkColors(myDarkColors)     // optional; falls back to light colors
    .withTypography(myTypography)     // ThemeTypographyTemplate (required)
    .withShapes(myShapes)             // ThemeShapesTemplate (required)
    .withDimensions(ThemeDimensTemplate(screenPadding = 10.0)) // required
    .build()
```

`build()` throws if `lightColors`, `typography`, `shapes`, or `dimensions` are missing; `darkColors`
is the only optional input. After `build()`, `ThemeManager.instance` returns your configured manager.

## Verifying your rebrand

* **Compose previews.** Most components have `@ThemeModePreviews` previews wrapped in
  [`PreviewTheme`](../ui-logic/src/main/java/eu/europa/ec/uilogic/component/preview/PreviewTheme.kt),
  which uses the real theme. Use Android Studio's preview pane to check colors/typography in light
  and dark without running the app.
* **Run both modes.** Launch the app and toggle the system dark mode; confirm both palettes look
  correct.
* **Run both flavors.** Build `dev` and `demo` and confirm the launcher icon is correct for each (the
  app name is shared across flavors by default).
* **Screenshot the key surfaces:** splash, dashboard/home, a presentation request, the PIN screen,
  document details, and the RQES signing flow.

## Production and accessibility checklist

* [ ] Light **and** dark palettes rebranded, including the extra `success`/`warning`/`pending`/`divider` roles.
* [ ] Text/background color pairs meet contrast guidance (WCAG AA: 4.5:1 for body text).
* [ ] All three `ic_logo_*` drawables replaced; no leftover EUDI logo paths/colors.
* [ ] Launcher icon replaced for every flavor; legible at small sizes and on the adaptive background.
* [ ] App display name set; localized if you ship multiple locales.
* [ ] Brand strings in `strings.xml` reviewed (product name in onboarding, side menu, NFC service label).
* [ ] `applicationId` set to an id you own and final before public release.
* [ ] System bars verified under edge-to-edge (icon contrast in light and dark).
* [ ] Splash (system + in-app) shows your brand.
* [ ] RQES signing flow reviewed — either matching your brand (if you chose to override its theme) or with the SDK default deemed acceptable.
* [ ] RTL verified (`android:supportsRtl="true"` is set).
* [ ] No demo/dev branding, trust anchors, or endpoints left in the production build — see
      [GO_LIVE.md](GO_LIVE.md).
