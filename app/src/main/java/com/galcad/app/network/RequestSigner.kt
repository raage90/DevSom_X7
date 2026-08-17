package com.galcad.app.network

import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Generates the per-request signature headers the backend's
 * requireSignedRequest middleware checks. Each request gets a fresh,
 * one-time nonce and current timestamp, so a captured request can't be
 * replayed later -- unlike a single static shared key.
 */
object RequestSigner {
    private const val ALGORITHM = "HmacSHA256"

    data class SignedHeaders(
        val installId: String,
        val timestamp: String,
        val nonce: String,
        val signature: String
    )

    fun sign(method: String, path: String, installId: String, secret: String): SignedHeaders {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString()

        // Must match the backend's canonical string exactly:
        // METHOD|PATH|installId|nonce|timestamp
        val canonical = "$method|$path|$installId|$nonce|$timestamp"

        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM))
        val rawSignature = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        val hexSignature = rawSignature.joinToString("") { "%02x".format(it) }

        return SignedHeaders(installId, timestamp, nonce, hexSignature)
    }
}
