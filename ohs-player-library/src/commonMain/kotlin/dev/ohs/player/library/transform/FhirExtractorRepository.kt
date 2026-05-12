package dev.ohs.player.library.transform

import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.library.model.ViewDefinition

interface FhirExtractorRepository {
    fun extract(resource: Resource, viewDefinition: ViewDefinition): FhirExtractionResult
}

class ExtractFhirDataUseCase(private val repository: FhirExtractorRepository){
    operator fun invoke(resource: Resource, viewDefinition: ViewDefinition) : FhirExtractionResult =
        repository.extract(resource, viewDefinition)
}