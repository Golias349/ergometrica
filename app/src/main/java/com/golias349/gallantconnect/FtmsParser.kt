package com.golias349.gallantconnect

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

data class BikeData(
    val watts: Int? = null,
    val cadence: Double? = null,
    val speedKmh: Double? = null,
    val distanceM: Int? = null,
    val heartRate: Int? = null
)

object FtmsParser {
    /**
     * FTMS Indoor Bike Data (0x2AD2).
     *
     * Flags are little-endian:
     * bit 0: More Data
     * bit 1: Average Speed Present
     * bit 2: Instantaneous Cadence Present
     * bit 3: Average Cadence Present
     * bit 4: Total Distance Present
     * bit 5: Resistance Level Present
     * bit 6: Instantaneous Power Present
     * bit 7: Average Power Present
     * bit 8: Expended Energy Present
     * bit 9: Heart Rate Present
     * bit 10: Metabolic Equivalent Present
     * bit 11: Elapsed Time Present
     * bit 12: Remaining Time Present
     */
    fun parse(bytes: ByteArray): BikeData {
        if (bytes.size < 2) return BikeData()

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val flags = buffer.short.toInt() and 0xFFFF

        var speedKmh: Double? = null
        var cadence: Double? = null
        var distanceM: Int? = null
        var watts: Int? = null
        var heartRate: Int? = null

        // Instantaneous speed is UInt16 in 0.01 km/h.
        if ((flags and 0x0001) == 0) {
            if (buffer.remaining() >= 2) {
                speedKmh = (buffer.short.toInt() and 0xFFFF) / 100.0
            }
        }

        // Average speed present
        if ((flags and 0x0002) != 0 && buffer.remaining() >= 2) {
            buffer.short
        }

        // Instantaneous cadence, 0.5 RPM
        if ((flags and 0x0004) != 0 && buffer.remaining() >= 2) {
            cadence = (buffer.short.toInt() and 0xFFFF) / 2.0
        }

        // Average cadence, 0.5 RPM
        if ((flags and 0x0008) != 0 && buffer.remaining() >= 2) {
            buffer.short
        }

        // Total distance, UInt24 meters
        if ((flags and 0x0010) != 0 && buffer.remaining() >= 3) {
            val b0 = buffer.get().toInt() and 0xFF
            val b1 = buffer.get().toInt() and 0xFF
            val b2 = buffer.get().toInt() and 0xFF
            distanceM = b0 or (b1 shl 8) or (b2 shl 16)
        }

        // Resistance level, SInt16
        if ((flags and 0x0020) != 0 && buffer.remaining() >= 2) {
            buffer.short
        }

        // Instantaneous power, SInt16 watts
        if ((flags and 0x0040) != 0 && buffer.remaining() >= 2) {
            watts = buffer.short.toInt()
        }

        // Average power, SInt16
        if ((flags and 0x0080) != 0 && buffer.remaining() >= 2) {
            buffer.short
        }

        // Expended energy: total 16-bit + per hour 16-bit + per minute 8-bit
        if ((flags and 0x0100) != 0 && buffer.remaining() >= 5) {
            buffer.position(buffer.position() + 5)
        }

        // Heart rate, UInt8
        if ((flags and 0x0200) != 0 && buffer.remaining() >= 1) {
            heartRate = buffer.get().toInt() and 0xFF
        }

        return BikeData(
            watts = watts,
            cadence = cadence,
            speedKmh = speedKmh,
            distanceM = distanceM,
            heartRate = heartRate
        )
    }

    fun hex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it) }
}
