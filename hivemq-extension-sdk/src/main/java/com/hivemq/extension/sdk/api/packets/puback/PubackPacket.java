package com.hivemq.extension.sdk.api.packets.puback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;

import java.util.Optional;

/**
 * Represents a PUBACK packet.
 * <p>
 * Contains all values of an MQTT 5 PUBACK, but will also used to represent MQTT 3 PUBACK packets.
 *
 * @author Yannick Weber
 */
public interface PubackPacket {

    int getPacketIdentifier();

    /**
     * The reason code from the PUBACK packet.
     * <p>
     *
     * @return The puback reason code.
     * @see AckReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     */
    @NotNull AckReasonCode getReasonCode();

    /**
     * The reason string of the PUBACK packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the puback reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the PUBACK packet.
     * <p>
     * The properties will always be empty for an MQTT 3 client.
     *
     * @return The user properties.
     */
    @NotNull UserProperties getUserProperties();

}
