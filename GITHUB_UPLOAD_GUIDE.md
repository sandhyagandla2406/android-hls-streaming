# How to Upload This Project to GitHub

## Step 1 — Create the GitHub Repository

1. Go to [github.com/new](https://github.com/new)
2. Repository name: `android-hls-streaming`
3. Description: `HLS video streaming POC — Android (Kotlin/ExoPlayer) + Go backend | Canadore College 2024`
4. Set to **Public** (or Private if preferred)
5. **Do NOT** check "Add a README" (we already have one)
6. Click **Create repository**

---

## Step 2 — Initialize Git and Push

Open your terminal and run these commands:

```bash
# Navigate to the project folder
cd android-hls-streaming

# Initialize git
git init

# Add all files
git add .

# First commit
git commit -m "feat: initial commit — Android HLS streaming POC

- Kotlin/Java Android app with ExoPlayer3 (Media3) HLS playback
- Adaptive bitrate streaming with DefaultTrackSelector
- Network simulation (Wi-Fi / 4G / 3G / Poor) via OkHttp interceptor
- Basic DRM handling (Widevine + ClearKey) concepts
- Real-time diagnostics overlay (bitrate, buffer, quality)
- Lightweight Go backend serving HLS manifests and stream catalog
- CORS + cache-control middleware
- Mock stream catalog with 3 test streams"

# Add your GitHub repo as the remote (replace YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/android-hls-streaming.git

# Set main branch and push
git branch -M main
git push -u origin main
```

---

## Step 3 — Verify on GitHub

Visit `https://github.com/YOUR_USERNAME/android-hls-streaming`

You should see:
```
android-hls-streaming/
├── README.md          ← project overview renders here
├── .gitignore
├── android-app/       ← Kotlin Android project
└── go-backend/        ← Go server
```

---

## Step 4 — Add Topics (optional but recommended)

On your repo page → **About (gear icon)** → Add topics:
```
android  kotlin  exoplayer  hls  streaming  go  media3  adaptive-bitrate  drm  canadore-college
```

---

## Troubleshooting

**Authentication error on push?**
```bash
# Use a Personal Access Token instead of password
# Generate one at: github.com/settings/tokens
git remote set-url origin https://YOUR_TOKEN@github.com/YOUR_USERNAME/android-hls-streaming.git
```

**Already have a git repo initialized?**
```bash
git remote -v        # check current remotes
git remote add origin https://github.com/YOUR_USERNAME/android-hls-streaming.git
```

**Want to update the remote URL?**
```bash
git remote set-url origin https://github.com/YOUR_USERNAME/android-hls-streaming.git
```
