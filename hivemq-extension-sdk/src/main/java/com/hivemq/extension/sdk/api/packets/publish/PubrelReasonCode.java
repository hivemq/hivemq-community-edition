package com.hivemq.extension.sdk.api.packets.publish;

/**
 * MQTT 5 Reason codes for PUBREL.
 * <p>
 * MQTT 3 does not support reason codes for the above mentioned MQTT packet.
 *
 * @author Yannick Weber
 */
public enum PubrelReasonCode {

    SUCCESS,

    PACKET_IDENTIFIER_NOT_FOUND

}