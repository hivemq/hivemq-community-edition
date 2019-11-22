package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * An outbound {@link DisconnectPacket} that can be modified before it is sent to the client.
 * <p>
 * This packet is only used for MQTT 5 clients as MQTT 3.x does not support outbound disconnects.
 *
 * @author Robin Atherton
 * @author Silvio Giebl
 */
@DoNotImplement
public interface ModifiableOutboundDisconnectPacket extends DisconnectPacket {

    /**
     * Set the {@link DisconnectReasonCode} of the DISCONNECT packet.
     *
     * @param reasonCode The reason code to set.
     * @throws NullPointerException     If the reason code is <code>null</code>.
     * @throws IllegalArgumentException If the disconnect reason code must not be used for outbound disconnect packets
     *                                  from the server to a client.
     * @see DisconnectReasonCode What reason codes exist for outbound disconnect packets from the server to a
     *         client.
     */
    void setReasonCode(@NotNull DisconnectReasonCode reasonCode);

    /**
     * Set the reason string of the DISCONNECT packet.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param reasonString The reason string to set or <code>null</code> to remove the reason string.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * Set the server reference of the DISCONNECT packet.
     * <p>
     * This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is ignored.
     *
     * @param serverReference The server reference to set or <code>null</code> to remove the server reference.
     * @throws IllegalArgumentException If the server reference is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the server reference exceeds the UTF-8 string length limit.
     */
    void setServerReference(@Nullable String serverReference);

    /**
     * The modifiable {@link UserProperties} of the DISCONNECT packet.
     *
     * @return The modifiable user properties.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();
}