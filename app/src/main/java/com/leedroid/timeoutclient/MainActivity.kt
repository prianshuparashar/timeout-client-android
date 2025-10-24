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
