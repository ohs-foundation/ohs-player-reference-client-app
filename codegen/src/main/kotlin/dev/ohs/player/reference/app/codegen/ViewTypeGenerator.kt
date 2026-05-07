package dev.ohs.player.reference.app.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import java.io.File

/**
 * Generates top-level [ViewType] val declarations from a CodeSystem JSON file.
 * e.g. `val PatientHeader = ViewType("PatientHeader")`
 */
object ViewTypeGenerator {

    private val viewTypeClass = ClassName("dev.ohs.player.library.config", "ViewType")

    fun generate(fshResourceDir: File, outputPackage: String): FileSpec? {
        val csFile = File(fshResourceDir, "CodeSystem-view-type-codesystem.json")
        if (!csFile.exists()) return null

        val cs = parseCodeSystem(csFile)
        val fileSpec = FileSpec.builder(outputPackage, "ViewTypes")

        cs.concept.forEach { concept ->
            val codeVal = concept.code?.value ?: return@forEach
            val propName = codeVal.replaceFirstChar { it.uppercase() }

            fileSpec.addProperty(
                PropertySpec.builder(propName, viewTypeClass)
                    .initializer("%T(%S)", viewTypeClass, codeVal)
                    .addKdoc(concept.display?.value ?: "")
                    .build()
            )
        }

        return fileSpec.build()
    }
}
