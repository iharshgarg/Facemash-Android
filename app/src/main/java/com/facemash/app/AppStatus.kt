package com.facemash.app

sealed class AppStatus {
    object Loading : AppStatus()
    object Online : AppStatus()
    object NoInternet : AppStatus()
    object ServerDown : AppStatus()
}