# HTTP Timeout Test Client for Android

A clean, professional Android application demonstrating HTTP timeout scenarios using **Retrofit**
and **OkHttp**. This project helps developers understand and configure client-side timeout behavior
in real-world network scenarios.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2029+-green.svg)](https://developer.android.com)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.9.0-orange.svg)](https://square.github.io/retrofit/)
[![OkHttp](https://img.shields.io/badge/OkHttp-4.12.0-yellow.svg)](https://square.github.io/okhttp/)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Timeout Scenarios](#timeout-scenarios)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Testing](#testing)
- [Contributing](#contributing)

---

## Overview

This Android application demonstrates **CLIENT-side HTTP timeout scenarios** that developers
commonly encounter when building network-intensive applications. It provides practical examples of
how to properly configure Retrofit and OkHttp timeout settings.

### What You'll Learn

- **Read Timeout**: Understanding client-side timeout when waiting for server responses
- **Write Timeout**: Understanding client-side timeout when sending data to server
- **Timeout Configuration**: How to properly set timeouts in OkHttp/Retrofit
- **Error Handling**: Best practices for handling SocketTimeoutException
- **Problem-Solution Pattern**: Each failing test is paired with a successful fix

---

## Features

- **4 Core Test Cases** - Demonstrating read and write timeout scenarios
- **Real-time Logging** - Visual feedback with detailed explanations
- **Multiple Client Configurations** - 4 different timeout profiles
- **Jetpack Compose UI** - Modern Android UI toolkit
- **Kotlin Coroutines** - Asynchronous programming with coroutines
- **Production-Ready Code** - Clean architecture and best practices

---

## Architecture

```
┌─────────────────┐
│   MainActivity  │
│  (Jetpack       │
│   Compose UI)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RetrofitClients │
│  (4 Timeout     │
│   Profiles)     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ TimeoutTestApi  │
│  (Retrofit      │
│   Interface)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Spring Boot    │
│     Server      │
│ (localhost:8080)│
└─────────────────┘
```

---

## Timeout Scenarios

### Test Case 1: Download - CLIENT Read Timeout FAILS ❌

**Scenario**: Server sends data slowly (6s delay), client has short timeout (3s)  
**Expected**: `SocketTimeoutException` - CLIENT read timeout  
**Demonstrates**: What happens when readTimeout is too short

### Test Case 2: Download - CLIENT Read Timeout FIXED ✅

**Scenario**: Same slow server (6s delay), client has long timeout (60s)  
**Expected**: Success - download completes  
**Demonstrates**: How increasing readTimeout fixes the issue

### Test Case 3: Upload - CLIENT Write Timeout FAILS ❌

**Scenario**: Server reads slowly (6s delay), client has short timeout (3s)  
**Expected**: `SocketTimeoutException` - CLIENT write timeout  
**Demonstrates**: What happens when writeTimeout is too short

### Test Case 4: Upload - CLIENT Write Timeout FIXED ✅

**Scenario**: Same slow server (6s delay), client has long timeout (60s)  
**Expected**: Success - upload completes  
**Demonstrates**: How increasing writeTimeout fixes the issue

---

## Prerequisites

### Required Software

- **Android Studio**
- **JDK**: 11 or higher
- **Android SDK**: API 29 (Android 10) or higher
- **Spring Boot Server**: Running on `localhost:8080`

### Server Endpoints

The application expects a Spring Boot server with these endpoints:

| Endpoint                                            | Method | Description              |
|-----------------------------------------------------|--------|--------------------------|
| `/api/ping`                                         | GET    | Connectivity test        |
| `/api/download/slow-server?delayBetweenChunks={ms}` | GET    | Slow download simulation |
| `/api/upload/slow-server?delayBetweenReads={ms}`    | POST   | Slow upload simulation   |

---

## Installation

### 1. Clone the Repository

### 2. Open in Android Studio

- Open Android Studio
- Select **File > Open**
- Navigate to the cloned project directory
- Click **OK**

### 3. Sync Gradle

Android Studio will automatically sync Gradle. If not:

- Click **File > Sync Project with Gradle Files**

### 4. Start the Server

Ensure your Spring Boot timeout test server is running on `localhost:8080`.

### 5. Run the Application

---

## Usage

### Running Tests

1. **Launch the app** on an Android emulator (not a physical device)
2. **Tests run automatically** when the app starts
3. **View results** in the scrollable text view

---

## Project Structure

```
TimeoutClient/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/leedroid/timeoutclient/
│   │   │   │   ├── MainActivity.kt          # Main UI and test execution
│   │   │   │   ├── RetrofitClients.kt       # 4 timeout configurations
│   │   │   │   ├── TimeoutTestApi.kt        # Retrofit API interface
│   │   │   │   └── ui/theme/                # Compose theme files
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                    # Dependency versions
├── .gitignore
├── README.md
└── build.gradle.kts
```

---

## Configuration

### Retrofit Client Configurations

The app uses 4 different OkHttp client configurations:

#### 1. Short Read Timeout Client

```kotlin
readTimeout: 3 seconds   // Short - will timeout on slow reads
writeTimeout: 10 seconds
connectTimeout: 10 seconds
```

#### 2. Normal Timeouts Client

```kotlin
readTimeout: 30 seconds
writeTimeout: 30 seconds
connectTimeout: 10 seconds
```

#### 3. Short Write Timeout Client

```kotlin
readTimeout: 30 seconds
writeTimeout: 3 seconds  // Short - will timeout on slow writes
connectTimeout: 10 seconds
```

#### 4. Long Timeouts Client

```kotlin
readTimeout: 60 seconds  // Long - allows slow operations
writeTimeout: 60 seconds
connectTimeout: 10 seconds
```

### Customizing Timeouts

Edit `RetrofitClients.kt` to modify timeout values:

```kotlin
val clientShortReadTimeout: TimeoutTestApi by lazy {
    createRetrofitClient(
        readTimeoutSeconds = 3,    // Modify this
        writeTimeoutSeconds = 10,
        connectTimeoutSeconds = 10,
        loggingTag = "CLIENT-SHORT-READ"
    )
}
```

---

## Testing

### Unit Tests

Currently, this is a demonstration/testing app. Unit tests can be added in:

```
app/src/test/java/com/leedroid/timeoutclient/
```

### Instrumentation Tests

Android UI tests can be added in:

```
app/src/androidTest/java/com/leedroid/timeoutclient/
```

### Manual Testing

1. Start the Spring Boot server
2. Run the Android app on an emulator
3. Observe test results in real-time
4. Check Logcat for detailed logs (tag: `%--PRINT--%`)

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

---

**⭐ If you find this project helpful, please consider giving it a star!**