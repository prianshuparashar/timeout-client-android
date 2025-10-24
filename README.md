# HTTP Timeout Test Client

An Android application that demonstrates and tests **all 4 timeout scenarios** in HTTP communication using Retrofit and OkHttp.

## Complete Timeout Scenarios

### Download Scenario (Client ← Server)

**Both timeouts can occur:**

#### 1. Client Read Timeout ✓
- **When**: Server sends data slowly
- **Where**: CLIENT side
- **Why**: Client is waiting to READ response data
- **Exception**: SocketTimeoutException at client
- **Fix**: Increase CLIENT readTimeout

#### 2. Server Write Timeout ✓
- **When**: Client reads/consumes data slowly (rare)
- **Where**: SERVER side
- **Why**: Server's write() call blocks because TCP buffer is full (client not consuming fast enough)
- **Exception**: Error at server side
- **Fix**: Increase SERVER writeTimeout

### Upload Scenario (Client → Server)

**Both timeouts can occur:**

#### 1. Client Write Timeout ✓
- **When**: Server reads/consumes data slowly
- **Where**: CLIENT side
- **Why**: Client's write() call blocks because TCP buffer is full (server not consuming fast enough)
- **Exception**: SocketTimeoutException at client
- **Fix**: Increase CLIENT writeTimeout

#### 2. Server Read Timeout ✓
- **When**: Client sends data slowly
- **Where**: SERVER side
- **Why**: Server is waiting to READ request body data
- **Exception**: Error at server (connection closed)
- **Fix**: Increase SERVER readTimeout

## Project Overview

This app tests 7 comprehensive test cases covering all timeout configurations:

### Test Cases

1. **Download - CLIENT Read Timeout FAILS** (3s timeout, 6s server delay) → ✗ Expected timeout
2. **Download - CLIENT Read Timeout FIXED** (60s timeout, 6s server delay) → ✓ Expected success
3. **Upload - CLIENT Write Timeout FAILS** (3s timeout, 6s server delay) → ✗ Expected timeout
4. **Upload - CLIENT Write Timeout FIXED** (60s timeout, 6s server delay) → ✓ Expected success
5. **Upload - CLIENT Read Timeout FAILS** (3s timeout waiting for response) → ✗ Expected timeout
6. **Upload - CLIENT Read Timeout FIXED** (60s timeout waiting for response) → ✓ Expected success
7. **Large File Download** (Normal timeouts) → ✓ Expected success

**Note**: SERVER-side timeouts (Server Write Timeout during download, Server Read Timeout during upload) are explained but not directly tested from the client, as they require server-side configuration and throttling client speed.

## Server Setup

The app connects to a Spring Boot server running on `localhost:8080` (accessed via `10.0.2.2:8080` from Android emulator).

### Server Endpoints

1. **GET /api/ping** - Simple connectivity test
2. **GET /api/download/slow-server?delayBetweenChunks={ms}** - Slow download (delays between chunks)
3. **GET /api/download/large-file** - Large file download (100MB)
4. **POST /api/upload/slow-server?delayBetweenReads={ms}** - Slow upload (server delays reading)
5. **POST /api/upload/normal** - Normal upload

## Client Configurations

The app uses 4 different Retrofit client configurations:

### 1. Short Read Timeout Client
- Read: 3s (short - will timeout on slow reads)
- Write: 10s
- Connect: 10s

### 2. Normal Timeouts Client
- Read: 30s
- Write: 30s
- Connect: 10s

### 3. Short Write Timeout Client
- Read: 30s
- Write: 3s (short - will timeout on slow writes)
- Connect: 10s

### 4. Long Timeouts Client
- Read: 60s (long - allows slow operations)
- Write: 60s
- Connect: 10s

## Expected Output

When all tests run successfully, you should see:

- **Test 0**: Ping succeeds ✓
- **Test 1**: CLIENT Read timeout during download ✓ (Expected failure)
- **Test 2**: Download succeeds with longer timeout ✓
- **Test 3**: CLIENT Write timeout during upload ✓ (Expected failure)
- **Test 4**: Upload succeeds with longer timeout ✓
- **Test 5**: CLIENT Read timeout waiting for response ✓ (Expected failure)
- **Test 6**: Response received with longer timeout ✓
- **Test 7**: Large file download succeeds ✓

This demonstrates clear understanding of:
- **All 4 timeout types**: Client Read, Client Write, Server Read, Server Write
- Where timeouts occur (client vs server)
- Which timeout to adjust (read vs write)
- How to configure OkHttp/Retrofit properly

## Running the App

1. **Start the Spring Boot server** on localhost:8080
2. **Launch Android emulator** (not physical device, as it uses 10.0.2.2)
3. **Install and run the app**
4. Tests will automatically execute and display results

## Key Files

- `MainActivity.kt` - Main activity with 7 test cases
- `TimeoutTestApi.kt` - Retrofit API interface
- `RetrofitClients.kt` - 4 client configurations with different timeouts
- `AndroidManifest.xml` - Includes INTERNET permission and cleartext traffic

## Dependencies

- Retrofit 2.9.0 (with Gson and Scalars converters)
- OkHttp 4.12.0 (with logging interceptor)
- Kotlin Coroutines 1.7.3
- Jetpack Compose for UI

## Troubleshooting

### Compilation Errors
If you see IDE errors about "Expecting member declaration", try:
1. **Build > Clean Project**
2. **Build > Rebuild Project**
3. **File > Invalidate Caches / Restart**

These are often false positives from stale compilation cache.

### Network Errors
- Ensure server is running on localhost:8080
- Use Android emulator (not physical device)
- Check AndroidManifest has INTERNET permission
- Verify `usesCleartextTraffic="true"` is set

### Timeout Not Occurring
- Check server implementation delays match expectations
- Verify client timeout values in RetrofitClients.kt
- Review server logs to see actual timing


