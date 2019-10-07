package com.hivemq.extension.sdk.api.packets.pubrec;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;

import java.util.Optional;

/**
 * Represents a PUBREC packet.
 * <p>
 * Contains all values of an MQTT 5 PUBREC, but will also used to represent MQTT 3 PUBREC packets.
 *
 * @author Yannick Weber
 */
public interface PubrecPacket {

    /**
     * The packet identifier of the PUBREC.
     *
     * @return The packet identifier.
     * @since 4.3.0
     */
    int getPacketIdentifier();

    /**
     * The reason code from the PUBREC packet.
     * <p>
     *
     * @return The pubrec reason code.
     * @see AckReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     */
    @NotNull AckReasonCode getReasonCode();

    /**
     * The reason string of the PUBREC packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the pubrec reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the PUBREC packet.
     * <p>
     * The properties will always be empty for an MQTT 3 client.
     *
     * @return The user properties.
     */
    @NotNull UserProperties getUserProperties();

}
