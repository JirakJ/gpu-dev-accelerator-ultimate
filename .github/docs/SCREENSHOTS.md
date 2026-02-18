# GPU Dev Accelerator Ultimate — Screenshot Plan

## Screenshots

### 1. CUDA Syntax Highlighting
**What to capture:** A `.cu` file open in the editor showing a CUDA kernel with colorful syntax highlighting. Include kernel launch syntax (`<<<blocks, threads>>>`) and CUDA-specific keywords highlighted.

### 2. Code Completion for CUDA API
**What to capture:** The code completion popup showing CUDA Runtime API suggestions (e.g., `cudaMalloc`, `cudaMemcpy`, `cudaDeviceSynchronize`) with parameter hints and documentation.

### 3. cuda-gdb Debugger Session
**What to capture:** A debugging session with a breakpoint inside a CUDA kernel. Show the debug panel with thread variables, the call stack, and the GPU thread selector.

### 4. GPU Profiler Tool Window
**What to capture:** The GPU Profiler bottom panel showing a flame graph of kernel execution times, with hotspots highlighted in red. Include timing data and percentage breakdowns.

### 5. AI Kernel Optimization Suggestions
**What to capture:** The AI optimization panel showing specific suggestions for a CUDA kernel — such as improving memory coalescing, reducing warp divergence, or increasing occupancy.

### 6. Metal Shader Editing
**What to capture:** A `.metal` file open in the editor with Metal Shading Language syntax highlighting. Show a compute kernel with Metal-specific attributes and types.

### 7. Multi-GPU Topology View
**What to capture:** The GPU topology visualization showing multiple GPU devices with their properties (name, memory, compute capability) and workload distribution bars.

### 8. Memory Analysis Results
**What to capture:** The memory check results panel showing detected memory leaks with file locations, allocation sizes, and suggestions for fixing them.

---

## Video Demo Outline (60 seconds)

| Time | Scene | Description |
|------|-------|-------------|
| 0:00–0:08 | Intro | Plugin name and tagline. Show the GPU Dev menu in the IDE menubar. |
| 0:08–0:18 | CUDA editing | Open a .cu file, show syntax highlighting and code completion for CUDA APIs |
| 0:18–0:28 | Compile and run | Compile with Ctrl+Alt+C, show the output panel with compilation results |
| 0:28–0:38 | Debugging | Set a breakpoint in a kernel, start cuda-gdb debugger, inspect thread variables |
| 0:38–0:48 | Profiling | Run the profiler, show the flame graph with kernel execution hotspots |
| 0:48–0:53 | AI optimization | Show AI optimization suggestions for the kernel |
| 0:53–0:60 | Outro | Show pricing tiers, supported platforms (CUDA, Metal, OpenCL), and install CTA |
