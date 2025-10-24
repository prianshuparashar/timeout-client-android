package com.leedroid.timeoutclient

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Object that provides multiple Retrofit client configurations
 * Each client has different timeout settings to test various timeout scenarios
 */
object RetrofitClients {

    // Base URL for emulator (10.0.2.2 maps to host machine's localhost)
    private const val BASE_URL = "http://10.0.2.2:8080"

    /**
     * Client 1: Short Read Timeout
     * Use this to test read timeout scenarios where server is slow to send data
     * - Read timeout: 3 seconds (short - will timeout on slow reads)
     * - Write timeout: 10 seconds (normal)
     * - Connect timeout: 10 seconds (normal)
     */
    val clientShortReadTimeout: TimeoutTestApi by lazy {
        createRetrofitClient(
            readTimeoutSeconds = 3,
            writeTimeoutSeconds = 10,
            loggingTag = "CLIENT-SHORT-READ"
        )
    }

    /**
     * Client 2: Normal Timeouts
     * Standard configuration suitable for most network operations
     * - Read timeout: 30 seconds
     * - Write timeout: 30 seconds
     * - Connect timeout: 10 seconds
     */
    val clientNormalTimeouts: TimeoutTestApi by lazy {
        createRetrofitClient(
            readTimeoutSeconds = 30,
            writeTimeoutSeconds = 30,
            loggingTag = "CLIENT-NORMAL"
        )
    }

    /**
     * Client 3: Short Write Timeout
     * Use this to test write timeout scenarios where server is slow to accept data
     * - Read timeout: 30 seconds (normal)
     * - Write timeout: 3 seconds (short - will timeout on slow writes)
     * - Connect timeout: 10 seconds (normal)
     */
    val clientShortWriteTimeout: TimeoutTestApi by lazy {
        createRetrofitClient(
            readTimeoutSeconds = 30,
            writeTimeoutSeconds = 3,
            loggingTag = "CLIENT-SHORT-WRITE"
        )
    }

    /**
     * Client 4: Long Timeouts
     * Use this for operations that legitimately take a long time
     * - Read timeout: 60 seconds (long - allows slow operations)
     * - Write timeout: 60 seconds (long - allows slow operations)
     * - Connect timeout: 10 seconds (normal)
     */
    val clientLongTimeouts: TimeoutTestApi by lazy {
        createRetrofitClient(
            readTimeoutSeconds = 60,
            writeTimeoutSeconds = 60,
            loggingTag = "CLIENT-LONG"
        )
    }

    /**
     * Creates a Retrofit client with specified timeout configurations
     * Connect timeout is fixed at 10 seconds for all clients
     */
    private fun createRetrofitClient(
        readTimeoutSeconds: Long,
        writeTimeoutSeconds: Long,
        loggingTag: String
    ): TimeoutTestApi {

        // Create logging interceptor with custom tag
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            android.util.Log.d(loggingTag, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Build OkHttpClient with specified timeouts
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS) // Fixed at 10 seconds for all clients
            .addInterceptor(loggingInterceptor)
            .build()

        // Build Retrofit instance with both Scalars and Gson converters
        val retrofit = Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create()).build()

        return retrofit.create(TimeoutTestApi::class.java)
    }
}

