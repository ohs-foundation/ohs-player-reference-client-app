package dev.ohs.player.reference.app.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.ohs.fhir.model.r4.ElementDefinition
import dev.ohs.fhir.model.r4.StructureDefinition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/** Generates a sealed ViewConfig hierarchy from logical StructureDefinition JSON files. */
object ViewConfigGenerator {

    fun generate(fshResourceDir: File, outputPackage: String): FileSpec? {
        val rootSdFile = File(fshResourceDir, "StructureDefinition-ViewConfig.json")
        if (!rootSdFile.exists()) return null

        val rootSd = parseSd(rootSdFile)
        val childSds = listOf("StructureDefinition-CardConfig.json", "StructureDefinition-TextConfig.json")
            .map { File(fshResourceDir, it) }
            .filter { it.exists() }
            .map { parseSd(it) }

        val parentPropNames = rootSd.ownFieldNames()

        return FileSpec.builder(outputPackage, "ViewConfigs")
            .addType(buildSealedClass(rootSd))
            .apply { childSds.forEach { addType(buildChildClass(it, rootSd, parentPropNames, outputPackage)) } }
            .build()
    }

    private fun buildSealedClass(sd: StructureDefinition): TypeSpec =
        TypeSpec.classBuilder(sd.nameValue())
            .addModifiers(KModifier.SEALED)
            .addAnnotation(Serializable::class)
            .apply {
                sd.ownElements().forEach { el ->
                    addProperty(
                        PropertySpec.builder(fieldName(el.pathValue()), kotlinTypeFor(el.typeCode()).copy(nullable = true))
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                }
            }
            .build()

    private fun buildChildClass(
        sd: StructureDefinition,
        rootSd: StructureDefinition,
        parentPropNames: Set<String>,
        outputPackage: String
    ): TypeSpec {
        val name = sd.nameValue()
        val ctor = FunSpec.constructorBuilder()
        val builder = TypeSpec.classBuilder(name)
            .addModifiers(KModifier.DATA)
            .superclass(ClassName(outputPackage, rootSd.nameValue()))
            .addAnnotation(Serializable::class)
            .addAnnotation(AnnotationSpec.builder(SerialName::class).addMember("%S", name).build())

        val childPropNames = mutableSetOf<String>()

        sd.ownElements().forEach { el ->
            val propName = fieldName(el.pathValue())
            val type = kotlinTypeFor(el.typeCode()).copy(nullable = true)
            childPropNames.add(propName)
            ctor.addParameter(ParameterSpec.builder(propName, type).defaultValue("null").build())
            val propSpec = PropertySpec.builder(propName, type).initializer(propName)
            if (propName in parentPropNames) propSpec.addModifiers(KModifier.OVERRIDE)
            builder.addProperty(propSpec.build())
        }

        // Override parent abstract properties absent from child's differential
        rootSd.ownElements()
            .filter { fieldName(it.pathValue()) !in childPropNames }
            .forEach { el ->
                val propName = fieldName(el.pathValue())
                val type = kotlinTypeFor(el.typeCode()).copy(nullable = true)
                ctor.addParameter(ParameterSpec.builder(propName, type).defaultValue("null").build())
                builder.addProperty(
                    PropertySpec.builder(propName, type)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer(propName)
                        .build()
                )
            }

        return builder.primaryConstructor(ctor.build()).build()
    }

    // Extension helpers to safely extract Kotlin values from FHIR primitive wrappers

    private fun StructureDefinition.nameValue(): String = name?.value ?: "Unknown"

    private fun StructureDefinition.ownElements() =
        differential?.element
            ?.filter { it.id != id && (it.path.value ?: "").count { c -> c == '.' } == 1 }
            ?: emptyList()

    private fun StructureDefinition.ownFieldNames(): Set<String> =
        ownElements().map { fieldName(it.path.value ?: "") }.toSet()

    private fun ElementDefinition.pathValue(): String = path.value ?: ""

    private fun ElementDefinition.typeCode(): String? = type.firstOrNull()?.code?.value
}
