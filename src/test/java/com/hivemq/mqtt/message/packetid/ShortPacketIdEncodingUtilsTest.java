package com.hivemq.mqtt.message.packetid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author Henning Lohse
 */
public class ShortPacketIdEncodingUtilsTest {

    @Test
    public void decode_whenAcceptingAnEncodedPacketId_returnsTheOriginalPacketId() {
        for (int i = ShortPacketIdEncodingUtils.MIN_PACKET_ID; i <= ShortPacketIdEncodingUtils.MAX_PACKET_ID; i++) {
            assertEquals(i, ShortPacketIdEncodingUtils.decode(ShortPacketIdEncodingUtils.encode(i)));
        }
    }

    @Test
    public void encode_whenThePacketIdIsOutsideUnsignedShortRange_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ShortPacketIdEncodingUtils.encode(ShortPacketIdEncodingUtils.MIN_PACKET_ID - 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> ShortPacketIdEncodingUtils.encode(ShortPacketIdEncodingUtils.MAX_PACKET_ID + 1));
    }
}
