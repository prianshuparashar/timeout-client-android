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
     * Download a large file to test read timeout with large payloads
     * @return Streaming response body containing large data
     */
    @Streaming
    @GET("/api/download/large-file")
    suspend fun downloadLargeFile(): ResponseBody

    /**
     * Download endpoint that expects slow client consumption
     * Tests SERVER write timeout when client reads data slowly
     * @return Streaming response body
     */
    @Streaming
    @GET("/api/download/expect-slow-client")
    suspend fun downloadExpectSlowClient(): ResponseBody

    /**
     * Download endpoint to test server write timeout
     * Server sends data and may timeout if client reads too slowly
     * @return Streaming response body
     */
    @Streaming
    @GET("/api/download/test-server-write-timeout")
    suspend fun downloadTestServerWriteTimeout(): ResponseBody

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
     * Upload with slow server response
     * Server processes upload normally but delays before sending response
     * Tests CLIENT read timeout while waiting for response after upload
     * @param delayBeforeResponse Server-side delay in milliseconds before sending response (default: 8000ms)
     * @param file Request body containing file data
     * @return String response from server
     */
    @POST("/api/upload/slow-response")
    suspend fun uploadSlowResponse(
        @Query("delayBeforeResponse") delayBeforeResponse: Long = 8000,
        @Body file: RequestBody
    ): String

    /**
     * Normal upload endpoint without delays
     * @param file Request body containing file data
     * @return String response from server
     */
    @POST("/api/upload/normal")
    suspend fun uploadNormal(@Body file: RequestBody): String

    /**
     * Upload endpoint that expects fast client
     * Tests SERVER read timeout when client sends data slowly
     * Server has short connection-timeout configured
     * @param file Request body containing file data
     * @return String response from server
     */
    @POST("/api/upload/expect-fast-client")
    suspend fun uploadExpectFastClient(@Body file: RequestBody): String

    /**
     * Simple ping endpoint to verify server connectivity
     * @return String response from server
     */
    @GET("/api/ping")
    suspend fun ping(): String
}

