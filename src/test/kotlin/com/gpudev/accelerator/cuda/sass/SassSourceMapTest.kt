package com.gpudev.accelerator.cuda.sass

import com.gpudev.accelerator.cuda.ptx.SourceLineReference

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SassSourceMapTest {
    @Test
    fun `maps SASS instruction rows from nvdisasm line annotations`() {
        val output = """
            //--------------------- .debug_line --------------------------
                    /*0000*/  .byte 0x00, 0x01
            //--------------------- .text._Z5probePf --------------------------
            .section .text._Z5probePf,"ax",@progbits
            .text._Z5probePf:
                //## File "/remote/workspace/kernels/common/kernel.cu", line 1
                    /*0000*/ MOV R1, c[0x0][0x28] ;
                //## File "/remote/workspace/kernels/common/kernel.cu", line 2
                    /*0010*/ S2R R2, SR_TID.X ;
                    /*0020*/ MOV R3, 0x4 ;
                //## File "/remote/workspace/kernels/common/kernel.cu", line 3
                    /*0030*/ EXIT ;
            //--------------------- .nv.info --------------------------
                    /*0000*/ .word 0x00000000
        """.trimIndent()
        val lines = output.lines()
        val references = SassSourceMap.forSourceFile(
            SassSourceMap.parse(output),
            "/local/workspace/kernels/common/kernel.cu"
        ) { remotePath -> remotePath.replace("/remote/workspace", "/local/workspace") }

        assertEquals(
            listOf(
                SourceLineReference(lines.indexOf("        /*0000*/ MOV R1, c[0x0][0x28] ;") + 1,
                    "/remote/workspace/kernels/common/kernel.cu", 1),
                SourceLineReference(lines.indexOf("        /*0010*/ S2R R2, SR_TID.X ;") + 1,
                    "/remote/workspace/kernels/common/kernel.cu", 2),
                SourceLineReference(lines.indexOf("        /*0020*/ MOV R3, 0x4 ;") + 1,
                    "/remote/workspace/kernels/common/kernel.cu", 2),
                SourceLineReference(lines.indexOf("        /*0030*/ EXIT ;") + 1,
                    "/remote/workspace/kernels/common/kernel.cu", 3)
            ),
            references
        )
    }

    @Test
    fun `leaves SASS unmapped without line annotations`() {
        val output = """
            //--------------------- .text.kernel --------------------------
            .text.kernel:
                /*0000*/ MOV R1, c[0x0][0x28] ;
        """.trimIndent()

        assertTrue(SassSourceMap.parse(output).isEmpty())
    }
}
