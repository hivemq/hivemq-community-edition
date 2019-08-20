package com.hivemq.extensions.packets.pubrec;

import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.mqtt.message.pubrec.PUBREC;

import java.util.Optional;

/**
 * @author Yannick Weber
 */
public class PubrecPacketImpl implements PubrecPacket {

    private final AckReasonCode ackReasonCode;
    private final int packetIdentifier;
    private final @Nullable String reasonString;
    private final @NotNull UserProperties userProperties;

    public PubrecPacketImpl(final @NotNull PUBREC pubrec) {
        ackReasonCode = AckReasonCode.valueOf(pubrec.getReasonCode().name());
        packetIdentifier = pubrec.getPacketIdentifier();
        reasonString = pubrec.getReasonString();
        userProperties = pubrec.getUserProperties().getPluginUserProperties();
    }

    public PubrecPacketImpl(final @NotNull PubrecPacket pubrecPacket) {
        this.ackReasonCode = pubrecPacket.getReasonCode();
        this.packetIdentifier = pubrecPacket.getPacketIdentifier();
        this.reasonString = pubrecPacket.getReasonString().orElse(null);
        this.userProperties = pubrecPacket.getUserProperties();
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull AckReasonCode getReasonCode() {
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
