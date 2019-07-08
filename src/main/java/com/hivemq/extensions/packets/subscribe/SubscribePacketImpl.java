package com.hivemq.extensions.packets.subscribe;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class SubscribePacketImpl implements SubscribePacket {

    private final @NotNull List<Subscription> subscriptionList;
    private final @NotNull UserProperties userProperties;
    private final int subscriptionIdentifier;
    private final int packetIdentifier;

    public SubscribePacketImpl(final @NotNull SUBSCRIBE subscribe) {

        subscriptionList = subscribe.getTopics().stream()
                .map((Function<Topic, Subscription>) topic -> new SubscriptionImpl(topic))
                .collect(Collectors.toList());
        userProperties = subscribe.getUserProperties().getPluginUserProperties();
        subscriptionIdentifier = subscribe.getSubscriptionIdentifier();
        packetIdentifier = subscribe.getPacketIdentifier();
    }

    public SubscribePacketImpl(final @NotNull ModifiableSubscribePacket subscribe) {

        subscriptionList = new ArrayList<>(subscribe.getSubscriptions());
        userProperties = subscribe.getUserProperties();
        subscriptionIdentifier = subscribe.getSubscriptionIdentifier().orElse(Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER);
        packetIdentifier = subscribe.getPacketId();
    }


    @NotNull
    @Override
    @Immutable
    public List<Subscription> getSubscriptions() {
        return subscriptionList;
    }

    @Override
    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }

    @NotNull
    @Override
    public Optional<Integer> getSubscriptionIdentifier() {
        if (subscriptionIdentifier == Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER) {
            return Optional.empty();
        } else {
            return Optional.of(subscriptionIdentifier);
        }
    }

    @Override
    public int getPacketId() {
        return packetIdentifier;
    }
}
