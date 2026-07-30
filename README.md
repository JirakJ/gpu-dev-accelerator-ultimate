# GPU Dev Accelerator Ultimate

**CUDA, Metal, and OpenCL development for IntelliJ-based IDEs**

[![Version](https://img.shields.io/badge/Version-2026.5.0-green.svg)](https://plugins.jetbrains.com)

Write, compile, inspect, and analyze GPU code in one IDE. Every feature is backed by a
real tool integration (nvcc, compute-sanitizer, Nsight Compute, nvidia-smi) or by
deterministic static analysis — no mock data.

## Features

### Language support (.cu, .cuh, .metal, .cl)
- Syntax highlighting for CUDA C/C++, Metal Shading Language, OpenCL C
- Code completion with API signatures for 100+ builtins (runtime API, atomics, warp/simd primitives, barriers)
- Quick documentation (Ctrl+Q) for kernel qualifiers and builtins
- Line/block commenting (Ctrl+/), brace matching
- Live templates: `cukernel`, `cugridstride`, `cucheck`, `culaunch`, `cushared`, `cutime`, `mtlkernel`, `clkernel`

### Toolchain integration (GPU Dev console, background tasks)
- **Compile CUDA File** (Ctrl+Alt+C) — nvcc with configured `-arch`/flags, clickable errors
- **Show PTX** — inspect the code nvcc actually generates for the current file
- **Memory Check** (Ctrl+Alt+M) — compute-sanitizer (or cuda-memcheck) on an executable
- **Profile Executable** (Ctrl+Shift+P) — Nsight Compute (`ncu`) or nvprof report
- **GPU Information** — nvidia-smi device query + Apple GPUs via system_profiler
- **Multi-GPU Topology** (Ctrl+Alt+G) — real link matrix from `nvidia-smi topo -m` with live utilization

### Static analysis
- Inspections: GPU memory leaks (cudaMalloc/cudaFree pairing), unchecked CUDA API calls
  (with a **Wrap in CUDA_CHECK** quick fix), kernel performance heuristics
- **Analyze Kernel** (Ctrl+Alt+O) — strided access, warp divergence, `__syncthreads()` in
  divergent branches, missing bounds checks, missing `__restrict__`, device-side malloc, and more
- **Occupancy Calculator** — theoretical SM occupancy for CC 7.0–9.0 with limiting-factor breakdown
- **Migrate Kernel** — CUDA ⇄ Metal / OpenCL / HIP source-port starting points

## Requirements

- IntelliJ IDEA / CLion 2024.3+ (any IntelliJ-based IDE)
- NVIDIA CUDA Toolkit for compile/profile/sanitize features (`nvcc`, `compute-sanitizer`, `ncu` on PATH,
  or set paths in **Settings | Tools | GPU Dev Accelerator**)
- macOS for Apple GPU detection

## Quick start

1. Open a `.cu` file — highlighting, completion, and inspections are active immediately.
2. Type `cukernel` + Tab for a kernel skeleton with a bounds check.
3. `Ctrl+Alt+C` to compile; errors are clickable in the **GPU Dev** tool window.
4. **GPU Dev | Occupancy Calculator** to size your blocks before profiling.

## Development

```bash
./gradlew test          # 136 unit tests
./gradlew runIde        # sandbox IDE
./gradlew buildPlugin   # build/distributions/*.zip
```

## Support

- Issues: https://github.com/JirakJ/gpu-dev-accelerator-ultimate/issues
- Discussions: https://github.com/JirakJ/gpu-dev-accelerator-ultimate/discussions
- Enterprise: support@jakubjirak.com

## License

See [LICENSE](LICENSE) for details.

---

**© 2026 Jakub Jirák. All rights reserved.**
