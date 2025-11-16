# Getting Started with GPU Dev Accelerator Ultimate

## Installation

### Prerequisites
- IntelliJ IDEA 2023.2 or later
- For CUDA development: NVIDIA CUDA Toolkit 11.0+
- For Metal development: macOS with Xcode Command Line Tools
- For OpenCL: OpenCL SDK

### Install Plugin

1. Open IntelliJ IDEA
2. Navigate to `Settings/Preferences` → `Plugins`
3. Click `Marketplace` tab
4. Search for "GPU Dev Accelerator Ultimate"
5. Click `Install`
6. Restart IDE when prompted

### Verify Installation

After restart, you should see:
- "GPU Dev" menu in the menu bar
- GPU file type icons (.cu, .metal, .cl)
- GPU Profiler tool window (bottom panel)

## Quick Start Guide

### CUDA Development

#### 1. Create Your First CUDA File

```cuda
#include <cuda_runtime.h>
#include <stdio.h>

__global__ void helloKernel() {
    printf("Hello from GPU thread %d!\n", threadIdx.x);
}

int main() {
    helloKernel<<<1, 10>>>();
    cudaDeviceSynchronize();
    return 0;
}
```

#### 2. Compile

- Right-click on `.cu` file → `Compile CUDA`
- Or use keyboard shortcut: `Ctrl+Alt+C` (Windows/Linux) or `Cmd+Alt+C` (Mac)

#### 3. Run

```bash
nvcc hello.cu -o hello
./hello
```

#### 4. Debug (Premium Feature)

- Set breakpoints in your CUDA code
- Right-click → `Debug with cuda-gdb`
- Step through kernel execution
- Inspect thread variables

### Metal Development

#### 1. Create Metal Shader

```metal
#include <metal_stdlib>
using namespace metal;

kernel void computeShader(device float* data [[buffer(0)]],
                         uint id [[thread_position_in_grid]]) {
    data[id] = data[id] * 2.0f;
}
```

#### 2. Compile

```bash
xcrun -sdk macosx metal -c shader.metal -o shader.air
xcrun -sdk macosx metallib shader.air -o shader.metallib
```

#### 3. Profile (Premium Feature)

- `Ctrl+Alt+P` to open GPU Profiler
- Run your shader
- View performance metrics

## Feature Tour

### Free Tier Features

#### Syntax Highlighting
- Full CUDA C/C++ syntax support
- Metal Shading Language highlighting
- OpenCL syntax support
- Keyword completion

#### Basic Code Completion
- CUDA keywords (`__global__`, `__device__`, etc.)
- Built-in variables (`threadIdx`, `blockIdx`)
- Runtime functions (`cudaMalloc`, `cudaMemcpy`)

#### Simple Compilation
- nvcc integration
- Metal compiler integration
- Error highlighting

### Individual License Features

#### Advanced Debugging
- cuda-gdb integration
- Breakpoints in kernels
- Variable inspection
- Step execution
- Watch expressions

#### GPU Profiling
- nvprof integration
- Nsight Compute support
- Metal Performance Shaders analysis
- Performance metrics visualization
- Bottleneck identification

#### AI-Powered Optimization
Select CUDA code → `Ctrl+Alt+O` for suggestions:
- Memory coalescing improvements
- Shared memory usage recommendations
- Thread block size optimization
- Occupancy improvements

#### Memory Analysis
- cuda-memcheck integration
- Memory leak detection
- Race condition detection
- Invalid memory access warnings

### Enterprise License Features

#### Team Collaboration
- Shared kernel libraries
- Team performance benchmarks
- Code review tools
- Centralized license management

#### Priority Support
- 4-hour response time
- Phone/video support
- Dedicated account manager
- Custom training

## Common Workflows

### Workflow 1: Develop CUDA Kernel

1. Create `.cu` file
2. Write kernel with syntax highlighting
3. Get code completion for CUDA APIs
4. Compile with `Ctrl+Alt+C`
5. Set breakpoints
6. Debug with cuda-gdb
7. Profile with nvprof
8. Get AI optimization suggestions
9. Iterate and improve

### Workflow 2: Cross-Platform Development

1. Write CUDA kernel for NVIDIA GPUs
2. Write equivalent Metal kernel for Apple devices
3. Use shared host code
4. Test on both platforms
5. Compare performance metrics
6. Optimize for each platform

### Workflow 3: Performance Optimization

