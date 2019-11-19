package com.hivemq.extension.sdk.api.packets.pubrel;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PubrelReasonCode;

import java.util.Optional;

/**
 * Represents a PUBREL packet.
 * <p>
 * Contains all values of an MQTT 5 PUBREL, but will also used to represent MQTT 3 PUBREL packets.
 *
 * @author Yannick Weber
 */
@DoNotImplement
@Immutable
public interface PubrelPacket {

    /**
     * The packet identifier of the PUBREL.
     *
     * @return The packet identifier.
     * @since 4.3.0
     */
    int getPacketIdentifier();

    /**
     * The reason code from the PUBREL packet.
     * <p>
     *
     * @return The pubrel reason code.
     * @see PubrelReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     */
    @NotNull PubrelReasonCode getReasonCode();

    /**
     * The reason string of the PUBREL packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the pubrel reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the PUBREL packet.
     * <p>
     * The properties will always be empty for an MQTT 3 client.
     *
     * @return The user properties.
     */
    @NotNull UserProperties getUserProperties();
}
