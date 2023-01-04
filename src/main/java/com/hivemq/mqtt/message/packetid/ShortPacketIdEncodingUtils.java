package com.hivemq.mqtt.message.packetid;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Encodes and decodes an integer packet ID of unsigned short range to a signed short and vice-versa.
 *
 * @author Henning Lohse
 */
public class ShortPacketIdEncodingUtils {

    @VisibleForTesting
    static final int MIN_PACKET_ID = 0;

    @VisibleForTesting
    static final int MAX_PACKET_ID = 65535;

    private static final int SHORT_BITMASK = 0xFFFF;

    public static short encode(final int packetId) {
        Preconditions.checkArgument(packetId >= MIN_PACKET_ID);
        Preconditions.checkArgument(packetId <= MAX_PACKET_ID);
        return (short) packetId;
    }

    public static int decode(final short encodedPackedId) {
        return encodedPackedId & SHORT_BITMASK;
    }
}
