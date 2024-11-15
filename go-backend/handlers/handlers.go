package handlers

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// ─── Domain types ─────────────────────────────────────────────────────────────

type StreamItem struct {
	ID                 string         `json:"id"`
	Title              string         `json:"title"`
	Description        string         `json:"description"`
	DurationSeconds    int            `json:"duration"`
	ThumbnailURL       string         `json:"thumbnail"`
	ManifestURL        string         `json:"manifest_url"`
	DRMType            string         `json:"drm_type"`
	LicenseURL         string         `json:"license_url,omitempty"`
	AvailableQualities []StreamQuality `json:"qualities"`
}

type StreamQuality struct {
	Label     string `json:"label"`
	Bandwidth int64  `json:"bandwidth"`
	Width     int    `json:"width"`
	Height    int    `json:"height"`
	URL       string `json:"url"`
}

type StreamListResponse struct {
	Streams []StreamItem `json:"streams"`
	Total   int          `json:"total"`
}

type HealthResponse struct {
	Status  string `json:"status"`
	Version string `json:"version"`
	Streams int    `json:"streams"`
	Time    string `json:"time"`
}

// ─── Mock catalog ─────────────────────────────────────────────────────────────

var catalog = []StreamItem{
	{
		ID:              "big-buck-bunny",
		Title:           "Big Buck Bunny",
		Description:     "Classic open-source animation — perfect for testing ABR across quality levels.",
		DurationSeconds: 596,
		ThumbnailURL:    "https://peach.blender.org/wp-content/uploads/bbb-splash.png",
		ManifestURL:     "/hls/big-buck-bunny/index.m3u8",
		DRMType:         "NONE",
		AvailableQualities: []StreamQuality{
			{Label: "1080p", Bandwidth: 5_000_000, Width: 1920, Height: 1080, URL: "/hls/big-buck-bunny/1080p/index.m3u8"},
			{Label: "720p",  Bandwidth: 2_500_000, Width: 1280, Height: 720,  URL: "/hls/big-buck-bunny/720p/index.m3u8"},
			{Label: "480p",  Bandwidth: 1_200_000, Width: 854,  Height: 480,  URL: "/hls/big-buck-bunny/480p/index.m3u8"},
			{Label: "360p",  Bandwidth: 600_000,   Width: 640,  Height: 360,  URL: "/hls/big-buck-bunny/360p/index.m3u8"},
		},
	},
	{
		ID:              "tears-of-steel",
		Title:           "Tears of Steel",
		Description:     "Sci-fi short by Blender Foundation — tests higher resolution tracks.",
		DurationSeconds: 734,
		ThumbnailURL:    "https://mango.blender.org/wp-content/uploads/2013/05/01_thom_celia_bridge.jpg",
		ManifestURL:     "/hls/tears-of-steel/index.m3u8",
		DRMType:         "NONE",
		AvailableQualities: []StreamQuality{
			{Label: "1080p", Bandwidth: 6_000_000, Width: 1920, Height: 1080, URL: "/hls/tears-of-steel/1080p/index.m3u8"},
			{Label: "720p",  Bandwidth: 3_000_000, Width: 1280, Height: 720,  URL: "/hls/tears-of-steel/720p/index.m3u8"},
			{Label: "480p",  Bandwidth: 1_500_000, Width: 854,  Height: 480,  URL: "/hls/tears-of-steel/480p/index.m3u8"},
		},
	},
	{
		ID:              "drm-clearkey-demo",
		Title:           "ClearKey DRM Demo",
		Description:     "Demonstrates the ClearKey DRM key exchange flow — keys are served as plain JSON.",
		DurationSeconds: 180,
		ThumbnailURL:    "",
		ManifestURL:     "/hls/drm-clearkey-demo/index.m3u8",
		DRMType:         "CLEARKEY",
		LicenseURL:      "https://cwip-shaka-proxy.appspot.com/no_auth",
		AvailableQualities: []StreamQuality{
			{Label: "720p", Bandwidth: 2_500_000, Width: 1280, Height: 720, URL: "/hls/drm-clearkey-demo/720p/index.m3u8"},
			{Label: "480p", Bandwidth: 1_200_000, Width: 854,  Height: 480, URL: "/hls/drm-clearkey-demo/480p/index.m3u8"},
		},
	},
}

// ─── Handlers ─────────────────────────────────────────────────────────────────

