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

package com.hivemq.mqtt.handler.auth;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
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
import java.util.Objects;

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

    @NotNull
    public ChannelFuture sendAuth(@NotNull final Channel channel,
                           @Nullable final ByteBuffer authData,
                           @NotNull final Mqtt5AuthReasonCode reasonCode,
                           @Nullable final Mqtt5UserProperties userProperties,
                           @Nullable final String reasonString) {

        Preconditions.checkNotNull(channel, "Channel must never be null");
        Preconditions.checkNotNull(reasonCode, "ReasonCode must never be null");

        if(reasonCode == Mqtt5AuthReasonCode.SUCCESS){
            channel.attr(ChannelAttributes.RE_AUTH_ONGOING).set(false);
        }

        final AUTH auth = new AUTH(channel.attr(ChannelAttributes.AUTH_METHOD).get(),
                Bytes.fromReadOnlyBuffer(authData),
                reasonCode,
                Objects.requireNonNullElse(userProperties, Mqtt5UserProperties.NO_USER_PROPERTIES),
                reasonString);

        logAuth(channel, reasonCode, false);
        return channel.writeAndFlush(auth);
    }

    public void logAuth(@NotNull final Channel channel, final @NotNull Mqtt5AuthReasonCode reasonCode, final boolean received) {
        eventLog.clientAuthentication(channel, reasonCode, received);
    }
}