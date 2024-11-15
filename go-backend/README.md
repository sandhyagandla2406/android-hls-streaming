# HLS Backend — Go Server

Lightweight Go HTTP server that serves HLS video manifests and stream metadata for the Android POC.

## Running

```bash
go mod tidy
go run main.go
# or build:
go build -o hls-server .
./hls-server
```

Server starts on **:8080** by default. Override with `PORT=9090 go run main.go`.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check + stream count |
| GET | `/api/streams?page=1&limit=20` | Stream catalog |
| GET | `/api/streams/{id}` | Single stream metadata |
| GET | `/hls/{id}/index.m3u8` | HLS master playlist |
| GET | `/hls/{id}/{quality}/index.m3u8` | HLS media playlist |
| GET | `/segments/{file}` | TS segment (simulated) |

## Example Requests

```bash
# Health
curl http://localhost:8080/health

# Stream list
curl http://localhost:8080/api/streams | jq

# Master playlist
curl http://localhost:8080/hls/big-buck-bunny/index.m3u8

# 720p media playlist
curl http://localhost:8080/hls/big-buck-bunny/720p/index.m3u8
```

## Sample Master Playlist Response

```m3u8
#EXTM3U
#EXT-X-VERSION:3

#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,NAME="1080p"
/hls/big-buck-bunny/1080p/index.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,NAME="720p"
/hls/big-buck-bunny/720p/index.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480,NAME="480p"
/hls/big-buck-bunny/480p/index.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=600000,RESOLUTION=640x360,NAME="360p"
/hls/big-buck-bunny/360p/index.m3u8
```

## Architecture Notes

- **No external dependencies** — uses only Go's `net/http` standard library
- **Middleware chain** — Logger → CORS → CacheControl (easily extensible)
- **Cache-Control strategy**: manifests get `max-age=5`, segments get `max-age=86400, immutable`
- Segment handler returns empty 200 responses (simulated); replace with real file serving for production
