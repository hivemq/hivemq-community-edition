package com.hivemq.extensions.packets.pubrel;

import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PubrelReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelPacket;
import com.hivemq.mqtt.message.pubrel.PUBREL;

import java.util.Optional;

/**
 * @author Yannick Weber
 */
public class PubrelPacketImpl implements PubrelPacket {

    private final PubrelReasonCode pubrelReasonCode;
    private final int packetIdentifier;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubrelPacketImpl(final @NotNull PUBREL pubrel) {
        pubrelReasonCode = PubrelReasonCode.valueOf(pubrel.getReasonCode().name());
        packetIdentifier = pubrel.getPacketIdentifier();
        reasonString = pubrel.getReasonString();
        userProperties = pubrel.getUserProperties().getPluginUserProperties();
    }

    public PubrelPacketImpl(final @NotNull PubrelPacket pubrelPacket) {
        this.pubrelReasonCode = pubrelPacket.getReasonCode();
        this.packetIdentifier = pubrelPacket.getPacketIdentifier();
        this.reasonString = pubrelPacket.getReasonString().orElse(null);
        this.userProperties = pubrelPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubrelReasonCode getReasonCode() {
        return pubrelReasonCode;
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
