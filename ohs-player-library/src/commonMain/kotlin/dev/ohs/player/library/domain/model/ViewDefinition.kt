package dev.ohs.player.library.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

// TODO: Generate this class from StructureDefinition using tools like:
//  https://github.com/hapifhir/org.hl7.fhir.core/tree/master/org.hl7.fhir.core.generator
@Serializable
data class ViewDefinition(
    val name: String? = null,
    val resource: String? = null,
    val status: String? = null,
    val fhirVersion: List<String>? = null,
    val select: List<SelectBlock>? = null,
    val where: List<WhereClause>? = null,
    val constant: List<Constant>? = null,
) {
    fun allColumns(): List<ViewColumn> = select?.flatMap { it.allColumns() } ?: emptyList()
}

@Serializable
data class SelectBlock(
    val column: List<ViewColumn>? = null,
    val select: List<SelectBlock>? = null,
    val forEach: String? = null,
    val forEachOrNull: String? = null,
    val unionAll: List<SelectBlock>? = null,
) {
    fun allColumns(): List<ViewColumn> {
        val direct = column ?: emptyList()
        val nested = select?.flatMap { it.allColumns() } ?: emptyList()
        val union = unionAll?.flatMap { it.allColumns() } ?: emptyList()
        return direct + nested + union
    }
}

@Serializable
data class ViewColumn(
    val name: String? = null,
    val path: String? = null,
    val type: String? = null,
    val collection: Boolean = false,
    val description: String? = null,
)

@Serializable
data class WhereClause(
    val path: String? = null,
)

@Serializable
data class Constant(
    val name: String? = null,
    val valueBase64Binary: String? = null,
    val valueBoolean: Boolean? = null,
    val valueCanonical: String? = null,
    val valueCode: String? = null,
    val valueDate: LocalDate? = null,
    val valueDateTime: LocalDateTime? = null,
    val valueDecimal: Double? = null,
    val valueId: String? = null,
    val valueInstant: Instant? = null,
    val valueInteger: Int? = null,
    val valueInteger64: Long? = null,
    val valueOid: String? = null,
    val valueString: String? = null,
    val valuePositiveInt: Int? = null,
    val valueTime: LocalTime? = null,
    val valueUnsignedInt: Int? = null,
    val valueUri: String? = null,
    val valueUrl: String? = null,
    val valueUuid: String? = null,
)
