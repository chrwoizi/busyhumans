package com.c5000.mastery.backend

import _root_.java.util.UUID
import com.google.api.client.util.Base64

object EncodingHelper {

    def decode64(s: String) : UUID = toUUID(Base64.decodeBase64(s))
    def encode64(id: UUID): String = Base64.encodeBase64String(asByteArray(id))

    private def asByteArray(uuid: UUID): Array[Byte] = {

        val msb = uuid.getMostSignificantBits
        val lsb = uuid.getLeastSignificantBits
        val buffer = new Array[Byte](16)

        for (i <- 0 to 7) {
            buffer(i) = (msb >>> 8 * (7 - i)).toByte
        }
        for (i <- 8 to 15) {
            buffer(i) = (lsb >>> 8 * (7 - i)).toByte
        }

        return buffer
    }

    private def toUUID(byteArray: Array[Byte]): UUID = {

        var msb = 0L
        var lsb = 0L

        for (i <- 0 to 7)
            msb = (msb << 8) | (byteArray(i) & 0xff)
        for (i <- 8 to 15)
            lsb = (lsb << 8) | (byteArray(i) & 0xff)

        return new UUID(msb, lsb)
    }
}
