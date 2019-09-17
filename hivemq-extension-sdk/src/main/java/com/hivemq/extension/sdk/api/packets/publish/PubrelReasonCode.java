package com.hivemq.extension.sdk.api.packets.publish;

/**
 * MQTT 5 Reason codes for PUBREL.
 * <p>
 * MQTT 3 does not support reason codes for the above mentioned MQTT packet.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public enum PubrelReasonCode {

    SUCCESS,

    PACKET_IDENTIFIER_NOT_FOUND

}