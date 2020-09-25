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
package com.hivemq.mqtt.handler.connect;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.gauges.OpenConnectionsGauge;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A {@link ChannelHandler} which is responsible for limiting the concurrent connections
 * which is defined by the 'max-connections' parameter in the configuration.
 *
 * @author Yannick Weber
 */
@Singleton
@ChannelHandler.Sharable
public class ConnectionLimiterHandler extends ChannelInboundHandlerAdapter {

    private final static Logger log = LoggerFactory.getLogger(ConnectionLimiterHandler.class);

    private final @NotNull MqttConnacker mqttConnacker;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull OpenConnectionsGauge openConnectionsGauge;
    private volatile long maxConnections;
    private volatile long warnThreshold;

    @Inject
    public ConnectionLimiterHandler(
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull OpenConnectionsGauge openConnectionsGauge) {
        this.mqttConnacker = mqttConnacker;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.openConnectionsGauge = openConnectionsGauge;
    }

    @Override
    public void channelActive(final @NotNull ChannelHandlerContext ctx) throws Exception {

        final long configuredCount = restrictionsConfigurationService.maxConnections();

        if (configuredCount > RestrictionsConfigurationService.UNLIMITED_CONNECTIONS) {
            // If we use the max connections configured in the config file, we set the Threshold to 90% of the maximum allowed connections.
            this.warnThreshold = 90 * configuredCount / 100;
            this.maxConnections = configuredCount;
        } else {
            //This means we are dealing with unlimited connections so we can remove this handler from the pipeline
            ctx.pipeline().remove(this);
        }

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
        if (msg instanceof CONNECT) {

            final CONNECT connect = (CONNECT) msg;

            final long currentCount = openConnectionsGauge.getValue();

            if (currentCount > maxConnections) {
                log.warn("The connection limit ({}) is reached. ClientID ({}) connection denied.", maxConnections, connect.getClientIdentifier());
                mqttConnacker.connackError(ctx.channel(),
                        null, // logged on warn
                        "The configured maximum amount of connections is reached",
                        Mqtt5ConnAckReasonCode.QUOTA_EXCEEDED,
                        null);
                return;
            } else if (warnThreshold > 0 && currentCount >= warnThreshold) {
                log.warn("The amount of connections ({}) is close to its limit ({}).", currentCount, maxConnections);
            }

            // We can remove the handler because it doesn't do anything after this point.
            ctx.pipeline().remove(this);
        }
        super.channelRead(ctx, msg);
    }

    @VisibleForTesting
    long getWarnThreshold() {
        return warnThreshold;
    }

    @VisibleForTesting
    long getMaxConnections() {
        return maxConnections;
    }
}

