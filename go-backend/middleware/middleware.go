package middleware

import (
	"log"
	"net/http"
	"time"
)

// MiddlewareFunc is a function that wraps an http.Handler.
type MiddlewareFunc func(http.Handler) http.Handler

// Chain applies a list of middleware functions to a handler (left-to-right order).
func Chain(h http.Handler, middleware ...MiddlewareFunc) http.Handler {
	for i := len(middleware) - 1; i >= 0; i-- {
		h = middleware[i](h)
	}
	return h
}

// ─── Logger ───────────────────────────────────────────────────────────────────

// Logger logs method, path, status code, and duration for every request.
func Logger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		lrw   := &loggingResponseWriter{ResponseWriter: w, statusCode: http.StatusOK}
		next.ServeHTTP(lrw, r)
		log.Printf("%s %s %d %s", r.Method, r.URL.Path, lrw.statusCode, time.Since(start))
	})
}

type loggingResponseWriter struct {
	http.ResponseWriter
	statusCode int
}

func (lrw *loggingResponseWriter) WriteHeader(code int) {
	lrw.statusCode = code
	lrw.ResponseWriter.WriteHeader(code)
}

// ─── CORS ─────────────────────────────────────────────────────────────────────

// CORS adds permissive CORS headers so the Android app (and browser dev tools) can
// reach the backend without origin restrictions.
func CORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin",  "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// ─── CacheControl ─────────────────────────────────────────────────────────────

// CacheControl sets appropriate cache headers for HLS media delivery:
//   - Manifests: short TTL (must-revalidate) so ABR stays current
//   - Segments:  longer TTL (immutable) since they never change once written
//   - API JSON:  no-store (always fresh)
func CacheControl(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case isHLSManifest(r.URL.Path):
			// Master and media playlists — revalidate frequently
			w.Header().Set("Cache-Control", "public, max-age=5, must-revalidate")
		case isSegment(r.URL.Path):
			// TS segments are immutable once created
			w.Header().Set("Cache-Control", "public, max-age=86400, immutable")
		default:
			// API responses — never cache
			w.Header().Set("Cache-Control", "no-store")
		}
		next.ServeHTTP(w, r)
	})
}

func isHLSManifest(path string) bool {
	return len(path) > 5 && path[len(path)-5:] == ".m3u8"
}

func isSegment(path string) bool {
	return len(path) > 3 && path[len(path)-3:] == ".ts"
}
