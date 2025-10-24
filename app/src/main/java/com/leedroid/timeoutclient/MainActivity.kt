package com.leedroid.timeoutclient

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.leedroid.timeoutclient.ui.theme.TimeoutClientTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException

class MainActivity : ComponentActivity() {

    private var logText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TimeoutClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TestResultsView(
                        logText = logText,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Start tests when activity is created
        startTests()
    }

    /**
     * Main test execution flow
     */
    private fun startTests() {
        lifecycleScope.launch {
            log("HTTP Timeout Test Suite")
            log("=" * 80)
            log("Testing all timeout scenarios in HTTP communication")
            log("=" * 80)
            log("")

            // Test 0: Verify server connectivity
            testPing()
            log("")

            // ========== DOWNLOAD SCENARIOS ==========

            // DOWNLOAD - CLIENT READ TIMEOUT
            log("=" * 80)
            log("SCENARIO 1: DOWNLOAD - CLIENT READ TIMEOUT")
            log("=" * 80)
            log("When: Server sends data slowly")
            log("Where: CLIENT side timeout")
            log("Why: Client is waiting to READ response data from server")
            log("")

            testCase1_Download_ClientReadTimeout_Fail()
            log("")

            testCase2_Download_ClientReadTimeout_Fixed()
            log("")

            // ========== UPLOAD SCENARIOS ==========

            // UPLOAD - CLIENT WRITE TIMEOUT
            log("=" * 80)
            log("SCENARIO 2: UPLOAD - CLIENT WRITE TIMEOUT")
            log("=" * 80)
            log("When: Server reads/consumes data slowly")
            log("Where: CLIENT side timeout")
            log("Why: Client's write() blocks because TCP buffer full (server not consuming)")
            log("")

            testCase3_Upload_ClientWriteTimeout_Fail()
            log("")

            testCase4_Upload_ClientWriteTimeout_Fixed()
            log("")

            log("=" * 80)
            log("ALL TESTS COMPLETED")
            log("=" * 80)
            log("")
            log("SUMMARY:")
            log("✓ Download scenario: Client Read Timeout")
            log("✓ Upload scenario: Client Write Timeout")
            log("")
            log("NOTE: Server-side timeouts (Server Write & Server Read) are best tested")
            log("      on the server side with proper monitoring and configuration.")

            /*
            // ========== SERVER-SIDE TIMEOUT TESTS (COMMENTED OUT) ==========
            // These tests are better performed on the server side

            // DOWNLOAD - SERVER WRITE TIMEOUT
            log("=" * 80)
            log("SCENARIO 2: DOWNLOAD - SERVER WRITE TIMEOUT")
            log("=" * 80)
            log("When: Client reads/consumes data slowly (rare scenario)")
            log("Where: SERVER side timeout")
            log("Why: Server's write() blocks because TCP buffer full (client not consuming)")
            log("")

            testCase2a_Download_ServerWriteTimeout_ClientSlowReading()
            log("")

            testCase2b_Download_ServerWriteTimeout_ClientNormalReading()
            log("")

            // UPLOAD - SERVER READ TIMEOUT
            log("=" * 80)
            log("SCENARIO 4: UPLOAD - SERVER READ TIMEOUT")
            log("=" * 80)
            log("When: Client sends data slowly")
            log("Where: SERVER side timeout")
            log("Why: Server is waiting to READ request body data from client")
            log("Note: This requires server-side connection-timeout configuration")
            log("")

            testCase4a_Upload_ServerReadTimeout_SlowClient()
            log("")

            testCase4b_Upload_ServerReadTimeout_NormalClient()
            log("")

            // ========== ADDITIONAL SCENARIOS (COMMENTED OUT) ==========

            // UPLOAD - CLIENT READ TIMEOUT (waiting for response)
            log("=" * 80)
            log("SCENARIO 5: UPLOAD - CLIENT READ TIMEOUT (after upload)")
            log("=" * 80)
            log("When: After upload completes, waiting for server's response")
            log("Where: CLIENT side timeout")
            log("Why: Server is slow to process and send response")
            log("")

            testCase5_Upload_ClientReadTimeout_Fail()
            log("")

            testCase6_Upload_ClientReadTimeout_Fixed()
            log("")

            // LARGE FILE TEST
            log("=" * 80)
            log("BONUS TEST - LARGE FILE DOWNLOAD")
            log("=" * 80)
            log("")

            testCase7_DownloadLargeFile()
            log("")

            // ADDITIONAL SERVER WRITE TIMEOUT TEST
            log("=" * 80)
            log("ADDITIONAL: DOWNLOAD - SERVER WRITE TIMEOUT (legacy)")
            log("=" * 80)
            log("")

            testCase8_Download_ServerWriteTimeout()
            log("")

            log("✓ Additional: Client Read Timeout waiting for response after upload")
            */
        }
    }

