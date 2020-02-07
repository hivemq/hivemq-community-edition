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

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extensions.events.OnClientDisconnectEvent;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION;
import static com.hivemq.util.ChannelAttributes.*;

/**
 * @author Florian Limpoeck
 * @author Dominik Obermaier
 */
@Singleton
@ChannelHandler.Sharable
public class DisconnectHandler extends SimpleChannelInboundHandler<DISCONNECT> {

    private static final Logger log = LoggerFactory.getLogger(DisconnectHandler.class);
    private final EventLog eventLog;
    private final MetricsHolder metricsHolder;
    private final TopicAliasLimiter topicAliasLimiter;
    private final boolean logClientReasonString;

    @Inject
    public DisconnectHandler(final EventLog eventLog, final MetricsHolder metricsHolder, final TopicAliasLimiter topicAliasLimiter) {
        this.eventLog = eventLog;
        this.metricsHolder = metricsHolder;
        this.topicAliasLimiter = topicAliasLimiter;
        this.logClientReasonString = InternalConfigurations.LOG_CLIENT_REASON_STRING_ON_DISCONNECT;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final DISCONNECT msg) throws Exception {

        ctx.channel().attr(GRACEFUL_DISCONNECT).set(true);
        final String clientId = ctx.channel().attr(CLIENT_ID).get();

        //no version check necessary, because mqtt 3 disconnect session expiry interval = SESSION_EXPIRY_NOT_SET
        if (msg.getSessionExpiryInterval() != CONNECT.SESSION_EXPIRY_NOT_SET) {
            ctx.channel().attr(CLIENT_SESSION_EXPIRY_INTERVAL).set(msg.getSessionExpiryInterval());
        }

        if (log.isTraceEnabled()) {
            log.trace("The client [{}] sent a disconnect message.", clientId);
        }
        eventLog.clientDisconnected(ctx.channel(), logClientReasonString ? msg.getReasonString() : null);

        if (msg.getReasonCode() != NORMAL_DISCONNECTION) {
            ctx.channel().attr(SEND_WILL).set(true);
        } else {
            ctx.channel().attr(SEND_WILL).set(false);
        }
        if (ctx.channel().attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
            ctx.pipeline().fireUserEventTriggered(new OnClientDisconnectEvent(msg.getReasonCode().toDisconnectedReasonCode(), msg.getReasonString(), msg.getUserProperties().getPluginUserProperties(), true));
        }
        ctx.channel().close();
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        final Channel channel = ctx.channel();
        final String[] topicAliasMapping = channel.attr(TOPIC_ALIAS_MAPPING).get();
        final boolean gracefulDisconnect = channel.attr(GRACEFUL_DISCONNECT).get() != null;
        final boolean preventLwt = channel.attr(PREVENT_LWT).get() != null ? channel.attr(PREVENT_LWT).get() : false;
        final boolean takenOver = channel.attr(TAKEN_OVER).get() != null ? channel.attr(TAKEN_OVER).get() : false;
        final boolean authenticated = channel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).get() != null ? channel.attr(AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).get() : false;
        final boolean logged = channel.attr(ChannelAttributes.DISCONNECT_EVENT_LOGGED).get() != null ? channel.attr(DISCONNECT_EVENT_LOGGED).get() : false;

        if (!gracefulDisconnect && !preventLwt && !takenOver && authenticated) {
            channel.attr(SEND_WILL).set(true);
        }

        if (!logged) {
            eventLog.clientDisconnected(channel, null);
        }

        //increase metrics
        metricsHolder.getClosedConnectionsCounter().inc();
        if (!gracefulDisconnect) {
            if (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                ctx.pipeline().fireUserEventTriggered(new OnClientDisconnectEvent(null, null, null, false));
            }
        }

        if (topicAliasMapping != null) {
            topicAliasLimiter.finishUsage(topicAliasMapping);
        }

        super.channelInactive(ctx);
    }
}
