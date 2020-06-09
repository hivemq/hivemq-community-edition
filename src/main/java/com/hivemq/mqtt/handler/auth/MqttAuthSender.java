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
package com.hivemq.mqtt.handler.auth;

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.util.Bytes;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import javax.inject.Singleton;
import java.nio.ByteBuffer;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
 */
@Singleton
public class MqttAuthSender {

    private final @NotNull EventLog eventLog;

    @Inject
    public MqttAuthSender(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    public @NotNull ChannelFuture sendAuth(
            final @NotNull Channel channel,
            final @Nullable ByteBuffer authData,
            final @NotNull Mqtt5AuthReasonCode reasonCode,
            final @NotNull Mqtt5UserProperties userProperties,
            final @Nullable String reasonString) {

        final AUTH auth = new AUTH(
                channel.attr(ChannelAttributes.AUTH_METHOD).get(),
                Bytes.fromReadOnlyBuffer(authData),
                reasonCode,
                userProperties,
                reasonString);

        logAuth(channel, reasonCode, false);
        return channel.writeAndFlush(auth);
    }

    public void logAuth(
            final @NotNull Channel channel,
            final @NotNull Mqtt5AuthReasonCode reasonCode,
            final boolean received) {

        eventLog.clientAuthentication(channel, reasonCode, received);
    }
}