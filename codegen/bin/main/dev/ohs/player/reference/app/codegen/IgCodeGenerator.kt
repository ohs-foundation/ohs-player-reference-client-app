package dev.ohs.player.reference.app.codegen

import java.io.File

private const val OUTPUT_PACKAGE = "dev.ohs.player.generated"

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: IgCodeGenerator <igDir> <outDir>")
        return
    }

    val igDir = File(args[0])
    val outDir = File(args[1]).also { it.mkdirs() }
    val fshResourceDir = File(igDir, "fsh-generated/resources") // SDs, CodeSystems
    val igOutputDir = File(igDir, "output")                     // Binary instances

    println("Generating from IG: ${igDir.absolutePath}")

    ViewConfigGenerator.generate(fshResourceDir, OUTPUT_PACKAGE)?.writeTo(outDir)
        ?: println("Skipped ViewConfigs: StructureDefinition-ViewConfig.json not found")

    ViewStateGenerator.generate(igOutputDir, fshResourceDir, OUTPUT_PACKAGE)?.writeTo(outDir)
        ?: println("Skipped ViewStates: no Binary-*View.json files found")

    ViewTypeGenerator.generate(fshResourceDir, OUTPUT_PACKAGE)?.writeTo(outDir)
        ?: println("Skipped ViewTypes: CodeSystem not found")

    ViewRegistryGenerator.generate(igOutputDir, fshResourceDir, OUTPUT_PACKAGE)?.writeTo(outDir)
        ?: println("Skipped ViewRegistry: no Binary-*View.json files found")

    println("Generation complete → $outDir")
}
