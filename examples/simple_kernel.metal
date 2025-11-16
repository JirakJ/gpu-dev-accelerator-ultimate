#include <metal_stdlib>
using namespace metal;

kernel void vectorAdd(device const float* a [[buffer(0)]],
                      device const float* b [[buffer(1)]],
                      device float* c [[buffer(2)]],
                      uint id [[thread_position_in_grid]]) {
    c[id] = a[id] + b[id];
}

kernel void matrixMultiply(device const float* A [[buffer(0)]],
                          device const float* B [[buffer(1)]],
                          device float* C [[buffer(2)]],
                          constant uint& width [[buffer(3)]],
                          uint2 gid [[thread_position_in_grid]]) {
    
    uint row = gid.y;
    uint col = gid.x;
    
    if (row >= width || col >= width) return;
    
    float sum = 0.0f;
    for (uint k = 0; k < width; k++) {
        sum += A[row * width + k] * B[k * width + col];
    }
    
    C[row * width + col] = sum;
}
