package dev.ohs.player.datatransformer

import kotlin.test.Test
import kotlin.test.assertSame

class DataTransformerTest {
    @Test
    fun singleton() {
        assertSame(DataTransformer, DataTransformer)
    }
}
