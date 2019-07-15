package com.hivemq.extension.sdk.api.packets.connack;


import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public interface ModifiableConnackPacket extends ConnackPacket {

    /**
     * Set a {@link ConnackReasonCode} to the CONNACK packet.
     * <p>
     * Switching from successful to unsuccessful and vice versa is not supported.
     *
     * @param reasonCode The reason code to set.
     * @throws NullPointerException  If reason code is <null>.
     * @throws IllegalStateException If switching from successful reason code to unsuccessful reason code or vice versa.
     * @since 4.2.0
     */
    void setReasonCode(@NotNull ConnackReasonCode reasonCode);

    /**
     * Set the reason string.
     * <p>
     * A reason must not be set for a successful connack.
     *
     * @param reasonString The reason string to set.
     * @throws IllegalStateException    If reason code is {@link ConnackReasonCode#SUCCESS}.
     * @throws IllegalArgumentException If the reason string is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the reason string exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setReasonString(@Nullable String reasonString);

    /**
     * Get the modifiable {@link UserProperties} of the CONNACK packet.
     *
     * @return Modifiable user properties.
     * @since 4.2.0
     */
    @Override
    @NotNull
    ModifiableUserProperties getUserProperties();

    /**
     * Set the response information.
     *
     * @param responseInformation The new response information for the CONNACK.
     * @throws IllegalArgumentException If the response information is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the response information exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setResponseInformation(@Nullable String responseInformation);

    /**
     * Set the server reference
     *
     * @param serverReference The new server reference for the CONNACK.
     * @throws IllegalArgumentException If the server reference is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the server reference exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setServerReference(@Nullable String serverReference);

    /**
     * @return True if the packet was modified, else false.
     */
    boolean isModified();
}
