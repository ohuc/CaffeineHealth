package com.uc.caffeine.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.EAN13Writer
import com.google.zxing.oned.EAN8Writer
import com.google.zxing.oned.UPCAWriter
import com.google.zxing.oned.UPCEWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeDecoderTest {

    private val decoder = BarcodeDecoder()

    @Test
    fun readsEan13() {
        val image = render(EAN13Writer().encode("4006381333931", BarcodeFormat.EAN_13, 380, 120))
        assertEquals("4006381333931", decoder.decode(image.pixels, image.width, image.height))
    }

    @Test
    fun readsEan8() {
        val image = render(EAN8Writer().encode("96385074", BarcodeFormat.EAN_8, 300, 120))
        assertEquals("96385074", decoder.decode(image.pixels, image.width, image.height))
    }

    @Test
    fun readsBarcodeTurnedSideways() {
        val image = render(EAN13Writer().encode("4006381333931", BarcodeFormat.EAN_13, 380, 120)).turned()
        assertEquals("4006381333931", decoder.decode(image.pixels, image.width, image.height))
    }

    @Test
    fun skipsRowPadding() {
        val image = render(EAN13Writer().encode("4006381333931", BarcodeFormat.EAN_13, 380, 120))
        val stride = image.width + 32
        val padded = ByteArray(stride * image.height) { 0x55 }
        for (y in 0 until image.height) {
            System.arraycopy(image.pixels, y * image.width, padded, y * stride, image.width)
        }
        assertEquals("4006381333931", decoder.decode(padded, image.width, image.height, stride))
    }

    @Test
    fun storesUpcAAsItsEan13Form() {
        val image = render(UPCAWriter().encode("036000291452", BarcodeFormat.UPC_A, 380, 120))
        assertEquals("0036000291452", decoder.decode(image.pixels, image.width, image.height))
    }

    @Test
    fun expandsUpcE() {
        val image = render(UPCEWriter().encode("04252614", BarcodeFormat.UPC_E, 400, 120))
        assertEquals("0042100005264", decoder.decode(image.pixels, image.width, image.height))
    }

    @Test
    fun returnsNullWithoutABarcode() {
        val blank = ByteArray(400 * 200) { 0x7F }
        assertNull(decoder.decode(blank, 400, 200))
    }

    private class Image(val pixels: ByteArray, val width: Int, val height: Int) {
        fun turned(): Image {
            val turned = ByteArray(pixels.size)
            for (y in 0 until height) {
                for (x in 0 until width) turned[x * height + (height - 1 - y)] = pixels[y * width + x]
            }
            return Image(turned, width = height, height = width)
        }
    }

    private fun render(matrix: BitMatrix): Image {
        val pixels = ByteArray(matrix.width * matrix.height) { i ->
            if (matrix[i % matrix.width, i / matrix.width]) 0 else 0xFF.toByte()
        }
        return Image(pixels, matrix.width, matrix.height)
    }
}
