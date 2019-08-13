package com.hivemq.extensions.packets.puback;

import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.Optional;

/**
 * @author Yannick Weber
 * @since 4.2.0
 */
public class PubackPacketImpl implements PubackPacket {

    private final AckReasonCode ackReasonCode;
    private final int packetIdentifier;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubackPacketImpl(final @NotNull PUBACK puback) {
        ackReasonCode = AckReasonCode.valueOf(puback.getReasonCode().name());
        packetIdentifier = puback.getPacketIdentifier();
        reasonString = puback.getReasonString();
        userProperties = puback.getUserProperties().getPluginUserProperties();
    }

    public PubackPacketImpl(final @NotNull PubackPacket pubackPacket) {
        this.ackReasonCode = pubackPacket.getReasonCode();
        this.packetIdentifier = pubackPacket.getPacketIdentifier();
        this.reasonString = pubackPacket.getReasonString().orElse(null);
        this.userProperties = pubackPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @com.hivemq.annotations.NotNull
    @Override
    public AckReasonCode getReasonCode() {
        return ackReasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }
}
