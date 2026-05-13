package dev.ohs.player.reference.app.feature.patient.profile

import androidx.lifecycle.ViewModel
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PatientProfileViewModel(patientId: String) : ViewModel() {

    private val _patient = MutableStateFlow<PatientView?>(null)
    val patient: StateFlow<PatientView?> = _patient.asStateFlow()

    init {
        _patient.value = PatientRepository.getById(patientId)
    }
}
