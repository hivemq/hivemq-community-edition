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

package com.hivemq.mqtt.handler.publish.qos;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_RETRY_INTERVAL;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class QoSSenderHandler extends ChannelDuplexHandler {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(QoSSenderHandler.class);

    private final @NotNull PublishPollService publishPollService;

    private final int publishRetryInterval;

    @NotNull
    private final Map<Integer, ScheduledFuture> timeouts = new ConcurrentHashMap<>();

    @Inject
    QoSSenderHandler(@NotNull final PublishPollService publishPollService) {
        this.publishPollService = publishPollService;
        this.publishRetryInterval = PUBLISH_RETRY_INTERVAL.get();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, @NotNull final Object msg) throws Exception {

        final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        if (msg instanceof CONNECT) {

            //devour message

        } else if (msg instanceof PUBACK) {

            //QoS1
            handlePuback((PUBACK) msg, client);

        } else if (msg instanceof PUBREC) {

            //QoS2
            handlePubrec(ctx, (PUBREC) msg, client);

        } else if (msg instanceof PUBCOMP) {
            //QoS2
            handlePubcomp((PUBCOMP) msg, client);

        } else {
            super.channelRead(ctx, msg);

        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise) {

        final String client = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        if (msg instanceof PublishWithFuture) {
            final PublishWithFuture publish = (PublishWithFuture) msg;
            if (log.isTraceEnabled()) {
                log.trace("Client {}: Sending PUBLISH QoS {} Message", client, publish.getQoS().getQosNumber());
            }

            if (publish.getQoS().getQosNumber() > 0) {
                promise.addListener(resendPublishListener(ctx, client, publish));
            }
        } else if (msg instanceof PUBLISH) {
            // Scheduled resend
            promise.addListener(resendPublishListener(ctx, client, (PUBLISH) msg));
        } else if (msg instanceof PUBREL) {
            if (log.isTraceEnabled()) {
                log.trace("Client {}: Sending PUBREL Message", client);
            }
            promise.addListener(resendPubrelListener(ctx, client, (PUBREL) msg));
        }

        try {
            ctx.writeAndFlush(msg, promise);
        } catch (final Exception e) {
            log.error("Exception in channel write. {}", e);
        }
    }

    private void handlePubcomp(@NotNull final PUBCOMP msg, @NotNull final String client) {
        log.trace("Client {}: Received PUBCOMP", client);
        cancelScheduledFuture(msg);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBCOMP remove message id:[{}]", client, msg.getPacketIdentifier());
        }
    }

    private void handlePuback(final PUBACK msg, @NotNull final String client) {
        log.trace("Client {}: Received PUBACK", client);
        final int messageId = msg.getPacketIdentifier();
        cancelScheduledFuture(msg);

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBACK remove message id:[{}] ", client, messageId);
        }
    }

    private void handlePubrec(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBREC msg, @NotNull final String client) {
        log.trace("Client {}: Received pubrec", client);

        cancelScheduledFuture(msg);

        final ListenableFuture<Void> future = publishPollService.putPubrelInQueue(client, msg.getPacketIdentifier());
        FutureUtils.addExceptionLogger(future);
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBREC remove message id:[{}]", client, msg.getPacketIdentifier());
        }
        //We send it with channel instead of context because otherwise we can't intercept the write in this handler
        ctx.channel().writeAndFlush(new PUBREL(msg.getPacketIdentifier()));
    }

    private void resendPublish(@NotNull final ChannelHandlerContext ctx, @NotNull final String client, @NotNull final PUBLISH publish) {
        if (publishRetryInterval > 0) {
            final ScheduledFuture<?> schedule = ctx.channel().eventLoop().schedule(republish(ctx.channel(), publish), publishRetryInterval, TimeUnit.SECONDS);
            timeouts.put(publish.getPacketIdentifier(), schedule);
        }

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Sent PUBLISH QoS {} add message id [{}]", client, publish.getQoS().getQosNumber(), publish.getPacketIdentifier());
        }
    }

    private void resendPubrel(@NotNull final ChannelHandlerContext ctx, @NotNull final String client, @NotNull final PUBREL pubrel) {
        if (publishRetryInterval > 0) {
            final ScheduledFuture<?> schedule = ctx.channel().eventLoop().schedule(republish(ctx.channel(), pubrel), publishRetryInterval, TimeUnit.SECONDS);
            timeouts.put(pubrel.getPacketIdentifier(), schedule);
        }

        if (log.isTraceEnabled()) {
            log.trace("Client {}: Sent PUBREL add message id [{}]", client, pubrel.getPacketIdentifier());
        }
    }

    private ChannelFutureListener resendPublishListener(@NotNull final ChannelHandlerContext ctx, @NotNull final String client, @NotNull final PUBLISH publish) {
        return future -> resendPublish(ctx, client, publish);
    }

    private ChannelFutureListener resendPubrelListener(@NotNull final ChannelHandlerContext ctx, @NotNull final String client, @NotNull final PUBREL pubrel) {
        return future -> resendPubrel(ctx, client, pubrel);
    }

    private Runnable republish(@NotNull final Channel channel, @NotNull final MessageWithID message) {

        return () -> {
            try {
                if (channel.isOpen()) {

                    if (message instanceof PUBLISH) {
                        final PUBLISHFactory.Mqtt5Builder publishBuilder = new PUBLISHFactory.Mqtt5Builder().fromPublish((PUBLISH) message);
                        publishBuilder.withDuplicateDelivery(true);
                        channel.writeAndFlush(publishBuilder.build());
                    } else {
                        channel.writeAndFlush(message);
                    }
                }
            } catch (final Exception e) {
                log.error("Exception in republish task", e);
            }
        };
    }

    private void cancelScheduledFuture(final MessageWithID message) {
        cancelScheduledFuture(message.getPacketIdentifier());
    }

    private void cancelScheduledFuture(final int messageId) {
        final ScheduledFuture remove = timeouts.remove(messageId);
        if (remove != null) {
            remove.cancel(false);
        }
    }

}