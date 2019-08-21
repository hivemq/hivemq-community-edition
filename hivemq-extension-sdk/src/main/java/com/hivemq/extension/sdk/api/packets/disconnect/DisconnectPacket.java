package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.Optional;

/**
 * @author Robin Atherton
 */
public interface DisconnectPacket {

    /**
     * Sets the server reference of the DISCONNECT packet.
     *
     * @return a String representing the  the server reference to set.
     */
    String getServerReference();

    /**
     * //TODO
     *
     * @return an enum containing the reason for disconnecting.
     */
    DisconnectReasonCode getReasonCode();

    /**
     * The reason string of the DISCONNECT packet.
     *
     * @return a string containing the disconnect reason if present.
     */
    String getReasonString();

    /**
     * Duration in seconds how long session for the client is stored.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
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
