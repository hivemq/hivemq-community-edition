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

package com.hivemq.mqtt.handler.publish;

import com.hivemq.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
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
 * @since 4.0.2
 */
@Singleton
@ChannelHandler.Sharable
public class PublishMessageExpiryHandler extends ChannelOutboundHandlerAdapter {

    static final Logger log = LoggerFactory.getLogger(PublishMessageExpiryHandler.class);

    @Override
    public void write(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise) throws Exception {

        final ProtocolVersion version = ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get();

        if (msg instanceof PUBLISH && ProtocolVersion.MQTTv5 == version) {
            checkAndSetPublishExpiry(ctx, (PUBLISH) msg);
        }

        super.write(ctx, msg, promise);
    }


    private void checkAndSetPublishExpiry(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH message) {
        if (message.getMessageExpiryInterval() != MAX_EXPIRY_INTERVAL_DEFAULT) {
            final long waitingInSeconds = (System.currentTimeMillis() - message.getTimestamp()) / 1000;
            final long remainingInterval = Math.max(0, message.getMessageExpiryInterval() - waitingInSeconds);

            if (remainingInterval == 0 && !(message.getQoS() == QoS.EXACTLY_ONCE && message.isDuplicateDelivery())) {
                final String clientIdFromChannel = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
                final String clientId = clientIdFromChannel != null ? clientIdFromChannel : "UNKNOWN";
                log.trace("Publish message with topic '{}' and qos '{}' for client '{}' expired after {} seconds.", message.getTopic(), message.getQoS().getQosNumber(), clientId, waitingInSeconds);
            }
            message.setMessageExpiryInterval(remainingInterval);
        }
    }

}
