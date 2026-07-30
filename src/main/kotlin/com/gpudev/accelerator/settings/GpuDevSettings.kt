/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * Application-level settings for GPU toolchain integration.
 * Empty paths mean "resolve from PATH".
 */
@Service(Service.Level.APP)
@State(
    name = "com.jakubjirak.gpudev.ultimate.Settings",
    storages = [Storage("com-jakubjirak-gpudev-ultimate.xml")]
)
class GpuDevSettings : PersistentStateComponent<GpuDevSettings.State> {

    data class State(
        var nvccPath: String = "",
        var gpuArch: String = "",
        var extraNvccFlags: String = "",
        var sanitizerPath: String = "",
        var ncuPath: String = "",
        var metalSdk: String = "macosx",
        var toolTimeoutSeconds: Int = 300
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): GpuDevSettings = service()
    }
}