1. Write initial kernel
2. Run profiler (`Ctrl+Alt+P`)
3. Identify bottlenecks
4. Get AI suggestions (`Ctrl+Alt+O`)
5. Apply optimizations
6. Re-profile
7. Compare results
8. Repeat until satisfied

## Configuration

### Settings

Navigate to `Settings` → `Tools` → `GPU Dev Accelerator`

#### CUDA Settings
- CUDA Toolkit path: `/usr/local/cuda`
- nvcc compiler options: `-O3 -arch=sm_70`
- cuda-gdb path: `/usr/local/cuda/bin/cuda-gdb`

#### Metal Settings
- Metal compiler path: `/usr/bin/xcrun`
- Optimization level: `-O2`

#### Profiler Settings
- nvprof path
- Nsight Compute path
- Sampling interval
- Metrics to collect

### License Activation

1. Go to `Settings` → `Tools` → `GPU Dev Accelerator`
2. Enter your license key: `GPUDEV-TYPE-XXXXXX-XXXXXX`
3. Click `Activate License`
4. Restart IDE

#### Example License Keys
- Individual: `GPUDEV-IND-ABC123-DEF456`
- Enterprise: `GPUDEV-ENT-GHI789-JKL012`

## Keyboard Shortcuts

| Action | Windows/Linux | macOS |
|--------|---------------|-------|
| Compile CUDA | `Ctrl+Alt+C` | `Cmd+Alt+C` |
| Profile Kernel | `Ctrl+Alt+P` | `Cmd+Alt+P` |
| AI Optimize | `Ctrl+Alt+O` | `Cmd+Alt+O` |
| GPU Info | - | - |
| Memory Check | - | - |

## Tips & Best Practices

### CUDA Development
1. **Always check for errors**
   ```cuda
   cudaError_t err = cudaGetLastError();
   if (err != cudaSuccess) {
       printf("Error: %s\n", cudaGetErrorString(err));
   }
   ```

2. **Use shared memory for frequently accessed data**
   ```cuda
   __shared__ float tile[TILE_SIZE][TILE_SIZE];
   ```

3. **Synchronize when needed**
   ```cuda
   __syncthreads(); // After writing to shared memory
   ```

4. **Optimize thread block size**
   - Try 256 or 512 threads per block
   - Ensure divisibility by warp size (32)

### Metal Development
1. **Use appropriate address spaces**
   - `device` for GPU memory
   - `constant` for read-only data
   - `threadgroup` for shared memory

2. **Leverage texture objects**
   ```metal
   texture2d<float> inputTex [[texture(0)]];
   ```

3. **Profile early and often**
   - Use Metal Performance Shaders
   - Optimize memory bandwidth
   - Minimize thread divergence

## Troubleshooting

### CUDA Compiler Not Found
**Error:** "nvcc: command not found"

**Solution:**
1. Install CUDA Toolkit from NVIDIA
2. Add to PATH:
   ```bash
   export PATH=/usr/local/cuda/bin:$PATH
   ```
3. Verify: `nvcc --version`

### Metal Compiler Not Found
**Error:** "xcrun: error: unable to find utility "metal""

**Solution:**
1. Install Xcode Command Line Tools:
   ```bash
   xcode-select --install
   ```
2. Verify: `xcrun metal --version`

### Premium Features Not Working
**Error:** "This feature requires Individual or Enterprise license"

**Solution:**
1. Check license status in Settings
2. Ensure license is not expired
3. Restart IDE after activation
4. Contact support@gpudev.com if issues persist

### Profiler Not Launching
**Error:** "nvprof not found"

**Solution:**
1. Install CUDA Toolkit (includes nvprof)
2. Configure path in Settings
3. For newer GPUs, use Nsight Compute instead

## Next Steps

### Learn More
- [Advanced CUDA Programming](docs/CUDA_ADVANCED.md)
- [Metal Best Practices](docs/METAL_GUIDE.md)
- [Performance Optimization Guide](docs/OPTIMIZATION.md)
- [API Reference](docs/API.md)

### Get Help
- **Documentation:** https://docs.gpudev.com
- **Community Forum:** https://forum.gpudev.com
- **Email Support:** support@gpudev.com
- **Enterprise Support:** enterprise@gpudev.com

### Upgrade
Ready for advanced features?
- **Individual License:** $200-400/year
- **Enterprise License:** $500-2000/seat/year
- Visit https://gpudev.com/pricing

---

**Happy GPU Programming! 🚀**
