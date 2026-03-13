# Changelog

All notable changes to GPU Dev Accelerator Ultimate will be documented in this file.

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
