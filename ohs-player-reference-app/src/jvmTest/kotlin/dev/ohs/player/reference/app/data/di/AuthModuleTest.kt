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

import dev.ohs.player.reference.app.auth.AuthService
import dev.ohs.player.reference.app.auth.AuthViewModel
import kotlin.test.AfterTest
import kotlin.test.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class AuthModuleTest : KoinTest {

  private val authService by inject<AuthService>()

  @AfterTest fun tearDown() = stopKoin()

  @Test
  fun authModule_resolvesAuthServiceAndAuthViewModel() {
    startKoin { modules(authModule, viewModelModule) }

    authService // resolves without throwing
    getKoin().get<AuthViewModel>()
  }
}
