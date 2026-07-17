# OHS Player Reference Client App

A Kotlin Multiplatform and Compose Multiplatform reference client for [Open Health Stack (OHS)](https://developers.google.com/open-health-stack/overview). A single Kotlin source tree targets Android, iOS, JVM desktop, JS browser, and Wasm browser.

The application renders healthcare UI from configuration rather than hand-written mapping code. FHIR resources are projected into typed view-state by declarative configuration, and that state is rendered by renderers resolved through a registry. The two halves — extraction and rendering — are described below, then joined in a single end-to-end example.

This repository is a GitHub template: use it as the starting point for your own OHS-based app, or just run it to see OHS in action.

## Supported platforms

Android, iOS, Desktop (Windows, macOS, Linux), and Web (JS and Wasm) — all from a single Kotlin source tree. Every platform is built, tested, and released by GitHub Actions; see [Deployment](#deployment).

## Getting started

### Just want to see OHS in action?

The desktop app is the fastest path — it needs only JDK 21, no Android SDK or Xcode:

```shell
git clone <repository-url>
cd ohs-player-reference-client-app
./gradlew :reference-app:run
```

The app ships with bundled sample FHIR data, so it works out of the box. Other platforms:

| Target | Command |
| --- | --- |
| Android | `./gradlew :reference-app:assembleDebug` |
| Web (Wasm) | `./gradlew :reference-app:wasmJsBrowserDevelopmentRun` |
| Web (JS) | `./gradlew :reference-app:jsBrowserDevelopmentRun` |

For iOS, open [`iosApp/`](./iosApp) in Xcode and run, or use the run-configuration widget in a Kotlin Multiplatform IDE.

### Start from this template

1. Click **Use this template → Create a new repository** on GitHub (or clone and re-init).
2. Follow [Developer setup](#developer-setup-kotlin-multiplatform) below and confirm `./gradlew build` passes.
3. Work through [Customizing the template](#customizing-the-template) to make the app yours.

### Developer setup (Kotlin Multiplatform)

- JDK 21
- [Android Studio](https://developer.android.com/studio) with the Kotlin Multiplatform plugin (Android builds also need the Android SDK)
- Xcode (for iOS builds, macOS only)
- Optional: run [`kdoctor`](https://github.com/Kotlin/kdoctor) to verify your multiplatform environment

Use `./gradlew` on macOS and Linux, and `gradlew.bat` on Windows. All commands run from the repository root.

### Build

```shell
./gradlew build
```

Code generation is part of compilation. The `ig-codegen` Gradle plugin runs its `generateIgCode` task automatically before Kotlin compilation, so there is no separate generation step.

## Customizing the template

The starting points, in rough order:

- **Application id / namespace** — `applicationId` and `namespace` in [`reference-app/build.gradle.kts`](./reference-app/build.gradle.kts), the iOS bundle id in [`iosApp/Configuration/Config.xcconfig`](./iosApp/Configuration/Config.xcconfig), and the Kotlin package `dev.ohs.player.reference.app` under `reference-app/src/*/kotlin/`.
- **Application name** — Android: `app_name` in [`reference-app/src/androidMain/res/values/strings.xml`](./reference-app/src/androidMain/res/values/strings.xml); iOS: `PRODUCT_NAME` in `Config.xcconfig`; Desktop: `packageName` in the `compose.desktop` block of `reference-app/build.gradle.kts`; Web: `<title>` in [`reference-app/src/webMain/resources/index.html`](./reference-app/src/webMain/resources/index.html).
- **Icons** — Android launcher icons in `reference-app/src/androidMain/res/mipmap-*/`; iOS in `iosApp/iosApp/Assets.xcassets`.
- **Generated code package** — `packageName` in the `igCodegen` block of `reference-app/build.gradle.kts` (defaults to `dev.ohs.player.generated`).
- **Project names** — `rootProject.name` and the module name in [`settings.gradle.kts`](./settings.gradle.kts). Note that renaming either changes the package of the generated Compose resources class (`Res`).
- **Screens and configuration** — replace the sample `Binary-*.json` configuration under `reference-app/src/commonMain/composeResources/files/` and the feature renderers under `reference-app/src/commonMain/kotlin/.../feature/`; the rest of this README explains how those two fit together.

## Bundled OHS libraries

The app is assembled from the OHS Player library plus the OHS Foundational Libraries; versions are pinned in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml):

| Library | Purpose |
| --- | --- |
| [`dev.ohs.player:reference-library`](https://github.com/ohs-foundation/ohs-player-reference-client-library) | Config-driven views using flattened data from FHIR resources (the player) |
| `dev.ohs.fhir:fhir-model` | Typed Kotlin models for FHIR resources |
| `dev.ohs.fhir:fhir-path` | FHIRPath expression evaluation |
| `dev.ohs.fhir:fhir-data-capture` | FHIR Structured Data Capture (questionnaires) |

### Have an existing app?

If you cannot start from this template, depend on the player library directly — it is a library, not a framework, and can be adopted one screen at a time in any Kotlin Multiplatform or Android project:

```kotlin
commonMain.dependencies {
  implementation("dev.ohs.player:reference-library:1.0.0-alpha01")
}
```

The [library README](https://github.com/ohs-foundation/ohs-player-reference-client-library#readme) is a standalone user guide; this repository then serves as the worked example.

## From FHIR data to view state

A screen never consumes a raw FHIR resource. It consumes a typed *view-state* — a flat, serializable data class containing exactly the fields the screen needs. View-state is produced by a configuration-driven pipeline:

1. **Author** configuration as FHIR `Binary` resources (a `ViewDefinition`, a `ViewJoinMap`, and a `ViewConfig`).
2. **Generate** typed Kotlin from those Binaries at build time via the `ig-codegen` plugin.
3. **Load** the Binaries at runtime through a `ConfigStore`.
4. **Extract** view-state from a `SearchResult` with `GenericStateExtractor.extract<T>()`.

### 1. Author configuration

A `ViewDefinition` declares the columns of a view as FHIRPath expressions over a FHIR resource. Each column carries a name, a path, and a FHIR type. Excerpt from [`Binary-PatientSummary.json`](./reference-app/src/commonMain/composeResources/files/states/Binary-PatientSummary.json):

```json
{
  "resourceType": "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition",
  "name": "PatientSummary",
  "status": "active",
  "resource": "Patient",
  "select": [
    {
      "column": [
        { "name": "patientId", "path": "id", "type": "http://hl7.org/fhir/StructureDefinition/string" },
        { "name": "familyName", "path": "name.family.first()", "type": "http://hl7.org/fhir/StructureDefinition/string" },
        { "name": "gender", "path": "gender", "type": "http://hl7.org/fhir/StructureDefinition/code" },
        { "name": "active", "path": "active", "type": "http://hl7.org/fhir/StructureDefinition/boolean" }
      ]
    }
  ]
}
```

A `ViewJoinMap` names the view-state and binds it to a pivot `ViewDefinition` (and, where needed, joined views). [`Binary-PatientSummaryState.json`](./reference-app/src/commonMain/composeResources/files/states/Binary-PatientSummaryState.json):

```json
{
  "resourceType": "http://ohs.dev/StructureDefinition/ViewJoinMap",
  "name": "patientSummary",
  "from": "root",
  "resource": "Patient",
  "view": "PatientSummary"
}
```

A `ViewConfig` declares the configuration a renderer accepts, with defaults. [`Binary-PatientCardConfig.json`](./reference-app/src/commonMain/composeResources/files/configs/Binary-PatientCardConfig.json):

```json
{
  "resourceType": "http://ohs.dev/StructureDefinition/ViewConfig",
  "viewType": "PatientCard",
  "property": [
    { "name": "showStatusChip", "type": "boolean", "valueBoolean": true },
    { "name": "showAge", "type": "boolean", "valueBoolean": true },
    { "name": "elevation", "type": "decimal", "valueDecimal": 2.0 }
  ]
}
```

A single `CodeSystem` Binary enumerates the view-types the app renders; see [`CodeSystem-ViewTypes.json`](./reference-app/src/commonMain/composeResources/files/viewtypes/CodeSystem-ViewTypes.json).

### 2. Generate typed Kotlin

The `ig-codegen` plugin reads these Binaries and emits typed sources. It is applied and configured in [`reference-app/build.gradle.kts`](./reference-app/build.gradle.kts):

```kotlin
plugins {
    id("dev.ohs.ig-codegen")
}

igCodegen {
    // sourcesDir defaults to src/commonMain/composeResources/files
    packageName = "dev.ohs.player.generated"
}
```

Inputs live under `src/commonMain/composeResources/files/`, organised as `states/` (ViewDefinition and ViewJoinMap), `configs/` (ViewConfig), and `viewtypes/` (the CodeSystem). The generated symbols are:

| Generated symbol | Source | Package |
| --- | --- | --- |
| `PatientSummaryState` and other `*State` classes | ViewJoinMap + columns | `dev.ohs.player.generated.state` |
| `PatientCardConfig` and other `*Config` classes | ViewConfig | `dev.ohs.player.generated.config` |
| `ViewTypeCS` | CodeSystem | `dev.ohs.player.generated.viewtype` |
| `GeneratedConfigManifest` | file listing | `dev.ohs.player.generated` |

`PatientSummaryState`, for example, is generated as:

```kotlin
@Serializable
data class PatientSummaryState(
    val patientId: String? = null,
    val familyName: String? = null,
    val givenName: String? = null,
    val gender: String? = null,
    val birthDate: FhirDate? = null,
    val active: Boolean? = null,
    val mrn: String? = null,
    val phone: String? = null,
)
```

### 3. Load configuration at runtime

A `ConfigStore` holds the parsed configuration, fed by a `ConfigSource`. The reference app reads the bundled Binaries; replacing this with a network fetch is the only change required to load configuration from a backend. See [`LocalConfigSource.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/datasource/LocalConfigSource.kt):

```kotlin
object LocalConfigSource : ConfigSource {
    private const val DIR_NAME = "states"

    override suspend fun readAll(): List<String> =
        GeneratedConfigManifest.byDirectory[DIR_NAME].orEmpty().map { fileName ->
            Res.readBytes("files/$DIR_NAME/$fileName").decodeToString()
        }
}
```

The store and a single extractor are wired once in [`Extraction.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/Extraction.kt):

```kotlin
object Extraction {
    private val configStore: ConfigStore = ConfigStore(LocalConfigSource)
    val extractor: GenericStateExtractor = GenericStateExtractor(configStore)
}
```

### 4. Extract view-state

`GenericStateExtractor.extract<T>()` selects the configuration for `T` by name, evaluates its FHIRPath columns against a `SearchResult`, and returns a list of typed `T`. A `SearchResult` carries the pivot resource plus any forward-included and reverse-included resources, mirroring a FHIR search response.

From [`PatientRepository.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/data/repository/PatientRepository.kt):

```kotlin
suspend fun getPatients(): List<PatientSummaryState> =
    withContext(extractorDispatcher) {
        allPatientIds().mapNotNull { id ->
            patientSummarySearchResult(id)?.let {
                extractor.extract<PatientSummaryState>(it).firstOrNull()
            }
        }
    }
```

The FHIRPath engine holds mutable evaluation state and is not safe for concurrent use. Serialize extraction onto a single thread; the repository does this with `Dispatchers.Default.limitedParallelism(1)`.

## Rendering view state

View-state is rendered by renderers resolved through a registry, so screens depend on view-types rather than concrete UI classes:

1. **Author** a `ComponentRenderer` for a view-state type.
2. **Register** it under a generated `ViewTypeCS` view-type in a `ViewRegistry`.
3. **Install** the registry into the composition via `LocalViewRegistry`.
4. **Render** with `ListScaffold` or `DetailScaffold`, which resolve renderers by view-type.

### 1. Author a renderer

A `ComponentRenderer<T, C>` renders one item of state `T` with configuration `C`. One renderer class can be registered under several view-types with different configurations.

```kotlin
class PatientCardRenderer : ComponentRenderer<PatientSummaryState, PatientCardConfig> {
    @Composable
    override fun Render(
        item: PatientSummaryState,
        config: PatientCardConfig,
        options: RenderOptions,
    ) {
        PatientCard(patient = item, config = config, onClick = options.onClick, modifier = options.modifier)
    }
}
```

`RenderOptions` carries the optional tap handler and root modifier. `LayoutRenderer<T>` is the corresponding arrangement abstraction; the library ships `VerticalListRenderer`, `HorizontalListRenderer`, and `GridListRenderer`.

### 2. Register renderers

Group a feature's registrations into an extension on `ViewRegistry`. See [`PatientListRegistrations.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListRegistrations.kt):

```kotlin
fun ViewRegistry.registerPatientList() {
    registerComponent<PatientSummaryState, PatientCardConfig>(
        ViewTypeCS.PatientCard,
        PatientCardRenderer(),
        PatientCardConfig(),
    )
    registerLayout<PatientSummaryState>(
        VerticalListRenderer.VIEW_TYPE,
        VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
}
```

Assemble all feature registrations in one builder, as in [`AppViewRegistry.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/AppViewRegistry.kt):

```kotlin
fun buildAppViewRegistry(): ViewRegistry = ViewRegistry().apply {
    registerPatientList()
    registerPatientProfile()
}
```

A registry lookup is keyed by both view-type and state type, and throws `NoSuchElementException` naming the missing key if a renderer was not registered.

### 3. Install the registry

Provide the registry at the composition root so every screen can resolve renderers. See [`App.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/App.kt):

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

### 4. Render

`ListScaffold` renders a list; `component(...)` and `layout(...)` name the view-types to resolve. An empty list short-circuits to `emptyState` without invoking the layout renderer, and omitting `layout(...)` falls back to `VerticalListRenderer`. See [`PatientListScreen.kt`](./reference-app/src/commonMain/kotlin/dev/ohs/player/reference/app/feature/patient/list/PatientListScreen.kt):

```kotlin
ListScaffold<PatientSummaryState>(
    items = patients,
    onItemClick = { onPatientClick(it.patientId ?: "") },
    key = { it.patientId ?: it.hashCode().toString() },
) {
    component(ViewTypeCS.PatientCard)
    layout(VerticalListRenderer.VIEW_TYPE)
    topBar { TopAppBar(title = { Text("Patients") }) }
    emptyState { Text("No patients") }
}
```

`DetailScaffold` is the single-item counterpart: it renders a stack of sections for one nullable item, falling back to a `notFound` slot when the item is absent.

## End-to-end example

A patient list screen exercises both halves of the pipeline:

1. **Configuration.** `Binary-PatientSummary.json` declares the columns; `Binary-PatientSummaryState.json` names the `patientSummary` view-state. `ig-codegen` generates `PatientSummaryState`.
2. **Extraction.** `PatientRepository.getPatients()` builds a `SearchResult` per patient and calls `extractor.extract<PatientSummaryState>(result)`, yielding `List<PatientSummaryState>`.
3. **Registration.** `registerPatientList()` binds `PatientCardRenderer` to `ViewTypeCS.PatientCard` for `PatientSummaryState`, and `buildAppViewRegistry()` installs it at the composition root.
4. **Rendering.** `PatientListScreen` passes the extracted states to `ListScaffold`, which resolves `PatientCard` by view-type and renders each row.

Adding a field is a configuration change: add a column to the `ViewDefinition`, then reference the regenerated state field in the renderer. No extraction or wiring code changes.

## Testing

Run all multiplatform tests:

```shell
./gradlew :reference-app:allTests
```

Run JVM tests only:

```shell
./gradlew :reference-app:jvmTest
```

## Deployment

### Continuous integration

Every pull request and push to `main` is validated by the [`ci.yml`](./.github/workflows/ci.yml) workflow: formatting (spotless), JVM tests, Android lint, JS/Wasm compilation, and iOS compile-and-link, each as a separate job.

### Release pipeline

Releases are produced by the [`release.yml`](./.github/workflows/release.yml) GitHub Actions workflow, triggered by a semantic version tag (`vX.Y.Z` or `vX.Y.Z-suffix`). The workflow builds and signs every platform, then publishes a GitHub Release with checksummed artifacts:

- Android APK (`assembleRelease`)
- Desktop installers: Linux `.deb` and `.rpm`, Windows `.msi`, macOS `.dmg`
- A portable Linux tarball (`createDistributable`)

A `workflow_dispatch` run performs a dry run: it builds, signs, and uploads artifacts without publishing a Release. The web (Wasm) and GitHub Pages jobs are currently gated off (`if: false`) pending a larger build runner; the web preview is deployed manually in the interim.

### Local installers

Build a native installer or distributable locally:

```shell
./gradlew :reference-app:packageDmg                 # macOS .dmg
./gradlew :reference-app:packageMsi                 # Windows .msi
./gradlew :reference-app:packageDeb                 # Linux .deb
./gradlew :reference-app:createDistributable        # portable app image
./gradlew :reference-app:wasmJsBrowserDistribution  # web bundle
```

### Android release signing

Release builds read signing inputs from environment variables first, then from a `keystore.properties` file as a development fallback. To produce a signed release locally:

```shell
cp keystore.properties.template keystore.properties
# Edit keystore.properties with your keystore path, alias, and passwords, then:
./gradlew :reference-app:bundleRelease
```

`keystore.properties` is gitignored and must never be committed. The environment variables `ANDROID_KEYSTORE_PATH`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and `ANDROID_STORE_PASSWORD` take precedence over the file when both are set. If neither is configured, release builds are emitted unsigned.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform), and [Kotlin/Wasm](https://kotl.in/wasm/).
