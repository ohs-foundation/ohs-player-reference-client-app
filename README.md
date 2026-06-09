# OHS Player Reference Client App

A Kotlin Multiplatform + Compose Multiplatform reference client for [Open Health Stack (OHS)](https://developers.google.com/open-health-stack/overview). Targets Android, iOS, JVM desktop, JS browser, and Wasm browser from a single Kotlin source tree.

## What's in this repo

The project is split into two Gradle modules.

**`:ohs-player-library`** — a small Compose Multiplatform framework for building data-driven screens. Defines a typed renderer registry that lets screens describe layouts by name (view-types) and resolve them at composition time, decoupling screen code from concrete UI components. Pulls in FHIR model + FHIRPath dependencies, intended for healthcare data models.

**`:ohs-player-reference-app`** — the consumer app. Built with Compose Multiplatform, targeting Android, iOS (arm64 + simulator arm64), JVM desktop, JS browser, and Wasm browser. Implements a patient list and patient detail feature on top of the library as a worked example.

Application ID and root package: `dev.ohs.player.reference.app`.

## Architecture: the registry pattern

The library is built around three types that map onto three roles:

- **`ComponentRenderer<T, C>`** — the renderer you author. Renders a single item of data type `T` using a config of type `C`.
- **`ConfiguredRenderer<T>`** — the bound, callable form of a renderer with the config already applied via closure. This is what scaffolds and layouts invoke; it doesn't carry `C`.
- **`ViewRegistry`** — a mutable map keyed by `(ViewType, KClass<T>)`. Populated once at app start, then made available to the composition via `LocalViewRegistry` so screens can look renderers up by name.

The compositional flow when a screen renders:

```
buildAppViewRegistry()             populate the registry
CompositionLocalProvider           install at composition root
ListScaffold / DetailScaffold      look up renderers by ViewType
LayoutRenderer.Render              arrange items
ConfiguredRenderer.Render          paint each item
```

A single `ComponentRenderer` instance can be registered under multiple view-types with different configs, e.g. one card class used for both list items and a detail header.

## Library usage

### 1. Define view-types

View-types are opaque labels for visual roles. Declare them once as constants. See [`AppViewTypes.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewTypes.kt).

```kotlin
object AppViewTypes {
    val Card = ViewType("Card")
    val PatientHeader = ViewType("PatientHeader")
    val VerticalList = ViewType("VerticalList")
}
```

### 2. Author a renderer

Implement `ComponentRenderer<T, C>` and write the Compose body. See [`PatientCardRenderer.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientCardRenderer.kt) for the real version.

```kotlin
data class PatientCardConfig(val showLastVisit: Boolean = true)

class PatientCardRenderer : ComponentRenderer<PatientView, PatientCardConfig> {
    @Composable
    override fun Render(
        item: PatientView,
        config: PatientCardConfig,
        onClick: (() -> Unit)?,
        modifier: Modifier,
    ) {
        Card(onClick = onClick, modifier = modifier) {
            Text(item.fullName)
            if (config.showLastVisit) Text("Last visit: ${item.lastVisitDate}")
        }
    }
}
```

### 3. Build the registry

Group registrations for a feature into a `*Registrations.kt` extension on `ViewRegistry`, then assemble them in a single builder. See [`PatientListRegistrations.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListRegistrations.kt) and [`AppViewRegistry.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewRegistry.kt).

```kotlin
fun ViewRegistry.registerPatientList() {
    registerComponent<PatientView, PatientCardConfig>(
        AppViewTypes.Card, PatientCardRenderer(), PatientCardConfig(),
    )
    registerLayout<PatientView>(
        AppViewTypes.VerticalList,
        VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
}

fun buildAppViewRegistry(): ViewRegistry = ViewRegistry().apply {
    registerPatientList()
    registerPatientProfile()
}
```

### 4. Install the registry

Provide the registry at the top of the composition. See [`App.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/App.kt).

```kotlin
@Composable
fun App() {
    val registry = remember { buildAppViewRegistry() }
    CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
            // NavHost, screens, etc.
        }
    }
}
```

### 5. Render a list

Use `ListScaffold` and refer to the registered view-types in the DSL. See [`PatientListScreen.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListScreen.kt).

```kotlin
@Composable
fun PatientListScreen(onPatientClick: (String) -> Unit) {
    val viewModel: PatientListViewModel = viewModel { PatientListViewModel() }
    val patients by viewModel.patients.collectAsStateWithLifecycle()

    ListScaffold<PatientView>(
        items = patients,
        onItemClick = { onPatientClick(it.id) },
        key = { it.id },
    ) {
        component(AppViewTypes.Card)
        layout(AppViewTypes.VerticalList)
        topBar { TopAppBar(title = { Text("Patients") }) }
        emptyState { Text("No patients") }
    }
}
```

An empty `items` list short-circuits to `emptyState` without invoking the layout renderer. If you omit `layout(...)`, the scaffold falls back to `VerticalListRenderer`.

### 6. Render a detail view

