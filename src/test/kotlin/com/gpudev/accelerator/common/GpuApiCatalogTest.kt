/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GpuApiCatalogTest {

    @Test
    fun `catalogs are non-trivial and free of duplicates`() {
        for ((language, entries) in mapOf(
            "CUDA" to GpuApiCatalog.cuda, "Metal" to GpuApiCatalog.metal, "OpenCL" to GpuApiCatalog.opencl
        )) {
            assertTrue(entries.size >= 25, "$language catalog too small: ${entries.size}")
            assertEquals(entries.size, entries.map { it.name }.toSet().size, "$language has duplicate names")
            entries.forEach {
                assertTrue(it.doc.isNotBlank() && it.signature.isNotBlank(), "$language ${it.name} missing docs")
            }
        }
    }

    @Test
    fun `lookup resolves by language`() {
        assertNotNull(GpuApiCatalog.lookup("CUDA", "cudaMalloc"))
        assertNotNull(GpuApiCatalog.lookup("Metal", "threadgroup_barrier"))
        assertNotNull(GpuApiCatalog.lookup("OpenCL", "get_global_id"))
        assertNull(GpuApiCatalog.lookup("CUDA", "get_global_id"))
        assertNull(GpuApiCatalog.lookup("Unknown", "cudaMalloc"))
    }
}
