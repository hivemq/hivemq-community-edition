/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.mqtt.handler.disconnect;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class Mqtt5ServerDisconnector implements MqttServerDisconnector {

    private final boolean disconnectWithReasonCode;
    private final boolean disconnectWithReasonString;

    private final @NotNull MqttDisconnectUtil disconnectUtil;

    @Inject
    public Mqtt5ServerDisconnector(final @NotNull MqttDisconnectUtil disconnectUtil) {
        this.disconnectWithReasonCode = InternalConfigurations.DISCONNECT_WITH_REASON_CODE;
        this.disconnectWithReasonString = InternalConfigurations.DISCONNECT_WITH_REASON_STRING;
        this.disconnectUtil = disconnectUtil;
    }

    @Override
    public void disconnect(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @Nullable Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        if (channel.isActive()) {
            disconnectUtil.logDisconnect(channel, logMessage, eventLogMessage);
            disconnectUtil.disconnect(channel, disconnectWithReasonCode, disconnectWithReasonString, reasonCode, reasonString, userProperties, isAuthentication);
        }
    }
}
