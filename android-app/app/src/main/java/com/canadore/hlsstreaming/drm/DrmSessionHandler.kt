package com.canadore.hlsstreaming.drm

import android.util.Log

/**
 * Placeholder DRM handler demonstrating how a production app would
 * interact with a license server for Widevine and ClearKey DRM.
 *
 * In ExoPlayer (Media3) actual DRM is handled transparently once you
 * configure [MediaItem.DrmConfiguration] — this class documents the
 * conceptual flow for the college project.
 *
 * Flow:
 *  1. ExoPlayer encounters an encrypted segment
 *  2. Extracts the PSSH (Protection System Specific Header) box from the manifest
 *  3. Sends a license request to [licenseUrl] with the PSSH payload
 *  4. License server returns decryption keys
 *  5. ExoPlayer decrypts and renders the media
 */
object DrmSessionHandler {

    private const val TAG = "DrmSessionHandler"

    /**
     * Logs and simulates a Widevine license request.
     * In production, Media3 handles this internally; this method is purely illustrative.
     */
    fun simulateWidevineRequest(licenseUrl: String, psshData: ByteArray) {
        Log.d(TAG, "=== Widevine License Request ===")
        Log.d(TAG, "License server : $licenseUrl")
        Log.d(TAG, "PSSH length    : ${psshData.size} bytes")
        Log.d(TAG, "PSSH (hex)     : ${psshData.take(16).joinToString("") { "%02x".format(it) }}…")
        Log.d(TAG, "Status         : ✓ Key fetched (simulated)")
    }

    /**
     * Simulates ClearKey — a W3C standard where keys are exchanged as plain JSON.
     * Useful for development / testing without a full Widevine server.
     *
     * Request body (sent to licenseUrl):
     * { "type": "temporary", "kids": ["<base64url-encoded-keyId>"] }
     *
     * Response body:
     * { "keys": [{ "kty":"oct", "k":"<base64url-key>", "kid":"<base64url-keyId>" }] }
     */
    fun simulateClearKeyExchange(licenseUrl: String, keyId: String): String {
        Log.d(TAG, "=== ClearKey Exchange ===")
        Log.d(TAG, "License server : $licenseUrl")
        Log.d(TAG, "Key ID         : $keyId")

        // Simulated response — in a real integration this comes from the server
        val simulatedKey = "fake_clearkey_${keyId.take(8)}"
        Log.d(TAG, "Derived key    : $simulatedKey (simulated)")
        return simulatedKey
    }

    /**
     * Builds the JSON request body for a ClearKey license request.
     */
    fun buildClearKeyRequestBody(keyIds: List<String>): String {
        val kids = keyIds.joinToString(",") { "\"$it\"" }
        return """{"type":"temporary","kids":[$kids]}"""
    }
}
