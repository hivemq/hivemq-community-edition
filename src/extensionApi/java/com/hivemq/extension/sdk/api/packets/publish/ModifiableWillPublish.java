package com.hivemq.extension.sdk.api.packets.publish;

import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;

/**
 * @author Lukas Brandl
 */
public interface ModifiableWillPublish extends WillPublishPacket, ModifiablePublishPacket {

    /**
     * Sets the will delay
     *
     * @param willDelay The new will delay for the will publish in seconds
     * @throws IllegalArgumentException If the delay is less than zero or more than '4294967295'.
     * @since 4.2.0
     */
    void setWillDelay(long willDelay);
}
