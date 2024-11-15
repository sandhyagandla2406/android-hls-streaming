package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/canadore/hls-backend/handlers"
	"github.com/canadore/hls-backend/middleware"
)

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	mux := http.NewServeMux()

	// ── Health ────────────────────────────────────────────────────────────────
	mux.HandleFunc("/health", handlers.HealthHandler)

	// ── Stream catalog API ────────────────────────────────────────────────────
	mux.HandleFunc("/api/streams", handlers.StreamsHandler)
	mux.HandleFunc("/api/streams/", handlers.StreamDetailHandler)

	// ── HLS manifest endpoints ────────────────────────────────────────────────
	// GET /hls/{streamID}/index.m3u8           → master playlist
	// GET /hls/{streamID}/{quality}/index.m3u8 → media playlist
	mux.HandleFunc("/hls/", handlers.HLSHandler)

	// ── Segment endpoint (simulated) ──────────────────────────────────────────
	// GET /segments/{filename}
	mux.HandleFunc("/segments/", handlers.SegmentHandler)

	// ── Wrap with middleware ──────────────────────────────────────────────────
	handler := middleware.Chain(
		mux,
		middleware.Logger,
		middleware.CORS,
		middleware.CacheControl,
	)

	server := &http.Server{
		Addr:         ":" + port,
		Handler:      handler,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	log.Printf("🎬  HLS backend listening on :%s", port)
	log.Printf("    Endpoints:")
	log.Printf("      GET /health")
	log.Printf("      GET /api/streams")
	log.Printf("      GET /api/streams/{id}")
	log.Printf("      GET /hls/{streamId}/index.m3u8")
	log.Printf("      GET /hls/{streamId}/{quality}/index.m3u8")
	log.Printf("      GET /segments/{file}")

	if err := server.ListenAndServe(); err != nil {
		log.Fatalf("Server error: %v", err)
	}
}
