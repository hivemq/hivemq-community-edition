/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.auth.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extensions.ExtensionInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.subscribe.SubscriptionImpl;
import com.hivemq.mqtt.message.subscribe.Topic;
import io.netty.channel.Channel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Lukas Brandl
 */
public class SubscriptionAuthorizerInputImpl implements SubscriptionAuthorizerInput, PluginTaskInput, Supplier<SubscriptionAuthorizerInputImpl> {

    @NotNull
    private final UserProperties userProperties;

    @NotNull
    private final Optional<Integer> subscriptionIdentifier;

    @NotNull
    private final Subscription subscription;

    @NotNull
    private final ConnectionInformation connectionInformation;

    @NotNull
    private final ClientInformation clientInformation;

    public SubscriptionAuthorizerInputImpl(final @NotNull UserProperties userProperties, final @NotNull Topic topic, final @NotNull Channel channel, final @NotNull String clientId) {
        Preconditions.checkNotNull(userProperties, "userproperties must never be null");
        Preconditions.checkNotNull(topic, "topic must never be null");
        Preconditions.checkNotNull(channel, "channel must never be null");
        Preconditions.checkNotNull(clientId, "clientId must never be null");
        this.userProperties = userProperties;
        this.subscriptionIdentifier = topic.getSubscriptionIdentifier() != null && topic.getSubscriptionIdentifier() > 0 ?
                Optional.of(topic.getSubscriptionIdentifier()) : Optional.empty();

        this.subscription = new SubscriptionImpl(topic);

        this.clientInformation = ExtensionInformationUtil.getAndSetClientInformation(channel, clientId);
        this.connectionInformation = ExtensionInformationUtil.getAndSetConnectionInformation(channel);
    }

    @Override
    public @NotNull UserProperties getUserProperties() {
        return userProperties;
    }

    @Override
    public @NotNull Optional<Integer> getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    @Override
    public @NotNull Subscription getSubscription() {
        return subscription;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull SubscriptionAuthorizerInputImpl get() {
        return this;
    }
}
