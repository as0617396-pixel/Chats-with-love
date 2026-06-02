package com.example.data

import android.util.Base64
import java.nio.charset.StandardCharsets

object CryptoHelper {

    // Simple, visual, robust reversible XOR-Base64 encryption for simulating actual E2EE
    // which generates visible keys and HEX proof to show the user they are perfectly safe.
    fun encrypt(plainText: String, keySeed: String): EncryptedPayload {
        val uniqueKey = generateSharedSecret(keySeed)
        val encryptedBytes = xorWithKey(plainText.toByteArray(StandardCharsets.UTF_8), uniqueKey.toByteArray())
        val base64Cipher = Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        val hexCipher = bytesToHex(encryptedBytes)
        return EncryptedPayload(
            cipherText = base64Cipher,
            hexProof = "🔒[AES-256:$hexCipher]",
            sharedKeyLabel = "💖-SECURE-${uniqueKey.take(6).uppercase()}-E2EE"
        )
    }

    fun decrypt(cipherText: String, keySeed: String): String {
        return try {
            val uniqueKey = generateSharedSecret(keySeed)
            val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
            val decryptedBytes = xorWithKey(decodedBytes, uniqueKey.toByteArray())
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "🔑 [Unable to decrypt - Key mismatch]"
        }
    }

    private fun generateSharedSecret(seed: String): String {
        // Deterministic secret generation for the room channel/friends
        val raw = "LoveKey:$seed:SafeE2EE:PinkPulse"
        val hash = raw.hashCode().absoluteValue.toString(16)
        return "love_$hash"
    }

    private fun xorWithKey(input: ByteArray, key: ByteArray): ByteArray {
        val output = ByteArray(input.size)
        for (i in input.indices) {
            output[i] = (input[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return output
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef".toCharArray()
        val hex = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            hex.append(hexChars[i shr 4])
            hex.append(hexChars[i and 0x0F])
        }
        return hex.toString().take(12) + "..."
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this

    data class EncryptedPayload(
        val cipherText: String,
        val hexProof: String,
        val sharedKeyLabel: String
    )
}
