package com.hivemq.extension.sdk.api.packets.pubcomp;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PubcompReasonCode;

import java.util.Optional;

/**
 * Represents a PUBCOMP packet.
 * <p>
 * Contains all values of an MQTT 5 PUBCOMP, but will also used to represent MQTT 3 PUBCOMP packets.
 *
 * @author Yannick Weber
 */
@DoNotImplement
public interface PubcompPacket {

    /**
     * The packet identifier of the PUBCOMP.
     *
     * @return The packet identifier.
     */
    int getPacketIdentifier();

    /**
     * The reason code from the PUBCOMP packet.
     * <p>
     *
     * @return The pubcomp reason code.
     * @see PubcompReasonCode How reason codes are translated from MQTT 5 to MQTT 3.
     */
    @NotNull PubcompReasonCode getReasonCode();

    /**
     * The reason string of the PUBCOMP packet.
     * <p>
     * For an MQTT 3 client this {@link Optional} for the MQTT 5 property will always be empty.
     *
     * @return An {@link Optional} containing the pubcomp reason string if present.
     */
    @NotNull Optional<String> getReasonString();

    /**
     * The user properties from the PUBCOMP packet.
     * <p>
     * The properties will always be empty for an MQTT 3 client.
     *
     * @return The user properties.
     */
    @NotNull UserProperties getUserProperties();
}
