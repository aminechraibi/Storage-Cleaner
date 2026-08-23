package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtils {

    /**
     * Calculates a fast partial hash of the first [maxBytes] of a file (default 4KB).
     * Used as an extremely cheap filter before performing full hashing.
     */
    fun calculatePartialHash(file: File, maxBytes: Int = 4096): String? {
        if (!file.exists() || !file.isFile || file.length() == 0L) return null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(minOf(maxBytes.toLong(), file.length()).toInt())
            FileInputStream(file).use { fis ->
                val read = fis.read(buffer)
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates full SHA-256 streaming hash using 16KB buffers.
     * Only executed for verified partial-hash collision candidates.
     */
    fun calculateSha256(file: File, bufferSize: Int = 16384): String? {
        if (!file.exists() || !file.isFile || file.length() == 0L) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(bufferSize)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes a 64-bit Difference Hash (dHash) for an image file.
     * Resizes image to 9x8 grayscale, compares adjacent columns, returns 64-bit Long.
     */
    fun calculateDHash(file: File): Long? {
        if (!file.exists() || !file.isFile || file.length() == 0L) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // Downsample decoding to save RAM on low-end devices
            val sampleSize = maxOf(1, minOf(options.outWidth / 32, options.outHeight / 32))
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
            val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, 9, 8, true)
            if (scaledBitmap != rawBitmap) {
                rawBitmap.recycle()
            }

            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val leftPixel = scaledBitmap.getPixel(x, y)
                    val rightPixel = scaledBitmap.getPixel(x + 1, y)

                    val leftLuma = Color.red(leftPixel) * 299 + Color.green(leftPixel) * 587 + Color.blue(leftPixel) * 114
                    val rightLuma = Color.red(rightPixel) * 299 + Color.green(rightPixel) * 587 + Color.blue(rightPixel) * 114

                    if (leftLuma > rightLuma) {
                        hash = hash or (1L shl (y * 8 + x))
                    }
                }
            }
            scaledBitmap.recycle()
            hash
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes the Hamming distance (number of bit differences) between two 64-bit hashes.
     * Distance <= 8 usually indicates near-identical or similar photos.
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
