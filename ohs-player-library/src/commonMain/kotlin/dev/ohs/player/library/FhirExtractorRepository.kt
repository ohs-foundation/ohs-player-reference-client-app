package dev.ohs.player.library

interface FhirExtractorRepository {
    fun extract(request: FhirExtractionRequest): FhirExtractionResult
}

class ExtractFhirDataUseCase(private val repository: FhirExtractorRepository) {
    operator fun invoke(request: FhirExtractionRequest): FhirExtractionResult =
        repository.extract(request)
}