package com.remotedexter.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreTest {
    @Test
    fun `greet returns core-ok`() {
        assertEquals("core-ok", Core.greet())
    }
}
