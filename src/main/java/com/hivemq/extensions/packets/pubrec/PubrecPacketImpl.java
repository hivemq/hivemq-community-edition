package com.hivemq.extensions.packets.pubrec;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
@Immutable
public class PubrecPacketImpl implements PubrecPacket {

    private final int packetIdentifier;
    private final @NotNull AckReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubrecPacketImpl(final @NotNull PUBREC pubrec) {
        packetIdentifier = pubrec.getPacketIdentifier();
        reasonCode = AckReasonCode.valueOf(pubrec.getReasonCode().name());
        reasonString = pubrec.getReasonString();
        userProperties = pubrec.getUserProperties().getPluginUserProperties();
    }

    public PubrecPacketImpl(final @NotNull PubrecPacket pubrecPacket) {
        packetIdentifier = pubrecPacket.getPacketIdentifier();
        reasonCode = pubrecPacket.getReasonCode();
        reasonString = pubrecPacket.getReasonString().orElse(null);
        userProperties = pubrecPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull AckReasonCode getReasonCode() {
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
