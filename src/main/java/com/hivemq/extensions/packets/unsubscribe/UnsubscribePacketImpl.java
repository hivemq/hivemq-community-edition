package com.hivemq.extensions.packets.unsubscribe;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.List;

/**
 * @author Robin Atherton
 */
public class UnsubscribePacketImpl implements UnsubscribePacket {

    private final @NotNull List<String> topics;
    private final @NotNull UserProperties userProperties;

    public UnsubscribePacketImpl(final UNSUBSCRIBE unsubscribe) {
        this.topics = unsubscribe.getTopics();
        this.userProperties = unsubscribe.getUserProperties().getPluginUserProperties();
    }

    public UnsubscribePacketImpl(final ModifiableUnsubscribePacket unsubscribe) {
        this.topics = unsubscribe.getTopics();
        this.userProperties = unsubscribe.getUserProperties();
    }

    @Override
    public @NotNull List<String> getTopics() {
        return this.topics;
    }

    @Override
    public UserProperties getUserProperties() {
        return this.userProperties;
    }

}
