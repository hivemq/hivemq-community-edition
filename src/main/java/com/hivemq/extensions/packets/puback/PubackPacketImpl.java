package com.hivemq.extensions.packets.puback;

import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.mqtt.message.puback.PUBACK;

import java.util.Optional;

/**
 * @author Yannick Weber
 */
@Immutable
public class PubackPacketImpl implements PubackPacket {

    private final int packetIdentifier;
    private final @NotNull AckReasonCode reasonCode;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubackPacketImpl(final @NotNull PUBACK puback) {
        packetIdentifier = puback.getPacketIdentifier();
        reasonCode = AckReasonCode.valueOf(puback.getReasonCode().name());
        reasonString = puback.getReasonString();
        userProperties = puback.getUserProperties().getPluginUserProperties();
    }

    public PubackPacketImpl(final @NotNull PubackPacket pubackPacket) {
        packetIdentifier = pubackPacket.getPacketIdentifier();
        reasonCode = pubackPacket.getReasonCode();
        reasonString = pubackPacket.getReasonString().orElse(null);
        userProperties = pubackPacket.getUserProperties();
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
