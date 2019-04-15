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

package com.hivemq.extensions.services.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ReasonCodeUtil;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.hivemq.extensions.services.auth.ConnectAuthTaskOutput.AuthenticationState.UNDECIDED;

/**
 * @author Georg Held
 */
public class ConnectAuthTaskContext extends PluginInOutTaskContext<ConnectAuthTaskOutput>
        implements Supplier<ConnectAuthTaskOutput> {

    private static final Logger log = LoggerFactory.getLogger(ConnectAuthTaskContext.class);

    @NotNull
    private final ConnectHandler connectHandler;
    @NotNull
    private final MqttConnacker mqttConnacker;
    @NotNull
    private final ChannelHandlerContext ctx;
    @NotNull
    private final CONNECT connect;

    private final int authenticatorsCount;

    @NotNull
    private final AtomicInteger position;
    @NotNull
    private ConnectAuthTaskOutput connectAuthTaskOutput;

    private final AtomicBoolean authTimedOut = new AtomicBoolean(false);

    public ConnectAuthTaskContext(
            @NotNull final String identifier, @NotNull final ConnectHandler connectHandler,
            @NotNull final MqttConnacker mqttConnacker, @NotNull final ChannelHandlerContext ctx,
            @NotNull final CONNECT connect, @NotNull final PluginOutPutAsyncer asyncer,
            final int authenticatorsCount, final boolean validateUTF8) {

        super(SimpleAuthTask.class, identifier);
        this.connectHandler = connectHandler;
        this.mqttConnacker = mqttConnacker;
        this.ctx = ctx;
        this.connect = connect;
        this.authenticatorsCount = authenticatorsCount;
        this.position = new AtomicInteger(0);
        this.connectAuthTaskOutput = new ConnectAuthTaskOutput(asyncer, validateUTF8);
    }

    @Override
    public void pluginPost(@NotNull final ConnectAuthTaskOutput pluginOutput) {

        if (pluginOutput.isAsync() && pluginOutput.isTimedOut() &&
                pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
            authTimedOut.set(true);
            pluginOutput.failByTimeout();
        }

        if (pluginOutput.getAuthenticationState() == UNDECIDED && connectAuthTaskOutput.isAuthenticatorPresent()) {
            pluginOutput.failAuthentication();
        }

        if (position.incrementAndGet() != authenticatorsCount) {
            this.connectAuthTaskOutput = new ConnectAuthTaskOutput(pluginOutput);
            return;
        }

        final InternalUserProperties changedUserProperties = pluginOutput.getChangedUserProperties();
        if (changedUserProperties != null && !authTimedOut.get()) {
            ctx.channel()
                    .attr(ChannelAttributes.AUTH_USER_PROPERTIES)
                    .set(changedUserProperties.consolidate().toMqtt5UserProperties());
        }

        switch (pluginOutput.getAuthenticationState()) {

            case SUCCESS:
                succeed();
                break;

            case FAILED:
                fail(pluginOutput);
                break;

            case CONTINUE:
                continuing(pluginOutput);
                break;
            case UNDECIDED:
                if (!connectAuthTaskOutput.isAuthenticatorPresent()) {
                    ctx.executor().execute(() -> connectHandler.connectSuccessfulUnauthenticated(ctx, connect));
                } else {
                    //should never happen
                    throw new IllegalStateException("Unsupported authentication state");
                }
        }

    }

    private void continuing(@NotNull final ConnectAuthTaskOutput pluginOutput) {
        if (ctx.channel().hasAttr(ChannelAttributes.AUTH_METHOD)) {
            // on to enhanced auth
            try {
                ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).set(connectAuthTaskOutput.getDefaultPermissions());
                ctx.executor().execute(() -> connectHandler.connectSuccessfulUnauthenticated(ctx, connect));
            } catch (final RejectedExecutionException ex) {
                if (!ctx.executor().isShutdown()) {
                    log.error("Execution of successful unauthenticated connect was rejected for client with IP {}.",
                            ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), ex);
                }
            }
        } else {
            final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.NOT_AUTHORIZED, pluginOutput.getReasonString(), pluginOutput.getChangedUserProperties());
            mqttConnacker.connackError(ctx.channel(), "Client with ip {} could not be authenticated",
                    "Failed Authentication", Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                    Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED, pluginOutput.getReasonString(), event);
        }
    }

    private void fail(@NotNull final ConnectAuthTaskOutput pluginOutput) {
        final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.valueOf(Objects.requireNonNull(pluginOutput.getConnackReasonCode()).name()), pluginOutput.getReasonString(), pluginOutput.getChangedUserProperties());
        mqttConnacker.connackError(ctx.channel(), "Client with ip {} could not be authenticated",
                "Failed Authentication", ReasonCodeUtil.toMqtt5(pluginOutput.getConnackReasonCode()),
                ReasonCodeUtil.toMqtt3(pluginOutput.getConnackReasonCode()), pluginOutput.getReasonString(), event);
    }

    private void succeed() {
        try {
            ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).set(connectAuthTaskOutput.getDefaultPermissions());
            ctx.executor().execute(() -> connectHandler.connectSuccessfulAuthenticated(ctx, connect));
        } catch (final RejectedExecutionException ex) {
            if (!ctx.executor().isShutdown()) {
                log.error("Execution of successful authenticated connect was rejected for client with IP {}.",
                        ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), ex);
            }
        }
    }

    public void increment() {
        position.incrementAndGet();
    }

    @NotNull
    @Override
    public ConnectAuthTaskOutput get() {
        return this.connectAuthTaskOutput;
    }
}
