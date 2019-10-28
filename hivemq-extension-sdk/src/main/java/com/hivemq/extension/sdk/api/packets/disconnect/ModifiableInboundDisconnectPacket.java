package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * An inbound {@link DisconnectPacket} that can be modified before it is sent to the server.
 *
 * @author Robin Atherton
 */
public interface ModifiableInboundDisconnectPacket extends DisconnectPacket {

    /**
     * Set a {@link DisconnectReasonCode} for the DISCONNECT packet.
     *
     * @param reasonCode The reason code to set.
     * @throws NullPointerException If reason code is <b>null</b>.
     * @see DisconnectReasonCode What reason codes exist for disconnecting.
     */
    void setReasonCode(@NotNull DisconnectReasonCode reasonCode);

    /**
     * Set the reason string.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param reasonString The reason string to set.
     * @throws NullPointerException     If reason String is <b>null</b>.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     */
    void setReasonString(@NotNull String reasonString);

    /**
     * Sets the session expiry interval of the DISCONNECT packet.
     *
     * @param expiryInterval A settable value indicating the interval after which the session will expire.
     * @throws IllegalStateException    If the session expiry interval is modified while it already is 0.
     * @throws IllegalArgumentException If the session expiry interval is less than 0.
     * @throws IllegalArgumentException If the session expiry interval is greater than the configured maximum.
     */
    void setSessionExpiryInterval(long expiryInterval);

    /**
     * Gets the modifiable {@link UserProperties} of the DISCONNECT packet.
     *
     * @return Modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();

    boolean isModified();
}
