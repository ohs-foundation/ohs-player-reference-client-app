package dev.ohs.player.library.registry

import kotlin.reflect.KClass

/**
 * Composite lookup key for the [ViewRegistry]: `(view-type × data type)`.
 *
 * Both dimensions are part of the key so the same [ViewType] name registered for
 * different `T`s never collides. Looking up `(Card, PatientView)` and
 * `(Card, MedicationView)` return distinct renderers; querying with the wrong `T`
 * throws rather than returning a mismatched renderer.
 *
 * ```
 * val key = ViewTypeKey(ViewType("Card"), PatientView::class)
 * ```
 *
 * @param viewType the visual role label.
 * @param dataType the Kotlin class of the data model that this renderer handles.
 */
data class ViewTypeKey<T : Any>(val viewType: ViewType, val dataType: KClass<T>)
