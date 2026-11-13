package com.gpudev.accelerator.cuda.ptx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PtxSourceMapTest {
    @Test
    fun `maps instruction rows to current source location`() {
        val ptx = """
            .version 8.0
            .file 1 "/remote/project/kernel.cu"
            .file 2 "/remote/project/include/helper.cuh"
            .visible .entry kernel() {
            .loc 1 12 0
                ld.global.u32 %r1, [%rd1];
                add.u32 %r2, %r1, 1;
            .loc 2 7 0
                mul.lo.u32 %r3, %r2, 4;
            .loc 1 13 0
                st.global.u32 [%rd2], %r3;
            }
        """.trimIndent()
        val lines = ptx.lines()
        val references = PtxSourceMap.parse(ptx).forSourceFile("/local/project/kernel.cu")

        assertEquals(
            listOf(
                PtxLineReference(lines.indexOf("    ld.global.u32 %r1, [%rd1];") + 1, "/remote/project/kernel.cu", 12),
                PtxLineReference(lines.indexOf("    add.u32 %r2, %r1, 1;") + 1, "/remote/project/kernel.cu", 12),
                PtxLineReference(lines.indexOf("    st.global.u32 [%rd2], %r3;") + 1, "/remote/project/kernel.cu", 13)
            ),
            references
        )
    }

    @Test
    fun `maps nothing when metadata is missing or filename is ambiguous`() {
        val withoutMetadata = PtxSourceMap.parse("add.u32 %r1, %r2, 1;")
        assertTrue(withoutMetadata.references.isEmpty())

        val ambiguous = PtxSourceMap.parse(
            """
                .file 1 "/one/kernel.cu"
                .file 2 "/two/kernel.cu"
                .loc 1 3 0
                add.u32 %r1, %r2, 1;
                .loc 2 5 0
                add.u32 %r3, %r4, 1;
            """.trimIndent()
        )
        assertTrue(ambiguous.forSourceFile("/workspace/kernel.cu").isEmpty())
    }

    @Test
    fun `ignores malformed locations and reports included source rows separately`() {
        val map = PtxSourceMap.parse(
            """
                .file 1 "/workspace/kernel.cu"
                .file 2 "/workspace/include/helper.cuh"
                .loc 1 nope 0
                add.u32 %r1, %r2, 1;
                .loc 2 9 0
                mul.lo.u32 %r3, %r4, 2;
            """.trimIndent()
        )

        assertTrue(map.forSourceFile("/workspace/kernel.cu").isEmpty())
        assertEquals(9, map.forSourceFile("/workspace/include/helper.cuh").single().sourceLine)
    }

    @Test
    fun `matches remote source path to local mount when basename is duplicated`() {
        val map = PtxSourceMap.parse(
            """
                .file 1 "/remote/workspace/kernels/common/kernel.cu"
                .file 2 "/remote/other/kernels/common/kernel.cu"
                .loc 1 25 0
                add.u32 %r1, %r2, 1;
                .loc 2 80 0
                add.u32 %r3, %r4, 1;
            """.trimIndent()
        )

        assertEquals(
            25,
            map.forSourceFile("/local/workspace/kernels/common/kernel.cu") { remotePath ->
                if (remotePath == "/remote/workspace/kernels/common/kernel.cu") {
                    "/local/workspace/kernels/common/kernel.cu"
                } else {
                    null
                }
            }.single().sourceLine
        )
    }

    @Test
    fun `resolves source locations when file table follows PTX instructions`() {
        val map = PtxSourceMap.parse(
            """
                .loc 1 25 0
                add.u32 %r1, %r2, 1;
                }
                .file 1 "/remote/workspace/kernels/common/kernel.cu"
            """.trimIndent()
        )

        assertEquals(
            25,
            map.forSourceFile("/local/workspace/kernels/common/kernel.cu").single().sourceLine
        )
    }

    @Test
    fun `maps instructions with trailing PTX comments`() {
        val map = PtxSourceMap.parse(
            """
                .file 1 "/remote/workspace/kernels/common/kernel.cu"
                .loc 1 25 0
                add.u32 %r1, %r2, 1; // compiler comment
            """.trimIndent()
        )

        assertEquals(25, map.forSourceFile("/local/workspace/kernels/common/kernel.cu").single().sourceLine)
    }
}
