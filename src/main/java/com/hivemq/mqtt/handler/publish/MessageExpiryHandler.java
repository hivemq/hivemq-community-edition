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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT;

/**
 * @author Florian Limpöck
 * @author Lukas Brandl
 * @since 4.0.2
 */
@Singleton
@ChannelHandler.Sharable
public class MessageExpiryHandler extends ChannelOutboundHandlerAdapter {

    static final Logger log = LoggerFactory.getLogger(MessageExpiryHandler.class);

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise)
            throws Exception {

        if (msg instanceof PUBLISH) {
            final PUBLISH publish = (PUBLISH) msg;
            checkAndSetPublishExpiry(publish);
            final boolean expireInflight = InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES;
            final boolean isInflight = (publish.getQoS() == QoS.EXACTLY_ONCE) && publish.isDuplicateDelivery();
            final boolean drop = (publish.getMessageExpiryInterval() == 0) && (!isInflight || expireInflight);
            if (drop) {
                ctx.fireUserEventTriggered(new PublishDroppedEvent(publish));
                return;
            }
        } else if (msg instanceof PUBREL) {
            final PUBREL pubrel = (PUBREL) msg;
            checkAndSetPubrelExpiry(pubrel);
            final boolean expireInflight = InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS;
            final boolean drop = (pubrel.getExpiryInterval() != null) && (pubrel.getExpiryInterval() == 0) && expireInflight;
            if (drop) {
                ctx.fireUserEventTriggered(new PubrelDroppedEvent(pubrel));
                return;
            }
        }

        super.write(ctx, msg, promise);
    }

    private void checkAndSetPublishExpiry(final @NotNull PUBLISH message) {
        if (message.getMessageExpiryInterval() != MAX_EXPIRY_INTERVAL_DEFAULT) {
            final long waitingInSeconds = (System.currentTimeMillis() - message.getTimestamp()) / 1000;
            final long remainingInterval = Math.max(0, message.getMessageExpiryInterval() - waitingInSeconds);
            message.setMessageExpiryInterval(remainingInterval);
        }
    }

    private void checkAndSetPubrelExpiry(final @NotNull PUBREL message) {
        if (message.getExpiryInterval() == null || message.getPublishTimestamp() == null) {
            return;
        }
        if (message.getExpiryInterval() != MAX_EXPIRY_INTERVAL_DEFAULT) {
            final long waitingInSeconds = (System.currentTimeMillis() - message.getPublishTimestamp()) / 1000;
            final long remainingInterval = Math.max(0, message.getExpiryInterval() - waitingInSeconds);
            message.setExpiryInterval(remainingInterval);
        }
    }

}
