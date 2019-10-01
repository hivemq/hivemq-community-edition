package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * @author Robin Atherton
 */
public interface ModifiableOutboundDisconnectPacket extends DisconnectPacket {

    /**
     * Sets a reasonString for the DISCONNECT packet.
     *
     * @param reasonString the reason to be set as a String.
     * @since 4.3.0
     */
    void setReasonString(@NotNull String reasonString);

    /**
     * Sets a reasonCode for the DISCONNECT packet.
     *
     * @param reasonCode the reason to be set as a enum.
     * @since 4.3.0
     */
    void setReasonCode(@NotNull DisconnectReasonCode reasonCode);

    /**
     * Sets the server reference of the DISCONNECT packet.
     *
     * @param serverReference the server reference value to be set.
     * @since 4.3.0
     */
    void setServerReference(@NotNull String serverReference);

    /**
     * Gets the modifiable {@link UserProperties} of the DISCONNECT packet.
     *
     * @return Modifiable user properties.
     * @since 4.3.0
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();

    boolean isModified();
}
