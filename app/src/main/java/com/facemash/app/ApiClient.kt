package com.facemash.app

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

object ApiClient {

    private const val PREFS_NAME = "cookies"
    private const val COOKIE_KEY = "cookie"

    lateinit var client: OkHttpClient
        private set

    // ✅ application context stored safely
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val cookieJar = object : CookieJar {

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (cookies.isNotEmpty()) {
                    prefs.edit()
                        .putString(COOKIE_KEY, cookies[0].toString())
                        .apply()
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookieString = prefs.getString(COOKIE_KEY, null)
                return if (cookieString != null) {
                    listOfNotNull(Cookie.parse(url, cookieString))
                } else {
                    emptyList()
                }
            }
        }

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
    }

    fun getCookieHeader(): String? {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(COOKIE_KEY, null)
    }

    fun clearCookies() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    const val BASE_URL = "https://facemash.in"
}