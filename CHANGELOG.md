# Changelog

## [Unreleased]

### Added
- Remote tool execution through CLion's default SSH toolchain: **Compile CUDA File**, **Show PTX**, **Memory Check**, **Profile Executable**, **GPU Information** and **Multi-GPU Topology** now run on the remote host, with local↔remote paths mapped through CLion's deployment settings.
- Opt-in plugin-managed remote file transfers (**Settings | Tools | GPU Dev Accelerator**) for setups without shared storage; off by default for shared mounts and CLion-synchronized projects.

### Changed
- Toolchain invocations go through local/remote execution adapters that stage artifacts explicitly: upload inputs, download outputs, clean up temporary files.
- Compile and PTX commands pass the project root to nvcc as an include directory so repository-relative headers resolve.
- Background tool failures are surfaced in the GPU Dev console and as a notification instead of being swallowed.

### Fixed
- Show PTX kept nvcc's stderr when the generated artifact could not be read, and the temporary `.ptx` file is now always removed.

## [2026.9.1] - 2026-11-12

### Removed
- The "Metal SDK (xcrun -sdk)" setting: the plugin never invokes `xcrun` or the Metal compiler, so the value had no effect. Metal support is editor-only (highlighting, completion, documentation, Migrate Kernel).

## [2026.9.0] - 2026-11-11

### Fixed
- The unchecked-CUDA-call inspection treated any preceding line containing the letters "if" or "return" (e.g. "verify", "returned") as error handling; it now matches whole keywords only.

### Removed
- CUDA/Metal run configuration types that could not run anything (no settings, no launch state).
- The CUDA "debug runner" and line-breakpoint type: debug launches only ran the program and breakpoints never hit.
- Outdated getting-started guide that described debugger, Vulkan and AI features the plugin does not have.

## [2026.8.0] - 2026-11-10

### Changed
- The plugin is now completely free — all features unlocked, no license needed.
- Source code is now open source under the Apache License 2.0: https://github.com/JirakJ/gpu-dev-accelerator-ultimate

## [2026.5.0] - 2026-07-30

### Production overhaul — every feature now backed by a real tool or deterministic analysis

### Added
- **GPU Dev console** (tool window) with clickable `file:line` links from nvcc/compute-sanitizer output — replaces all modal result dialogs
- **Show PTX** — compile the current `.cu` file to PTX and open it in the editor
- **Occupancy Calculator** — theoretical SM occupancy for CC 7.0–9.0 with limiting-factor breakdown (warps/registers/shared memory/block cap)
- **Migrate Kernel** action — CUDA ⇄ Metal/OpenCL/HIP source-port starting points, warnings in console
- **Editor basics** for all three languages: Ctrl+/ commenting, brace matching, quick documentation (Ctrl+Q) for 100+ builtins
- **8 live templates**: `cukernel`, `cugridstride`, `cucheck`, `culaunch`, `cushared`, `cutime`, `mtlkernel`, `clkernel`
- **Unchecked CUDA API call** inspection with a "Wrap in CUDA_CHECK" quick fix (inserts the macro when missing)
- **Settings page**: nvcc/compute-sanitizer/ncu paths, `-arch`, extra nvcc flags, Metal SDK, tool timeout

