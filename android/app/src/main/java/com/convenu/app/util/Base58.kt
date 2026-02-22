package com.convenu.app.util

/**
 * Base58 encoder/decoder for Solana addresses and signatures.
 * Uses the Bitcoin/Solana alphabet: no 0, O, I, l.
 */
object Base58 {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = ALPHABET.length // 58
    private val INDEXES = IntArray(128) { -1 }.also { arr ->
        ALPHABET.forEachIndexed { i, c -> arr[c.code] = i }
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        // Count leading zeros
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) zeros++

        // Convert base-256 to base-58
        val encoded = CharArray(input.size * 2) // upper bound
        var outputStart = encoded.size
        var inputStart = zeros

        while (inputStart < input.size) {
            var carry = input[inputStart].toInt() and 0xFF
            var i = encoded.size - 1
            while (i >= outputStart || carry != 0) {
                carry += 256 * (if (i < encoded.size) ALPHABET.indexOf(encoded[i]).coerceAtLeast(0) else 0)
                encoded[i] = ALPHABET[carry % BASE]
                carry /= BASE
                i--
            }
            outputStart = i + 1
            inputStart++
        }

        // Preserve leading zeros as '1's
        while (outputStart < encoded.size && encoded[outputStart] == ALPHABET[0]) outputStart++
        repeat(zeros) { encoded[--outputStart] = '1' }

        return String(encoded, outputStart, encoded.size - outputStart)
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return byteArrayOf()

        // Count leading '1's (zeros)
        var zeros = 0
        while (zeros < input.length && input[zeros] == '1') zeros++

        // Convert base-58 to base-256
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size

        for (i in zeros until input.length) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            require(digit >= 0) { "Invalid Base58 character: $c" }

            var carry = digit
            var j = decoded.size - 1
            while (j >= outputStart || carry != 0) {
                carry += 58 * (decoded[j].toInt() and 0xFF)
                decoded[j] = (carry % 256).toByte()
                carry /= 256
                j--
            }
            outputStart = j + 1
        }

        // Build result with leading zeros
        val result = ByteArray(zeros + (decoded.size - outputStart))
        System.arraycopy(decoded, outputStart, result, zeros, decoded.size - outputStart)
        return result
    }
}
