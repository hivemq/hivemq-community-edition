package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * An outbound {@link DisconnectPacket} that can be modified before it is sent to the client.
 *
 * @author Robin Atherton
 */
public interface ModifiableOutboundDisconnectPacket extends DisconnectPacket {

    /**
     * Set a {@link DisconnectReasonCode} for the DISCONNECT packet.
     *
     * @param reasonCode The reason code to set.
     * @throws NullPointerException  If reason code is <b>null</b>.
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
     * Set the server reference.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param serverReference The server reference for the DISCONNECT.
     * @throws NullPointerException     If the server reference is <b>null</b>.
     * @throws IllegalArgumentException If the server reference is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the server reference exceeds the UTF-8 string length limit.
     */
    void setServerReference(@NotNull String serverReference);

    /**
     * Gets the modifiable {@link UserProperties} of the DISCONNECT packet.
     *
     * @return Modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();

    boolean isModified();
}