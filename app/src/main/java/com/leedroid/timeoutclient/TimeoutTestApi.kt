package com.leedroid.timeoutclient

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit API interface for testing various HTTP timeout scenarios
 */
interface TimeoutTestApi {

    /**
     * Download from a slow server that delays between chunks
     * @param delayBetweenChunks Server-side delay in milliseconds between chunks (default: 6000ms)
     * @return Streaming response body to test read timeouts
     */
    @Streaming
    @GET("/api/download/slow-server")
    suspend fun downloadSlowServer(@Query("delayBetweenChunks") delayBetweenChunks: Long = 6000): ResponseBody

    /**
     * Upload to a slow server that delays between reads
     * @param delayBetweenReads Server-side delay in milliseconds between reads (default: 6000ms)
     * @param file Request body containing file data
     * @return String response from server
     */
    @POST("/api/upload/slow-server")
    suspend fun uploadToSlowServer(
        @Query("delayBetweenReads") delayBetweenReads: Long = 6000,
        @Body file: RequestBody
    ): String


    /**
     * Simple ping endpoint to verify server connectivity
     * @return String response from server
     */
    @GET("/api/ping")
    suspend fun ping(): String
}

