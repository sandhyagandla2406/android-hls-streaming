# Android HLS Video Streaming — Proof of Concept
**Canadore College | 2024**

A proof-of-concept demonstrating HLS (HTTP Live Streaming) video playback on Android using ExoPlayer, paired with a lightweight Go backend that serves video manifests and segment URLs.

---

## 📱 Project Overview

This project showcases:
- **Adaptive Bitrate Streaming (ABR)** — ExoPlayer dynamically selects quality based on network conditions
- **Basic DRM concepts** — Widevine/ClearKey placeholder integration
- **Simulated network profiles** — WiFi, 4G, 3G, and poor-connection scenarios
- **VOD delivery architecture** — Manifest serving, segment routing, CORS handling
- **CDN integration patterns** — URL signing concepts and cache headers

---

## 🗂 Repository Structure

```
android-hls-streaming/
├── android-app/          # Kotlin/Java Android application
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/canadore/hlsstreaming/
│   │       │   ├── ui/           # Activities & Fragments
│   │       │   ├── player/       # ExoPlayer setup & ABR logic
│   │       │   ├── network/      # Network simulation & API client
│   │       │   ├── drm/          # DRM callback handlers
│   │       │   ├── model/        # Data classes
│   │       │   └── util/         # Helpers & extensions
│   │       └── res/              # Layouts, drawables, values
│   └── build.gradle
└── go-backend/           # Go HLS manifest & segment server
    ├── main.go
    ├── handlers/
    ├── middleware/
    └── README.md
```

---

## 🚀 Quick Start

### Backend (Go)

```bash
cd go-backend
go mod tidy
go run main.go
# Server starts on :8080
```

### Android App

1. Open `android-app/` in Android Studio (Hedgehog or newer)
2. Update `BASE_URL` in `NetworkConfig.kt` to your machine's IP
3. Run on device or emulator (API 24+)

---

## 🏗 Architecture

```
Android App                     Go Backend
┌─────────────────┐             ┌──────────────────────┐
│  MainActivity   │             │  /api/streams        │ ← stream list
│  PlayerActivity │──── HTTP ──▶│  /hls/{id}/index.m3u8│ ← master manifest
│  ExoPlayer      │             │  /hls/{id}/{quality} │ ← media manifest
│  ABR Logic      │◀── HLS  ───│  /segments/{file}    │ ← TS segments
│  DRM Handler    │             │  /health             │ ← health check
└─────────────────┘             └──────────────────────┘
```

---

## 🧪 Network Simulation Profiles

| Profile  | Bandwidth  | Latency | Packet Loss |
|----------|------------|---------|-------------|
| WiFi     | 50 Mbps    | 5ms     | 0%          |
| 4G LTE   | 20 Mbps    | 30ms    | 0.1%        |
| 3G       | 2 Mbps     | 100ms   | 1%          |
| Poor     | 500 Kbps   | 300ms   | 5%          |

---

## 📚 Technologies

| Layer    | Technology              |
|----------|-------------------------|
| Android  | Kotlin, Java, ExoPlayer3|
| UI       | Jetpack ViewBinding     |
| Network  | Retrofit 2, OkHttp      |
| DI       | Manual (no framework)   |
| Backend  | Go 1.21, net/http       |
| Streaming| HLS (RFC 8216)          |
| DRM      | Widevine / ClearKey     |

---

## 📖 Key Learning Outcomes

- HLS manifest structure (master + media playlists)
- ExoPlayer `TrackSelector` and bandwidth estimation
- DRM license server flow (PSSH → license request → key)
- Go middleware patterns for CORS and request logging
- CDN cache-control strategies for media segments

---

*Developed as an academic proof-of-concept at Canadore College, 2024.*