Use `DetailScaffold` for a stack of sections rendering one item. See [`PatientProfileScreen.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/profile/PatientProfileScreen.kt).

```kotlin
@Composable
fun PatientProfileScreen(patientId: String, onBack: () -> Unit) {
    val viewModel = remember(patientId) { PatientProfileViewModel(patientId) }
    val patient by viewModel.patient.collectAsStateWithLifecycle()

    DetailScaffold<PatientView>(item = patient) {
        topBar { TopAppBar(title = { Text(patient?.fullName.orEmpty()) }) }
        notFound { Text("Patient not found") }
        section(AppViewTypes.PatientHeader)
        section(AppViewTypes.PersonalSection)
        section(AppViewTypes.MedicalSection)
        section(AppViewTypes.ContactSection)
    }
}
```

Sections render vertically in declared order. A null `item` renders `notFound` and skips sections entirely. Sections are read-only — their `onClick` is a no-op.

### 7. Reuse one renderer under multiple roles

The same renderer class can be registered under different view-types with different configs:

```kotlin
fun ViewRegistry.registerPatientProfile() {
    registerComponent(
        AppViewTypes.PatientHeader,
        PatientCardRenderer(),
        PatientCardConfig(showLastVisit = false),
    )
    registerComponent(AppViewTypes.PersonalSection, PersonalSectionRenderer(), PersonalSectionConfig)
    // ...
}
```

Here `PatientCardRenderer` does double duty: as a list card with default config, and as a detail header with `showLastVisit = false`.

## Reference app: patient feature tour

The patient list and profile feature exercises every interesting surface of the library. Read the files in this order to see the end-to-end flow:

- [`App.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/App.kt) — builds the registry, installs it via `LocalViewRegistry`, sets up the `NavHost`.
- [`AppViewRegistry.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewRegistry.kt) and [`AppViewTypes.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewTypes.kt) — the registry builder and the view-type constants.
- [`feature/patient/list/PatientListRegistrations.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListRegistrations.kt) and [`feature/patient/profile/PatientProfileRegistrations.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/profile/PatientProfileRegistrations.kt) — what each screen registers.
- [`feature/patient/list/PatientListScreen.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListScreen.kt) and [`feature/patient/profile/PatientProfileScreen.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/profile/PatientProfileScreen.kt) — the screens themselves.
- [`feature/patient/list/PatientCardRenderer.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientCardRenderer.kt) and the section renderers under [`feature/patient/profile/`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/profile/) — concrete `ComponentRenderer` implementations.
- [`data/repository/PatientRepository.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/repository/PatientRepository.kt) and [`data/datasource/PatientDataSource.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/datasource/PatientDataSource.kt) — the in-memory JSON data layer (placeholder until real sources land).

## Adding a new feature

To plug a new screen into the registry pattern:

1. Add the view-type constants you need to [`AppViewTypes.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewTypes.kt).
2. Author the renderer class(es) implementing `ComponentRenderer<YourData, YourConfig>`. Keep configs as small immutable data classes or objects.
3. Create a `*Registrations.kt` next to the renderers, exposing `fun ViewRegistry.registerYourFeature() { ... }` that calls `registerComponent` / `registerLayout` for each view-type.
4. Call `registerYourFeature()` from `buildAppViewRegistry()` in [`AppViewRegistry.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewRegistry.kt).
5. Build the screen with `ListScaffold` or `DetailScaffold`, referencing the new view-types in the DSL builder.
6. Wire the route into the `NavHost` in [`App.kt`](./ohs-player-reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/App.kt).
7. Add an assertion to [`AppViewRegistryTest`](./ohs-player-reference-app/src/commonTest/kotlin/dev/ohs/player/reference/app/AppViewRegistryTest.kt) so missing registrations are caught at test time, not at screen-open time.

## Build and run

All commands run from the repo root. Use `./gradlew` on macOS/Linux and `.\gradlew.bat` on Windows.

### Android

```shell
./gradlew :ohs-player-reference-app:assembleDebug
```

### Desktop (JVM)

```shell
./gradlew :ohs-player-reference-app:run
```

### Web

For the Wasm target (faster, modern browsers only):

```shell
./gradlew :ohs-player-reference-app:wasmJsBrowserDevelopmentRun
```

For the JS target (slower, supports older browsers):

```shell
./gradlew :ohs-player-reference-app:jsBrowserDevelopmentRun
```

### iOS

Open the [`iosApp/`](./iosApp) directory in Xcode and run, or use the run-configuration widget in your Kotlin Multiplatform IDE.

### Tests

Run all multiplatform tests:

```shell
./gradlew :ohs-player-library:allTests :ohs-player-reference-app:allTests
```

Run JVM tests only:

```shell
./gradlew :ohs-player-library:jvmTest :ohs-player-reference-app:jvmTest
```

### Local Release Signing (Android)

Release Android builds are signed via environment variables in CI. To test a signed
release build locally, copy `keystore.properties.template` to `keystore.properties`
and fill in the four values:

```shell
cp keystore.properties.template keystore.properties
# Edit keystore.properties with your keystore path, alias, and passwords, then:
./gradlew :ohs-player-reference-app:bundleRelease
```

`keystore.properties` is gitignored — never commit it. Environment variables
(`ANDROID_KEYSTORE_PATH`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`,
`ANDROID_STORE_PASSWORD`) take precedence over the file when both are set. If
neither is configured, release builds are emitted unsigned.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform), [Kotlin/Wasm](https://kotl.in/wasm/).
