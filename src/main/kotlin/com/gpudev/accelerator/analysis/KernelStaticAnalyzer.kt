/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.analysis

/**
 * Rule-based static analysis of GPU kernel source. Replaces the former
 * "AI optimizer" — these are deterministic pattern heuristics, labeled as such.
 */
object KernelStaticAnalyzer {

    enum class Impact { HIGH, MEDIUM, LOW }

    data class Finding(
        val title: String,
        val detail: String,
        val impact: Impact,
        /** 0-based line of the first offending occurrence, -1 when file-wide. */
        val line: Int
    )

    private data class Rule(val title: String, val detail: String, val impact: Impact, val pattern: Regex)

    private val lineRules = listOf(
        Rule(
            "Strided global memory access",
            "Threads in a warp access non-contiguous addresses (index scaled by a stride). " +
                "Coalesced layout — consecutive threads reading consecutive addresses — can be several times faster.",
            Impact.HIGH,
            Regex("""\[\s*threadIdx\.[xyz]\s*\*""")
        ),
        Rule(
            "Warp divergence from thread-dependent branch",
            "A branch condition using threadIdx with % or & splits warps into divergent paths that serialize. " +
                "Prefer predication or restructure so whole warps take the same path.",
            Impact.MEDIUM,
            Regex("""if\s*\(\s*threadIdx\.[xyz]\s*[%&]""")
        ),
        Rule(
            "Device-side heap allocation in kernel",
            "malloc/new inside a kernel serializes on the device heap and fragments memory. " +
                "Allocate from the host or use shared memory.",
            Impact.HIGH,
            Regex("""__global__[\s\S]{0,400}?\b(?:malloc\s*\(|new\s+\w)""")
        ),
        Rule(
            "printf in device code",
            "Device printf serializes threads and inflates register usage. Fine for debugging, remove in production kernels.",
            Impact.LOW,
            Regex("""__global__[\s\S]{0,600}?\bprintf\s*\(""")
        ),
        Rule(
            "atomicAdd inside a loop",
            "An atomic in a loop body contends on every iteration. Accumulate in a register and issue one atomic after the loop.",
            Impact.MEDIUM,
            Regex("""for\s*\([^)]*\)\s*\{[^}]{0,300}?atomic(?:Add|Sub|Max|Min|And|Or|Xor|Exch|CAS)\s*\(""")
        ),
        Rule(
            "cudaDeviceSynchronize in a loop",
            "Host-side sync inside a loop stalls the pipeline every iteration. Sync once after the loop, or use streams/events.",
            Impact.MEDIUM,
            Regex("""(?:for|while)\s*\([^)]*\)\s*\{[^}]{0,300}?cudaDeviceSynchronize\s*\(""")
        ),
        Rule(
            "pow() with small integer exponent",
            "pow(x, 2.0) is far slower than x*x. Replace small integer powers with multiplication.",
            Impact.LOW,
            Regex("""\bpow[f]?\s*\(\s*[^,]+,\s*[234](?:\.0*f?)?\s*\)""")
        ),
        Rule(
            "Double-precision literal in float expression",
            "A literal without the f suffix promotes the expression to double — on consumer GPUs that is up to 64x slower. " +
                "Write 1.0f instead of 1.0.",
            Impact.LOW,
            Regex("""float\s+\w+\s*=\s*[^;]*\d\.\d+(?![0-9fF])""")
        )
    )

    fun analyze(code: String): List<Finding> {
        val findings = mutableListOf<Finding>()

        for (rule in lineRules) {
            val match = rule.pattern.find(code) ?: continue
            findings.add(Finding(rule.title, rule.detail, rule.impact, lineOf(code, match.range.first)))
        }

        // __global__ kernels with pointer params lacking __restrict__
        val kernelSig = Regex("""__global__\s+\w+\s+(\w+)\s*\(([^)]*)\)""")
        for (m in kernelSig.findAll(code)) {
            val params = m.groupValues[2]
            if (params.contains('*') && !params.contains("__restrict__")) {
                findings.add(
                    Finding(
                        "Pointer parameters without __restrict__ in kernel '${m.groupValues[1]}'",
                        "__restrict__ tells the compiler pointers don't alias, enabling load reordering and read-only cache use.",
                        Impact.LOW,
                        lineOf(code, m.range.first)
                    )
                )
            }
            // global index computed but never bounds-checked
            val body = code.substring(m.range.last.coerceAtMost(code.length))
            if (body.take(800).contains(Regex("""blockIdx\.[xyz]\s*\*\s*blockDim\.[xyz]""")) &&
                !body.take(800).contains(Regex("""if\s*\(\s*\w+\s*<"""))
            ) {
                findings.add(
                    Finding(
                        "Possible missing bounds check in kernel '${m.groupValues[1]}'",
                        "A global index is computed from blockIdx*blockDim but no `if (idx < n)` guard was found. " +
                            "Grids usually overshoot the data size; unguarded accesses read/write out of bounds.",
                        Impact.MEDIUM,
                        lineOf(code, m.range.first)
                    )
                )
            }
        }

        // __syncthreads() inside a thread-dependent conditional — deadlock risk
        val syncInBranch = Regex("""if\s*\([^)]*threadIdx[^)]*\)\s*\{[^}]{0,400}?__syncthreads\s*\(""")
        syncInBranch.find(code)?.let {
            findings.add(
                Finding(
                    "__syncthreads() inside thread-dependent branch",
                    "If any thread of the block skips the barrier, the kernel deadlocks. " +
                        "Move __syncthreads() outside the conditional.",
                    Impact.HIGH,
                    lineOf(code, it.range.first)
                )
            )
        }

        return findings.sortedBy { it.impact }
    }

    private fun lineOf(code: String, offset: Int): Int =
        code.substring(0, offset.coerceIn(0, code.length)).count { it == '\n' }
}
