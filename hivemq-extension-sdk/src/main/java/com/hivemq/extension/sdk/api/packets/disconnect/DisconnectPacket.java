package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * Represents a DISCONNECT packet.
 * <p>
 * Contains all values of an MQTT 5 DISCONNECT, but will also used to represent MQTT 3 disconnects.
 *
 * @author Robin Atherton
 */
public interface DisconnectPacket {

    /**
     * Gets the server reference of the DISCONNECT packet.
     *
     * @return A String representing the server reference.
     */
    String getServerReference();

    /**
     * The reason code of the DISCONNECT packet.
     *
     * @return An enum containing the reason for disconnecting.
     */
    DisconnectReasonCode getReasonCode();

    /**
     * The reason String of the DISCONNECT packet.
     *
     * @return A String containing the disconnect reason if present.
     */
    String getReasonString();

    /**
     * The duration in seconds for which the clients session is stored.
     *
     * @return A long representing the session expiry interval.
     */
    long getSessionExpiryInterval();

    /**
     * The user properties from the DISCONNECT packet.
     *
     * @return The {@link UserProperties} of the DISCONNECT packet.
     */
    @NotNull UserProperties getUserProperties();
}
