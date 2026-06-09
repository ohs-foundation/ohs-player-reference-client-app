/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ohs.player.library.extractor

import dev.ohs.player.library.config.fhirScalarSerializers
import kotlinx.serialization.json.Json

/**
 * The `Json` used to turn an extracted row into a typed state. State classes are `@Serializable`,
 * so the compiler already provides their serializers; this only adds the contextual serializers for
 * the FHIR scalar types they reference (`FhirDate`, `FhirDateTime`, `BigDecimal`) — shared with the
 * config store via [fhirScalarSerializers].
 */
//TODO delete this once kotlin-fhir beta04 is available, a contextualized Json is no longer required

internal val stateJson: Json = Json {
  ignoreUnknownKeys = true
  isLenient = true
  serializersModule = fhirScalarSerializers
}
