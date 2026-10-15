package com.uc.caffeine.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.UPCEReader

/**
 * Reads retail product barcodes (EAN-13, EAN-8, UPC-A, UPC-E) from a greyscale image, so camera
 * frames and photos share one path. Not thread-safe: give each thread its own instance.
 *
 * [tryHarder] trades speed for accuracy; worth it for a single photo, too slow for every frame.
 */
class BarcodeDecoder(tryHarder: Boolean = false) {

    private val reader = MultiFormatReader().apply {
        val hints = mutableMapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
            ),
        )
        // ZXing enables this on the key's presence alone, whatever the value.
        if (tryHarder) hints[DecodeHintType.TRY_HARDER] = true
        setHints(hints)
    }

    /**
     * Decodes [height] rows of [rowStride] bytes each, of which the first [width] are pixels (camera
     * planes pad their rows). Returns the normalised code (see [normalizeProductBarcode]), or null.
     */
    fun decode(luminance: ByteArray, width: Int, height: Int, rowStride: Int = width): String? {
        val upright = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        // Cans carry barcodes both ways round and camera frames arrive sideways, so a quarter turn
        // covers whichever way the bars ended up.
        return decode(upright) ?: decode(rotateQuarter(luminance, width, height, rowStride))
    }

    private fun decode(source: LuminanceSource): String? = try {
        val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        normalizeProductBarcode(result.text, result.barcodeFormat)
    } catch (_: ReaderException) {
        null
    } finally {
        reader.reset()
    }

    private fun rotateQuarter(src: ByteArray, width: Int, height: Int, rowStride: Int): LuminanceSource {
        val rotated = ByteArray(width * height)
        for (y in 0 until height) {
            val rowStart = y * rowStride
            val column = height - 1 - y
            for (x in 0 until width) rotated[x * height + column] = src[rowStart + x]
        }
        return PlanarYUVLuminanceSource(rotated, height, width, 0, 0, height, width, false)
    }
}

/**
 * The same product can be read as UPC-A, UPC-E or EAN-13 depending on the packaging and the reader.
 * Storing every code as its EAN-13 form (UPC-E expanded, UPC-A with its leading zero) makes them
 * all match. EAN-8 has no longer form and is kept as is.
 */
internal fun normalizeProductBarcode(text: String, format: BarcodeFormat): String = when (format) {
    BarcodeFormat.UPC_A -> "0$text"
    BarcodeFormat.UPC_E -> "0" + UPCEReader.convertUPCEtoUPCA(text)
    else -> text
}
