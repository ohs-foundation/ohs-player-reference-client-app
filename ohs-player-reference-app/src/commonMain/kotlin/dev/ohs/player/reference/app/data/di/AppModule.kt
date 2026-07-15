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
package dev.ohs.player.reference.app.data.di

import dev.ohs.player.reference.app.data.repository.FhirEngineRepository
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.GroupRepository
import dev.ohs.player.reference.app.data.repository.PatientRepository
import dev.ohs.player.reference.app.feature.group.list.GroupListViewModel
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileViewModel
import dev.ohs.player.reference.app.feature.patient.list.PatientListViewModel
import dev.ohs.player.reference.app.feature.patient.profile.PatientProfileViewModel
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireHostViewModel
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireLaunchContext
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireService
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Binds the production [FhirRepository], backed by [FhirEngineRepository]. Requires a
 * [dev.ohs.fhir.FhirEngine] binding, supplied by each platform's module (see `initKoin` callers).
 */
internal val fhirEngineRepositoryModule = module {
  single<FhirRepository> { FhirEngineRepository(get()) }
}

/**
 * Repositories that only depend on [FhirRepository] — kept separate from
 * [fhirEngineRepositoryModule] so tests can swap in a fake [FhirRepository] without redeclaring
 * these bindings.
 */
internal val repositoryModule = module {
  single { PatientRepository(get()) }
  single { GroupRepository(get()) }
}

internal val serviceModule = module { factory { QuestionnaireService(get()) } }

internal val viewModelModule = module {
  viewModel { PatientListViewModel(get()) }
  viewModel { (patientId: String) -> PatientProfileViewModel(patientId, get()) }
  viewModel { GroupListViewModel(get()) }
  viewModel { (groupId: String) -> GroupProfileViewModel(groupId, get()) }
  viewModel { (questionnaireId: String, launchContext: QuestionnaireLaunchContext) ->
    QuestionnaireHostViewModel(questionnaireId, launchContext, get())
  }
}
