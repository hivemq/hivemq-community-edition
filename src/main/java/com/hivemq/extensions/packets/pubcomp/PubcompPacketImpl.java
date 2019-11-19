package com.hivemq.extensions.packets.pubcomp;

import com.hivemq.annotations.Immutable;
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
@Immutable
public class PubcompPacketImpl implements PubcompPacket {

    private final int packetIdentifier;
    private final @NotNull PubcompReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubcompPacketImpl(final @NotNull PUBCOMP pubcomp) {
        packetIdentifier = pubcomp.getPacketIdentifier();
        reasonCode = PubcompReasonCode.valueOf(pubcomp.getReasonCode().name());
        reasonString = pubcomp.getReasonString();
        userProperties = pubcomp.getUserProperties().getPluginUserProperties();
    }

    public PubcompPacketImpl(final @NotNull PubcompPacket pubcompPacket) {
        packetIdentifier = pubcompPacket.getPacketIdentifier();
        reasonCode = pubcompPacket.getReasonCode();
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