// HealthHandler — GET /health
func HealthHandler(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, HealthResponse{
		Status:  "ok",
		Version: "1.0.0",
		Streams: len(catalog),
		Time:    time.Now().UTC().Format(time.RFC3339),
	})
}

// StreamsHandler — GET /api/streams
func StreamsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Pagination
	page  := queryInt(r, "page",  1)
	limit := queryInt(r, "limit", 20)
	start := (page - 1) * limit
	end   := start + limit
	if start > len(catalog) { start = len(catalog) }
	if end   > len(catalog) { end   = len(catalog) }

	writeJSON(w, http.StatusOK, StreamListResponse{
		Streams: catalog[start:end],
		Total:   len(catalog),
	})
}

// StreamDetailHandler — GET /api/streams/{id}
func StreamDetailHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/api/streams/")
	for _, s := range catalog {
		if s.ID == id {
			writeJSON(w, http.StatusOK, s)
			return
		}
	}
	http.Error(w, `{"error":"stream not found"}`, http.StatusNotFound)
}

// HLSHandler — GET /hls/{streamID}/index.m3u8  or  /hls/{streamID}/{quality}/index.m3u8
func HLSHandler(w http.ResponseWriter, r *http.Request) {
	// Parse path:  /hls/<id>/index.m3u8  or  /hls/<id>/<quality>/index.m3u8
	path    := strings.TrimPrefix(r.URL.Path, "/hls/")
	parts   := strings.Split(strings.TrimSuffix(path, "/index.m3u8"), "/")

	if len(parts) < 1 || parts[0] == "" {
		http.Error(w, "invalid path", http.StatusBadRequest)
		return
	}

	streamID := parts[0]
	var stream *StreamItem
	for i := range catalog {
		if catalog[i].ID == streamID {
			stream = &catalog[i]
			break
		}
	}
	if stream == nil {
		http.Error(w, "stream not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/vnd.apple.mpegurl")

	if len(parts) == 1 {
		// Master playlist
		fmt.Fprint(w, buildMasterPlaylist(stream))
	} else {
		// Media playlist for a specific quality
		quality := parts[1]
		fmt.Fprint(w, buildMediaPlaylist(stream, quality))
	}
}

// SegmentHandler — GET /segments/{filename}
// Returns a minimal placeholder response so the app doesn't crash on missing segments.
// In production, real .ts segment files would be served here.
func SegmentHandler(w http.ResponseWriter, r *http.Request) {
	filename := strings.TrimPrefix(r.URL.Path, "/segments/")
	log.Printf("Segment requested: %s", filename)
	// Return an empty 200 — ExoPlayer will handle the empty payload gracefully in POC mode
	w.Header().Set("Content-Type", "video/MP2T")
	w.Header().Set("X-POC-Note", "simulated segment endpoint")
	w.WriteHeader(http.StatusOK)
}

// ─── HLS manifest builders ────────────────────────────────────────────────────

func buildMasterPlaylist(s *StreamItem) string {
	var sb strings.Builder
	sb.WriteString("#EXTM3U\n")
	sb.WriteString("#EXT-X-VERSION:3\n\n")

	for _, q := range s.AvailableQualities {
		sb.WriteString(fmt.Sprintf(
			"#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d,NAME=\"%s\"\n%s\n\n",
			q.Bandwidth, q.Width, q.Height, q.Label, q.URL,
		))
	}
	return sb.String()
}

func buildMediaPlaylist(s *StreamItem, quality string) string {
	// Target duration 6 s, 10 simulated segments
	var sb strings.Builder
	sb.WriteString("#EXTM3U\n")
	sb.WriteString("#EXT-X-VERSION:3\n")
	sb.WriteString("#EXT-X-TARGETDURATION:6\n")
	sb.WriteString("#EXT-X-MEDIA-SEQUENCE:0\n")
	sb.WriteString(fmt.Sprintf("#EXT-X-PLAYLIST-TYPE:VOD\n\n"))

	for i := 0; i < 10; i++ {
		sb.WriteString(fmt.Sprintf("#EXTINF:6.000,\n"))
		sb.WriteString(fmt.Sprintf("/segments/%s_%s_seg%03d.ts\n", s.ID, quality, i))
	}
	sb.WriteString("#EXT-X-ENDLIST\n")
	return sb.String()
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		log.Printf("writeJSON error: %v", err)
	}
}

func queryInt(r *http.Request, key string, def int) int {
	v := r.URL.Query().Get(key)
	if v == "" { return def }
	n, err := strconv.Atoi(v)
	if err != nil { return def }
	return n
}
