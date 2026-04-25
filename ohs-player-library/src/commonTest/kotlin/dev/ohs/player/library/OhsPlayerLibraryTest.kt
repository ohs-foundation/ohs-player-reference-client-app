package dev.ohs.player.library

import kotlin.test.Test
import kotlin.test.assertSame

class OhsPlayerLibraryTest {
    @Test
    fun singleton() {
        assertSame(OhsPlayerLibrary, OhsPlayerLibrary)
    }
}
