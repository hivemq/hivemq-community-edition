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
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class Mqtt3ServerDisconnector implements MqttServerDisconnector {

    private final @NotNull MqttDisconnectUtil mqttDisconnectUtil;

    @Inject
    public Mqtt3ServerDisconnector(final @NotNull MqttDisconnectUtil mqttDisconnectUtil) {
        this.mqttDisconnectUtil = mqttDisconnectUtil;
    }

    @Override
    public void disconnect(@NotNull final Channel channel,
                           @Nullable final String logMessage,
                           @Nullable final String eventLogMessage,
                           @Nullable final Mqtt5DisconnectReasonCode reasonCode,
                           @Nullable final String reasonString) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        if (channel.isActive()) {
            mqttDisconnectUtil.logDisconnect(channel, logMessage, eventLogMessage);
            mqttDisconnectUtil.disconnect(channel);
        }
    }
}
