package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extension.sdk.api.packets.publish.PubcompReasonCode;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.Optional;

/**
 * @author Yannick Weber
 */
public class PubcompPacketImpl implements PubcompPacket {

    private final PubcompReasonCode pubcompReasonCode;
    private final int packetIdentifier;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubcompPacketImpl(final @NotNull PUBCOMP pubcomp) {
        pubcompReasonCode = PubcompReasonCode.valueOf(pubcomp.getReasonCode().name());
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonString = pubcomp.getReasonString();
        userProperties = pubcomp.getUserProperties().getPluginUserProperties();
    }

    public PubcompPacketImpl(final @NotNull PubcompPacket pubcompPacket) {
        this.pubcompReasonCode = pubcompPacket.getReasonCode();
        this.packetIdentifier = pubcompPacket.getPacketIdentifier();
        this.reasonString = pubcompPacket.getReasonString().orElse(null);
        this.userProperties = pubcompPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubcompReasonCode getReasonCode() {
        return pubcompReasonCode;
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
