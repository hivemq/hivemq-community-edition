package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.pubcomp.PubcompPacket;
import com.hivemq.extension.sdk.api.packets.publish.PubcompReasonCode;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;

import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompPacketImpl implements PubcompPacket {

    private final @NotNull PubcompReasonCode reasonCode;
    private final int packetIdentifier;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubcompPacketImpl(final @NotNull PUBCOMP pubcomp) {
        reasonCode = PubcompReasonCode.valueOf(pubcomp.getReasonCode().name());
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonString = pubcomp.getReasonString();
        userProperties = pubcomp.getUserProperties().getPluginUserProperties();
    }

    public PubcompPacketImpl(final @NotNull PubcompPacket pubcompPacket) {
        reasonCode = pubcompPacket.getReasonCode();
        packetIdentifier = pubcompPacket.getPacketIdentifier();
        reasonString = pubcompPacket.getReasonString().orElse(null);
        userProperties = pubcompPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubcompReasonCode getReasonCode() {
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
