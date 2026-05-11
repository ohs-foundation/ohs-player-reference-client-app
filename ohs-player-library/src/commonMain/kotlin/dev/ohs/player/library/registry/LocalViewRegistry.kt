package dev.ohs.player.library.registry

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The active [ViewRegistry] for the composition. Defaults to an empty registry
 * so direct-pass DSL paths (e.g. `component(MyRenderer(), MyConfig())`) work without a provider.
 * Provide an app-level registry via:
 *
 * ```
 * CompositionLocalProvider(LocalViewRegistry provides registry) { ... }
 * ```
 */
val LocalViewRegistry = staticCompositionLocalOf { ViewRegistry() }