### Changed
- **Compile CUDA** now runs in the background (no more EDT freeze), respects configured `-arch`/flags, parses error counts into a notification
- **GPU Information** reads real data: nvidia-smi device query, Apple GPUs via `system_profiler`, nvcc version — into the console, not a dialog
- **Memory Check** runs compute-sanitizer (fallback cuda-memcheck) on a chosen executable and parses the error summary
- **Profile Executable** runs Nsight Compute (`ncu --set basic`) or nvprof and streams the report
- **Multi-GPU Topology** renders the actual topology from `nvidia-smi topo -m` (NV#/PIX/PHB/SYS links) with live utilization/memory/power
- Memory-leak and kernel-heuristic inspections now register real editor problems (previously they reported nothing)
- Code completion is catalog-based with API signatures (CUDA 50+, Metal 30+, OpenCL 25+ entries)

### Removed
- All simulated/mock "premium" outputs: fake profiler metrics, fake flame graph, fake memory reports, fake 4×RTX-4090 topology, fake distributed-GPU cluster manager, fake performance-regression tester, fake advanced debugger

## [2026.4.2] - 2026-04-18

### Changed
- Enabled freemium licensing via `<product-descriptor optional="true">`. Plugin remains fully functional without a license; premium features may be introduced in future minor releases.


All notable changes to GPU Dev Accelerator Ultimate will be documented in this file.

## [2026.3.7] - 2026-03-27

### Added
- **Freemium licensing** — Added `product-descriptor` for JetBrains Marketplace freemium model (code: `PGPUDEVACCELERA`)

## [2026.3.6] - 2026-03-13

### 🐛 Critical Bug Fix

- **Fixed: OpenCL files crash editor on open** — The OpenCL, CUDA, and Metal lexers had a critical bug where `getTokenType()` always returned `null`, causing `LexerEditorHighlighter` to throw `IllegalStateException: Unexpected termination offset`. This made `.cl`, `.cu`, `.cuh`, and `.metal` files completely unopenable.

### ✨ Improvements

- **Complete lexer rewrite** — All three GPU language lexers (CUDA, Metal, OpenCL) now properly tokenize source code with full support for:
  - Keywords, types, qualifiers, and built-in functions with semantic highlighting
  - Line comments (`//`) and block comments (`/* */`) with state restoration
  - Preprocessor directives with line continuation support
  - String and character literals with escape sequences
  - Numeric literals (decimal, hex, binary, float, scientific notation)
  - Multi-character operators (`<<`, `>>`, `<<<`, `>>>`, `==`, `!=`, etc.)
- **Proper syntax highlighting** — Token types now map to distinct editor colors (keywords, types, built-ins, comments, strings, numbers, preprocessor, operators)
- **OpenCL code completion** — Added code completion for OpenCL qualifiers, work-item functions, vector types, math functions, atomic operations, and image functions
- **Shared lexer infrastructure** — Introduced `CLikeLexer` base class for maintainable, consistent tokenization across all three GPU languages

## [1.0.0] - 2024-11-16

### 🎉 Initial Release

#### Features

**Core Language Support:**
- ✅ CUDA C/C++ syntax highlighting and parsing
- ✅ Metal Shading Language full support
- ✅ OpenCL syntax highlighting
- ✅ Smart code completion for all GPU languages
- ✅ File type icons and associations

**Debugging & Profiling:**
- ✅ Advanced CUDA debugging with cuda-gdb integration
- ✅ GPU profiling (nvprof, Nsight Compute)
- ✅ Metal debugger support
- ✅ AI-powered kernel optimization suggestions
- ✅ Memory leak detection (cuda-memcheck)
- ✅ Advanced code completion with signatures
- ✅ Multi-GPU workload visualization
- ✅ Flame graph visualization

**Developer Tools:**
- ✅ GPU Profiler tool window
- ✅ Compile CUDA action (Ctrl+Alt+C)
- ✅ Profile Kernel action (Ctrl+Alt+P)
- ✅ AI Optimize action (Ctrl+Alt+O)
- ✅ GPU Information display
- ✅ Memory Check integration

**Inspections:**
- ✅ CUDA memory leak detection
- ✅ Kernel optimization suggestions
- ✅ Real-time code analysis

**Run Configurations:**
- ✅ CUDA Application run configuration
- ✅ Metal Application run configuration

**Settings:**
- ✅ Compiler path configuration
- ✅ Profiler settings

#### Supported Platforms
- ✅ Windows 10/11
- ✅ macOS 12+ (Intel and Apple Silicon)
- ✅ Linux (Ubuntu, Fedora, etc.)

#### Supported IntelliJ Versions
- IntelliJ IDEA 2023.2+
- IntelliJ IDEA Ultimate
- IntelliJ IDEA Community Edition

#### Known Issues
- Debugger integration requires cuda-gdb to be installed separately
- Metal debugging only available on macOS
- Profiler requires CUDA Toolkit for nvprof/Nsight Compute

#### Documentation
- ✅ Comprehensive README
- ✅ Getting Started guide
- ✅ Business plan
- ✅ Marketing strategy
- ✅ Example CUDA programs
- ✅ Example Metal shaders

---

## Upcoming Features (Roadmap)

### Version 1.1.0 (Q1 2025)
- [ ] Vulkan compute shader support
- [ ] Enhanced AI optimization engine
- [ ] Kernel performance benchmarking
- [ ] Team sharing and collaboration features
- [ ] Integration with CI/CD pipelines

### Version 1.2.0 (Q2 2025)
- [ ] SYCL language support
- [ ] ROCm/HIP for AMD GPUs
- [ ] Advanced memory profiling visualization
- [ ] Multi-GPU topology viewer
- [ ] Cloud GPU integration (AWS, Azure, GCP)

### Version 2.0.0 (Q3 2025)
- [ ] Real-time collaborative debugging
- [ ] Kernel version control and diff
- [ ] AI-powered code generation
- [ ] Performance regression testing
- [ ] Enterprise dashboard and analytics

---

## Version History

| Version | Release Date | Status |
|---------|-------------|---------|
| 1.0.0   | 2024-11-16  | Current |

---

For detailed release notes and migration guides, visit:
https://docs.gpudev.com/changelog

To report issues or request features:
https://github.com/gpudev/issues
