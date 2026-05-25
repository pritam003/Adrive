package com.example.adrive.data.network

import android.content.Context
import com.example.adrive.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Persists cookies across app restarts using SharedPreferences.
 * This ensures the JWT auth_token cookie survives app closure.
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("adrive_cookies", Context.MODE_PRIVATE)
    private val store = HashMap<String, MutableList<Cookie>>()

    init {
        // Restore persisted cookies on startup
        prefs.all.entries.forEach { (key, value) ->
            if (value is String) {
                val parts = key.split("@", limit = 2)
                if (parts.size == 2) {
                    val host = parts[1]
                    val fakeUrl = "https://$host/".toHttpUrlOrNull() ?: return@forEach
                    Cookie.parse(fakeUrl, value)?.let { cookie ->
                        store.getOrPut(host) { mutableListOf() }.add(cookie)
                    }
                }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        store[host] = cookies.toMutableList()
        val editor = prefs.edit()
        // Clear old cookies for this host first
        prefs.all.keys.filter { it.endsWith("@$host") }.forEach { editor.remove(it) }
        cookies.forEach { cookie ->
            // Store cookie as its Set-Cookie string representation
            editor.putString("${cookie.name}@$host", cookie.toString())
        }
        editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host]?.filter { cookie ->
            cookie.expiresAt > System.currentTimeMillis()
        } ?: emptyList()
    }

    fun clearAll() {
        store.clear()
        prefs.edit().clear().apply()
    }

    fun hasAuthCookie(): Boolean {
        return store.values.flatten().any { it.name == "auth_token" }
    }
}

/**
 * Singleton API client shared across the app.
 */
object ApiClient {
    private lateinit var cookieJar: PersistentCookieJar
    private lateinit var okHttpClient: OkHttpClient

    lateinit var service: ApiService
        private set

    fun init(context: Context) {
        cookieJar = PersistentCookieJar(context.applicationContext)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        service = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun getOkHttpClient(): OkHttpClient = okHttpClient
    fun getCookieJar(): PersistentCookieJar = cookieJar

    /** Upload a file directly to Azure Blob Storage via SAS URL (bypasses the API). */
    suspend fun uploadToAzure(
        sasUrl: String,
        contentUri: android.net.Uri,
        contentType: String,
        contentLength: Long,
        onProgress: (Long, Long) -> Unit
    ) {
        val resolver = cookieJar  // just for context ref (not used here)
        // Actual upload is done in UploadWorker using the raw OkHttpClient
    }
}

