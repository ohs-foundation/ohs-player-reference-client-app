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
package dev.ohs.player.reference.app.data.sync

import java.net.NetworkInterface
import java.util.Collections

/**
 * Desktop has no OS-level sync scheduler to enforce a network constraint (unlike Android/iOS), so
 * [dev.ohs.player.reference.app.data.sync.Sync.periodicSync] checks this before every cycle. Fails
 * open (returns true) on enumeration errors — an unnecessary sync attempt that fails is preferable
 * to silently never syncing again because this heuristic broke.
 */
internal actual fun isNetworkConnected(): Boolean =
  try {
    Collections.list(NetworkInterface.getNetworkInterfaces()).any { it.isUp && !it.isLoopback }
  } catch (e: Exception) {
    true
  }
