package com.health.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.health.tracker.di.AppContainer
import com.health.tracker.di.HealthApplication

/**
 * 手写容器 → ViewModel 的唯一桥：ViewModel 只接 AppContainer，不认识 Android。
 * 不用 viewModelFactory DSL，因为它要求 reified 类型参数，无法写成普通泛型函数。
 */
fun containerFactory(build: (AppContainer) -> ViewModel): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application =
                extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HealthApplication
            @Suppress("UNCHECKED_CAST")
            return build(application.container) as T
        }
    }
