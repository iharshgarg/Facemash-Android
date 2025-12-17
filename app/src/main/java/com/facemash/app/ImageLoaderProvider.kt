package com.facemash.app

import android.content.Context
import coil.ImageLoader

object ImageLoaderProvider {

    fun get(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient { ApiClient.client } // 🔑 share cookies
            .build()
    }
}