    /**
     * Test 0: Ping server to verify connectivity
     */
    private suspend fun testPing() {
        log("[TEST] Ping Server")
        log("Expected: Successful connection to verify server is running")

        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientNormalTimeouts.ping()
            }
            log("✓ CORRECT: Server is reachable")
            log("  → Response: $response")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Cannot reach server")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
            log("  → Make sure server is running on localhost:8080")
        }
    }

    /**
     * Helper function to log messages and update UI
     */
    private fun log(message: String) {
        logText += message + "\n"
        Log.d("%--PRINT--%", message)
    }

    /**
     * String repetition operator for decorative lines
     */
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }

    /**
     * Test Case 1: Download - CLIENT Read Timeout FAILS
     * Server sends data slowly (delays 6s between chunks)
     * Client has SHORT 3 second read timeout
     * Expected: SocketTimeoutException (CLIENT read timeout)
     */
    private suspend fun testCase1_Download_ClientReadTimeout_Fail() {
        log("[TEST 1] Download - CLIENT Read Timeout FAILS")
        log("Expected: ✗ SocketTimeoutException - CLIENT read timeout")
        log("Configuration: CLIENT readTimeout=3s, SERVER delayBetweenChunks=6s")
        log("Scenario: Server sends data slowly, client times out waiting to READ")

        try {
            val totalBytes = withContext(Dispatchers.IO) {
                val responseBody = RetrofitClients.clientShortReadTimeout.downloadSlowServer(delayBetweenChunks = 6000)

                val inputStream = responseBody.byteStream()
                val buffer = ByteArray(1024) // 1KB chunks
                var bytes = 0L

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    bytes += bytesRead
                }
                bytes
            }

            log("✗ UNEXPECTED: Download succeeded (should have timed out)")
            log("  → Total bytes received: $totalBytes")
            log("  → This means the timeout configuration is NOT working")
        } catch (e: SocketTimeoutException) {
            log("✓ CORRECT: CLIENT Read timeout occurred as expected")
            log("  → SocketTimeoutException: ${e.message}")
            log("  → Where: CLIENT side")
            log("  → Why: Client waiting to READ data from slow server")
            log("  → Server delay (6s) > Client readTimeout (3s)")
            log("  → Fix: Increase CLIENT readTimeout to > 6 seconds")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Different exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 2: Download - CLIENT Read Timeout FIXED
     * Server sends data slowly (delays 6s between chunks)
     * Client has LONG 60 second read timeout
     * Expected: Success (no timeout)
     */
    private suspend fun testCase2_Download_ClientReadTimeout_Fixed() {
        log("[TEST 2] Download - CLIENT Read Timeout FIXED")
        log("Expected: ✓ Success - increased CLIENT read timeout handles server delay")
        log("Configuration: CLIENT readTimeout=60s, SERVER delayBetweenChunks=6s")
        log("Scenario: Same slow server, but client has longer timeout")

        try {
            val totalBytes = withContext(Dispatchers.IO) {
                val responseBody = RetrofitClients.clientLongTimeouts.downloadSlowServer(delayBetweenChunks = 6000)

                val inputStream = responseBody.byteStream()
                val buffer = ByteArray(1024) // 1KB chunks
                var bytes = 0L

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    bytes += bytesRead
                }
                bytes
            }

            log("✓ CORRECT: Download succeeded with increased CLIENT timeout")
            log("  → Total bytes received: $totalBytes")
            log("  → Client readTimeout (60s) > Server delay (6s)")
            log("  → This proves increasing CLIENT readTimeout fixes the issue")
        } catch (e: SocketTimeoutException) {
            log("✗ UNEXPECTED: Timeout occurred (shouldn't happen with 60s timeout)")
            log("  → SocketTimeoutException: ${e.message}")
            log("  → Server delay may be longer than expected")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 2a: Download - SERVER Write Timeout (Client reads slowly)
     * Client intentionally reads very slowly to cause server's write() to block
     * Expected: Server's write may timeout or block for long time
     */
    private suspend fun testCase2a_Download_ServerWriteTimeout_ClientSlowReading() {
        log("[TEST 2a] Download - SERVER Write Timeout (client reads slowly)")
        log("Expected: Demonstrates server-side write blocking")
        log("Configuration: Client deliberately reads slowly (2s delay per chunk)")
        log("Scenario: Client's slow consumption causes server's write() to block")

        try {
            val result = withContext(Dispatchers.IO) {
                val responseBody = RetrofitClients.clientNormalTimeouts.downloadTestServerWriteTimeout()
                val inputStream = responseBody.byteStream()
                val buffer = ByteArray(64 * 1024) // 64KB buffer
                var totalBytes = 0L
                var chunkCount = 0

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    totalBytes += bytesRead
                    chunkCount++

                    // Read slowly - delay after each read
                    if (chunkCount % 5 == 0) {
                        log("  → Client read ${totalBytes / 1024}KB, delaying 2s...")
                        Thread.sleep(2000) // 2 second delay
                    }
                }

                totalBytes
            }

            log("✓ Download completed: ${result / 1024}KB")
            log("  → Server's write() was blocking while waiting for client to consume")
            log("  → If server had aggressive write timeout, this would fail")
            log("  → Check server logs for write durations")
        } catch (e: Exception) {
            log("✗ Exception occurred: ${e.javaClass.simpleName}")
            log("  → ${e.message}")
            log("  → This might indicate server write timeout occurred")
        }
    }

    /**
     * Test Case 2b: Download - Normal client reading (no server timeout)
     * Client reads at normal speed
     * Expected: Success - server write completes normally
     */
    private suspend fun testCase2b_Download_ServerWriteTimeout_ClientNormalReading() {
        log("[TEST 2b] Download - Normal Reading (no timeout)")
        log("Expected: ✓ Success - client reads normally")
        log("Configuration: No delays in client reading")
        log("Scenario: Normal download completes successfully")

        try {
            val result = withContext(Dispatchers.IO) {
                val responseBody = RetrofitClients.clientNormalTimeouts.downloadTestServerWriteTimeout()
                val inputStream = responseBody.byteStream()
                val buffer = ByteArray(512 * 1024) // 512KB buffer
                var totalBytes = 0L

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    totalBytes += bytesRead
                }

                totalBytes
            }

            log("✓ CORRECT: Download completed successfully: ${result / 1024}KB")
            log("  → Normal client reading doesn't cause server write blocking")
            log("  → Server write timeout not triggered")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 3: Upload - CLIENT Write Timeout FAILS
     * Server reads/consumes data slowly (delays 6s between reads)
     * Client has SHORT 3 second write timeout
     * Expected: SocketTimeoutException (CLIENT write timeout)
     */
    private suspend fun testCase3_Upload_ClientWriteTimeout_Fail() {
        log("[TEST 3] Upload - CLIENT Write Timeout FAILS")
        log("Expected: ✗ SocketTimeoutException - CLIENT write timeout")
        log("Configuration: CLIENT writeTimeout=3s, SERVER delayBetweenReads=6s")
        log("Scenario: Server reads slowly, client times out trying to WRITE")

        try {
            val fileData = ByteArray(5 * 1024 * 1024) { it.toByte() } // 5MB
            val requestBody = fileData.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientShortWriteTimeout.uploadToSlowServer(
                    delayBetweenReads = 6000,
                    file = requestBody
                )
            }

            log("✗ UNEXPECTED: Upload succeeded (should have timed out)")
            log("  → Response: $response")
            log("  → This means the timeout configuration is NOT working")
        } catch (e: SocketTimeoutException) {
            log("✓ CORRECT: CLIENT Write timeout occurred as expected")
            log("  → SocketTimeoutException: ${e.message}")
            log("  → Where: CLIENT side")
            log("  → Why: Client's write() blocked (TCP buffer full, server not consuming)")
            log("  → Server delay (6s) > Client writeTimeout (3s)")
            log("  → Fix: Increase CLIENT writeTimeout to > 6 seconds")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Different exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 4: Upload - CLIENT Write Timeout FIXED
     * Server reads/consumes data slowly (delays 6s between reads)
     * Client has LONG 60 second write timeout
     * Expected: Success (no timeout)
     */
    private suspend fun testCase4_Upload_ClientWriteTimeout_Fixed() {
        log("[TEST 4] Upload - CLIENT Write Timeout FIXED")
        log("Expected: ✓ Success - increased CLIENT write timeout handles server delay")
        log("Configuration: CLIENT writeTimeout=60s, SERVER delayBetweenReads=6s")
        log("Scenario: Same slow server, but client has longer timeout")

        try {
            val fileData = ByteArray(5 * 1024 * 1024) { it.toByte() } // 5MB
            val requestBody = fileData.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientLongTimeouts.uploadToSlowServer(
                    delayBetweenReads = 6000,
                    file = requestBody
                )
            }

            log("✓ CORRECT: Upload succeeded with increased CLIENT timeout")
            log("  → Response: $response")
            log("  → Client writeTimeout (60s) > Server total delay")
            log("  → This proves increasing CLIENT writeTimeout fixes the issue")
        } catch (e: SocketTimeoutException) {
            log("✗ UNEXPECTED: Timeout occurred (shouldn't happen with 60s timeout)")
            log("  → SocketTimeoutException: ${e.message}")
            log("  → Server delay may be longer than expected")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 4a: Upload - SERVER Read Timeout (slow client sending)
     * Client uploads slowly using a custom interceptor
     * Expected: Server times out waiting for data (connection-timeout)
     */
    private suspend fun testCase4a_Upload_ServerReadTimeout_SlowClient() {
        log("[TEST 4a] Upload - SERVER Read Timeout (slow client)")
        log("Expected: Server closes connection due to READ timeout")
        log("Configuration: Server connection-timeout=5s, client sends slowly")
        log("Scenario: Client delays sending data, server times out reading")
        log("Note: This test uses a slow-upload client with custom throttling")

        try {
            // Small file but we'll send it slowly
            val fileData = ByteArray(50 * 1024) { it.toByte() } // 50KB
            val requestBody = fileData.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            // This will likely fail because we can't easily throttle OkHttp's upload speed
            // The client sends data as fast as TCP allows
            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientNormalTimeouts.uploadExpectFastClient(requestBody)
            }

            log("✗ UNEXPECTED: Upload succeeded")
            log("  → Response: $response")
            log("  → Note: Hard to simulate slow client upload from application layer")
            log("  → TCP handles the upload speed, not the application")
        } catch (e: Exception) {
            log("✓ Exception occurred (may indicate server timeout)")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
            log("  → If server connection-timeout is low (5s) and network is slow,")
            log("  → server's READ timeout occurs waiting for client data")
            log("  → Fix: Increase SERVER connection-timeout")
        }
    }

    /**
     * Test Case 4b: Upload - Normal client (no server timeout)
     * Client uploads at normal speed
     * Expected: Success
     */
    private suspend fun testCase4b_Upload_ServerReadTimeout_NormalClient() {
        log("[TEST 4b] Upload - Normal Upload Speed (no server timeout)")
        log("Expected: ✓ Success - upload completes within server timeout")
        log("Configuration: Server connection-timeout=5s, client sends normally")
        log("Scenario: Normal upload completes successfully")

        try {
            val fileData = ByteArray(100 * 1024) { it.toByte() } // 100KB
            val requestBody = fileData.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientNormalTimeouts.uploadExpectFastClient(requestBody)
            }

            log("✓ CORRECT: Upload completed successfully")
            log("  → Response: $response")
            log("  → Client sent data fast enough")
            log("  → Server's READ timeout not triggered")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 8: Download - SERVER Write Timeout
     * Client reads data slowly to fill server's TCP send buffer
     * This can cause server-side write timeout
     * Expected: Demonstrates server-side write timeout scenario
     */
    private suspend fun testCase8_Download_ServerWriteTimeout() {
        log("[TEST 8] Download - SERVER Write Timeout (client reads slowly)")
        log("Expected: Demonstrates server-side write timeout scenario")
        log("Configuration: Client reads slowly to fill server's TCP send buffer")
        log("Scenario: Client throttles reading to cause server's write() to block")

        try {
            withContext(Dispatchers.IO) {
                val response = RetrofitClients.clientNormalTimeouts.downloadExpectSlowClient()

                val inputStream = response.byteStream()
                val buffer = ByteArray(1024) // Small buffer
                var totalBytes = 0L
                var chunkCount = 0

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    totalBytes += bytesRead
                    chunkCount++

                    // SLOW DOWN CLIENT READING - delay after each read
                    if (chunkCount % 100 == 0) {
                        Thread.sleep(500) // Delay to slow down consumption
                    }
                }

                log("✓ Download completed: $totalBytes bytes")
                log("  → If server had short writeTimeout, it would timeout here")
                log("  → Server's write() blocks when client reads slowly")
                log("  → Fix: Increase SERVER writeTimeout or speed up client consumption")
            }
        } catch (e: SocketTimeoutException) {
            log("✗ Timeout occurred")
            log("  → ${e.message}")
        } catch (e: Exception) {
            log("✗ Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /*
    // ========== ADDITIONAL TEST CASES (COMMENTED OUT) ==========
    // Uncomment these test cases if you want to run additional scenarios

    /**
     * Test Case 5: Upload - CLIENT Read Timeout FAILS (waiting for response)
     * After uploading, client waits for server response
     * Server delays before responding
     * Client has SHORT 3 second read timeout
     * Expected: SocketTimeoutException (CLIENT read timeout waiting for response)
     */
    private suspend fun testCase5_Upload_ClientReadTimeout_Fail() {
        log("[TEST 5] Upload - CLIENT Read Timeout FAILS (waiting for response)")
        log("Expected: ✗ SocketTimeoutException - CLIENT read timeout")
        log("Configuration: CLIENT readTimeout=3s, SERVER delays response by 8s")
        log("Scenario: After upload, client times out waiting to READ server's response")

        try {
            val fileData = ByteArray(10 * 1024) { it.toByte() } // Small 10KB file
            val requestBody = fileData.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientShortReadTimeout.uploadSlowResponse(
                    delayBeforeResponse = 8000, // Server delays 8s before responding
                    file = requestBody
                )
            }

            log("✗ UNEXPECTED: Got response (should have timed out)")
            log("  → Response: $response")
        } catch (e: SocketTimeoutException) {
            log("✓ CORRECT: CLIENT Read timeout occurred as expected")
            log("  → SocketTimeoutException: ${e.message}")
            log("  → Where: CLIENT side")
            log("  → Why: Timeout while WAITING to READ server's response")
            log("  → Server delay (8s) > Client readTimeout (3s)")
            log("  → Fix: Increase CLIENT readTimeout to handle slow responses")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Different exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 6: Upload - CLIENT Read Timeout FIXED (waiting for response)
     * After uploading, client waits for server response
     * Client has LONG 60 second read timeout
     * Expected: Success (gets response even if server is slow)
     */
    private suspend fun testCase6_Upload_ClientReadTimeout_Fixed() {
        log("[TEST 6] Upload - CLIENT Read Timeout FIXED (waiting for response)")
        log("Expected: ✓ Success - increased CLIENT read timeout handles slow response")
        log("Configuration: CLIENT readTimeout=60s, SERVER delays response by 8s")
        log("Scenario: After upload, client successfully waits for server's response")

        try {
            val fileData = ByteArray(10 * 1024) { it.toByte() } // Small 10KB file
            val requestBody = fileData.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val response = withContext(Dispatchers.IO) {
                RetrofitClients.clientLongTimeouts.uploadSlowResponse(
                    delayBeforeResponse = 8000, // Same delay as Test 5, but with longer timeout
                    file = requestBody
                )
            }

            log("✓ CORRECT: Upload and response received successfully")
            log("  → Response: $response")
            log("  → Client readTimeout (60s) > Server delay (8s)")
            log("  → This proves adequate CLIENT readTimeout handles server responses")
        } catch (e: SocketTimeoutException) {
            log("✗ UNEXPECTED: Timeout occurred (shouldn't happen with 60s timeout)")
            log("  → SocketTimeoutException: ${e.message}")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 7: Large File Download
     * Downloads a large file with normal timeout settings
     * Tests that normal timeouts work for large transfers
     */
    private suspend fun testCase7_DownloadLargeFile() {
        log("[TEST 7] Large File Download with Normal Timeouts")
        log("Expected: ✓ Success - normal timeouts handle large file")
        log("Configuration: readTimeout=30s, writeTimeout=30s")

        try {
            val result = withContext(Dispatchers.IO) {
                val responseBody = RetrofitClients.clientNormalTimeouts.downloadLargeFile()

                val inputStream = responseBody.byteStream()
                val buffer = ByteArray(1024 * 1024) // 1MB chunks
                var totalBytes = 0L
                var chunkCount = 0

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    totalBytes += bytesRead
                    chunkCount++

                    if (chunkCount % 10 == 0) {
                        android.util.Log.d("DOWNLOAD", "Progress: $chunkCount MB downloaded")
                    }
                }

                Pair(totalBytes, chunkCount)
            }

            log("✓ CORRECT: Large file download succeeded")
            log("  → Total bytes received: ${result.first}")
            log("  → Chunks processed: ${result.second}")
            log("  → Normal timeouts are sufficient for continuous data transfer")
        } catch (e: SocketTimeoutException) {
            log("✗ UNEXPECTED: Timeout occurred")
            log("  → SocketTimeoutException: ${e.message}")
            log("  → Fix: Increase readTimeout for large file downloads")
        } catch (e: Exception) {
            log("✗ UNEXPECTED: Exception occurred")
            log("  → ${e.javaClass.simpleName}: ${e.message}")
        }
    }
    */
}

@Composable
fun TestResultsView(logText: String, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when logText changes
    LaunchedEffect(logText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Text(
        text = logText.ifEmpty { "Starting tests..." },
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onBackground,
        lineHeight = 16.sp
    )
}
