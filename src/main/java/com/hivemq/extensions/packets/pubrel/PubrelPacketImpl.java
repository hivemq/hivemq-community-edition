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
 * @author Silvio Giebl
 */
public class PubrelPacketImpl implements PubrelPacket {

    private final int packetIdentifier;
    private final @NotNull PubrelReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubrelPacketImpl(final @NotNull PUBREL pubrel) {
        packetIdentifier = pubrel.getPacketIdentifier();
        reasonCode = PubrelReasonCode.valueOf(pubrel.getReasonCode().name());
        reasonString = pubrel.getReasonString();
        userProperties = pubrel.getUserProperties().getPluginUserProperties();
    }

    public PubrelPacketImpl(final @NotNull PubrelPacket pubrelPacket) {
        packetIdentifier = pubrelPacket.getPacketIdentifier();
        reasonCode = pubrelPacket.getReasonCode();
        reasonString = pubrelPacket.getReasonString().orElse(null);
        userProperties = pubrelPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubrelReasonCode getReasonCode() {
        return reasonCode;
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
