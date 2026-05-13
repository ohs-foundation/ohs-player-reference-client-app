package dev.ohs.player.library.registry

import kotlin.jvm.JvmInline

/**
 * Opaque string label used to dispatch a renderer for a particular visual role.
 *
 * Paired with a data-type [kotlin.reflect.KClass] in [ViewTypeKey] to form the
 * registry's lookup key. View-types are normally declared once per app as constants.
 *
 * ```
 * object AppViewTypes {
 *     val Card = ViewType("Card")
 *     val PatientHeader = ViewType("PatientHeader")
 * }
 * ```
 *
 * @param value the underlying identifier; conventionally PascalCase.
 */
@JvmInline
value class ViewType(val value: String)
