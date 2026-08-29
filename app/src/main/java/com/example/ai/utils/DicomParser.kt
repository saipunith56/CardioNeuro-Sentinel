package com.example.ai.utils

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DicomParser(private val inputStream: InputStream) {
    var rows: Int = 0
        private set
    var columns: Int = 0
        private set
    var bitsAllocated: Int = 0
        private set
    var bitsStored: Int = 0
        private set
    var pixelRepresentation: Int = 0 // 0 = Unsigned, 1 = Signed
        private set
    var rescaleIntercept: Float = 0.0f
        private set
    var rescaleSlope: Float = 1.0f
        private set
    var pixelData: ByteArray? = null
        private set
    var transferSyntaxUid: String = "1.2.840.10008.1.2.1" // Default: Explicit VR Little Endian
        private set

    private var isLittleEndian = true
    private var isExplicit = true

    fun parse() {
        val stream = BufferedInputStream(inputStream)

        // 1. Read preamble (128 bytes)
        val preamble = ByteArray(128)
        var read = stream.read(preamble)
        if (read < 128) throw IllegalArgumentException("Malformed DICOM: Premature EOF reading preamble")

        // 2. Read prefix (4 bytes) - "DICM"
        val prefix = ByteArray(4)
        read = stream.read(prefix)
        if (read < 4) throw IllegalArgumentException("Malformed DICOM: Premature EOF reading prefix")
        if (String(prefix) != "DICM") {
            throw IllegalArgumentException("Malformed DICOM: Missing 'DICM' prefix")
        }

        // 3. Read elements sequentially
        while (true) {
            val group = readUShort(stream) ?: break
            val element = readUShort(stream) ?: throw IllegalArgumentException("Malformed Element in DICOM")

            val tag = (group.toInt() shl 16) or element.toInt()

            // In DICOM, group 0002 (Meta Information) is ALWAYS Explicit Little Endian
            val elementExplicit = if (group.toInt() == 0x0002) true else isExplicit

            var vr = ""
            var length: Long = 0

            if (elementExplicit) {
                // Explicit VR: VR is encoded in 2 bytes
                val vrBytes = readBytes(stream, 2)
                vr = String(vrBytes)

                val longLengthVRs = setOf("OB", "OW", "SQ", "UN", "UT", "OD", "OF", "OL", "OV", "UC", "UR")
                if (longLengthVRs.contains(vr)) {
                    // 2 bytes reserved (0), then 4 bytes length
                    readBytes(stream, 2) // skip reserved
                    length = readUInt(stream)
                } else {
                    // 2 bytes length
                    length = (readUShort(stream) ?: throw IllegalArgumentException("Malformed Element in DICOM: Premature EOF reading element length")).toLong()
                }
            } else {
                // Implicit VR: 4 bytes length (no VR field)
                length = readUInt(stream)
            }

            // Handle undefined length (0xFFFFFFFF) - typically for Sequence (SQ) or Pixel Data encapsulation
            if (length == 0xFFFFFFFFL) {
                if (tag == 0x7FE00010) {
                    // Encapsulated / Compressed Pixel Data is not supported in this lightweight parser
                    throw IllegalArgumentException("Unsupported Transfer Syntax: Compressed/Encapsulated Pixel Data (Undefined Length)")
                }
                // For sequence or other groups, we must read until Sequence Delimitation Item
                skipSequenceUntilDelimitation(stream)
                continue
            }

            // Read or skip value
            if (length > 0) {
                when (tag) {
                    0x00020010 -> { // Transfer Syntax UID
                        val valueBytes = readBytes(stream, length.toInt())
                        val uid = String(valueBytes).trim('\u0000', ' ')
                        transferSyntaxUid = uid
                        applyTransferSyntax(uid)
                    }
                    0x00280010 -> { // Rows
                        val valueBytes = readBytes(stream, length.toInt())
                        rows = bytesToInt(valueBytes)
                    }
                    0x00280011 -> { // Columns
                        val valueBytes = readBytes(stream, length.toInt())
                        columns = bytesToInt(valueBytes)
                    }
                    0x00280100 -> { // Bits Allocated
                        val valueBytes = readBytes(stream, length.toInt())
                        bitsAllocated = bytesToInt(valueBytes)
                    }
                    0x00280101 -> { // Bits Stored
                        val valueBytes = readBytes(stream, length.toInt())
                        bitsStored = bytesToInt(valueBytes)
                    }
                    0x00280103 -> { // Pixel Representation
                        val valueBytes = readBytes(stream, length.toInt())
                        pixelRepresentation = bytesToInt(valueBytes)
                    }
                    0x00281052 -> { // Rescale Intercept
                        val valueBytes = readBytes(stream, length.toInt())
                        rescaleIntercept = String(valueBytes).trim('\u0000', ' ').toFloatOrNull() ?: 0.0f
                    }
                    0x00281053 -> { // Rescale Slope
                        val valueBytes = readBytes(stream, length.toInt())
                        rescaleSlope = String(valueBytes).trim('\u0000', ' ').toFloatOrNull() ?: 1.0f
                    }
                    0x7FE00010 -> { // Pixel Data
                        pixelData = readBytes(stream, length.toInt())
                    }
                    else -> {
                        skipBytes(stream, length)
                    }
                }
            }
        }

        // Post-parsing validations
        if (rows <= 0 || columns <= 0) {
            throw IllegalArgumentException("Malformed DICOM: Unsupported image dimensions (Rows=$rows, Cols=$columns)")
        }
        if (pixelData == null || pixelData!!.isEmpty()) {
            throw IllegalArgumentException("Malformed DICOM: Missing pixel data")
        }
    }

    private fun applyTransferSyntax(uid: String) {
        when (uid) {
            "1.2.840.10008.1.2.1" -> { // Explicit VR Little Endian
                isLittleEndian = true
                isExplicit = true
            }
            "1.2.840.10008.1.2" -> { // Implicit VR Little Endian
                isLittleEndian = true
                isExplicit = false
            }
            "1.2.840.10008.1.2.2" -> { // Explicit VR Big Endian
                isLittleEndian = false
                isExplicit = true
            }
            else -> {
                throw IllegalArgumentException("Unsupported Transfer Syntax: $uid")
            }
        }
    }

    private fun readUShort(stream: InputStream): Int? {
        val b1 = stream.read()
        val b2 = stream.read()
        if (b1 == -1 || b2 == -1) return null

        return if (isLittleEndian) {
            (b2 shl 8) or b1
        } else {
            (b1 shl 8) or b2
        }
    }

    private fun readUInt(stream: InputStream): Long {
        val b1 = stream.read()
        val b2 = stream.read()
        val b3 = stream.read()
        val b4 = stream.read()
        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) {
            throw IllegalArgumentException("Malformed DICOM: Premature EOF reading UInt")
        }
        return if (isLittleEndian) {
            ((b4.toLong() and 0xFF) shl 24) or
            ((b3.toLong() and 0xFF) shl 16) or
            ((b2.toLong() and 0xFF) shl 8) or
            (b1.toLong() and 0xFF)
        } else {
            ((b1.toLong() and 0xFF) shl 24) or
            ((b2.toLong() and 0xFF) shl 16) or
            ((b3.toLong() and 0xFF) shl 8) or
            (b4.toLong() and 0xFF)
        }
    }

    private fun readBytes(stream: InputStream, count: Int): ByteArray {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val numRead = stream.read(buffer, offset, count - offset)
            if (numRead == -1) throw IllegalArgumentException("Malformed DICOM: Premature EOF reading bytes")
            offset += numRead
        }
        return buffer
    }

    private fun skipBytes(stream: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) {
                if (stream.read() == -1) throw IllegalArgumentException("Malformed DICOM: Premature EOF on skip")
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        if (bytes.isEmpty()) return 0
        if (bytes.size == 2) {
            val buffer = ByteBuffer.wrap(bytes).order(if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
            return buffer.short.toInt() and 0xFFFF
        }
        if (bytes.size == 4) {
            val buffer = ByteBuffer.wrap(bytes).order(if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
            return buffer.int
        }
        return 0
    }

    private fun skipSequenceUntilDelimitation(stream: InputStream) {
        while (true) {
            val group = readUShort(stream) ?: throw IllegalArgumentException("Malformed DICOM: EOF within sequence skipping")
            val element = readUShort(stream) ?: throw IllegalArgumentException("Malformed DICOM: EOF within sequence skipping")
            val length = readUInt(stream)

            if (group == 0xFFFE && element == 0xE0DD) {
                break
            }
            if (length > 0 && length != 0xFFFFFFFFL) {
                skipBytes(stream, length)
            }
        }
    }

    fun getPixels(): IntArray {
        val data = pixelData ?: throw IllegalStateException("Pixel data has not been parsed")
        val numPixels = rows * columns
        val pixels = IntArray(numPixels)

        val byteOrder = if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

        if (bitsAllocated == 16) {
            val buffer = ByteBuffer.wrap(data).order(byteOrder)
            val count = Math.min(numPixels, data.size / 2)
            for (i in 0 until count) {
                if (pixelRepresentation == 1) {
                    pixels[i] = buffer.short.toInt()
                } else {
                    pixels[i] = buffer.short.toInt() and 0xFFFF
                }
            }
        } else if (bitsAllocated == 8) {
            val count = Math.min(numPixels, data.size)
            for (i in 0 until count) {
                if (pixelRepresentation == 1) {
                    pixels[i] = data[i].toInt()
                } else {
                    pixels[i] = data[i].toInt() and 0xFF
                }
            }
        } else {
            throw IllegalArgumentException("Unsupported Bits Allocated: $bitsAllocated (Only 8-bit and 16-bit uncompressed grayscale is supported)")
        }

        return pixels
    }
}
