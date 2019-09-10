package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * @author Robin Atherton
 */
public interface DisconnectPacket {

    /**
     * Gets the server reference of the DISCONNECT packet.
     *
     * @return a String representing the server reference.
     */
    String getServerReference();

    /**
     * The reason code of the DISCONNECT packet.
     *
     * @return an enum containing the reason for disconnecting.
     */
    DisconnectReasonCode getReasonCode();

    /**
     * The reason String of the DISCONNECT packet.
     *
     * @return a String containing the disconnect reason if present.
     */
    String getReasonString();

    /**
     * The Duration in seconds for which the clients session is stored.
     *
     * @return a long representing the session expiry interval.
     */
    long getSessionExpiryInterval();

    /**
     * The user properties from the DISCONNECT packet.
     *
     * @return The {@link UserProperties} of the DISCONNECT packet.
     */
    @NotNull UserProperties getUserProperties();
}
