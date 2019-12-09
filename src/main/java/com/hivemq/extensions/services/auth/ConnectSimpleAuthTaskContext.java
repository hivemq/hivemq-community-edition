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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ReasonCodeUtil;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * @author Georg Held
 * @author Florian Limpöck
 */
public class ConnectSimpleAuthTaskContext extends PluginInOutTaskContext<ConnectSimpleAuthTaskOutput>
        implements Supplier<ConnectSimpleAuthTaskOutput> {

    private static final Logger log = LoggerFactory.getLogger(ConnectSimpleAuthTaskContext.class);

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
    private final AuthenticationContext authenticationContext;
    @NotNull
    private ConnectSimpleAuthTaskOutput connectSimpleAuthTaskOutput;

    public ConnectSimpleAuthTaskContext(
            @NotNull final String identifier,
            @NotNull final ConnectHandler connectHandler,
            @NotNull final MqttConnacker mqttConnacker,
            @NotNull final ChannelHandlerContext ctx,
            @NotNull final CONNECT connect,
            @NotNull final PluginOutPutAsyncer asyncer,
            final int authenticatorsCount,
            final boolean validateUTF8,
            @NotNull final ModifiableClientSettingsImpl clientSettings,
            @NotNull final ModifiableDefaultPermissions permissions,
            @NotNull final AuthenticationContext authenticationContext) {

        super(ConnectSimpleAuthTask.class, identifier);
        this.connectHandler = connectHandler;
        this.mqttConnacker = mqttConnacker;
        this.ctx = ctx;
        this.connect = connect;
        this.authenticatorsCount = authenticatorsCount;
        this.authenticationContext = authenticationContext;
        this.connectSimpleAuthTaskOutput = new ConnectSimpleAuthTaskOutput(asyncer, clientSettings, permissions, authenticationContext, validateUTF8);
    }

    @Override
    public void pluginPost(@NotNull final ConnectSimpleAuthTaskOutput pluginOutput) {

        AuthContextUtil.checkTimeout(pluginOutput);
        AuthContextUtil.checkUndecided(pluginOutput);

        if (this.authenticationContext.getIndex().incrementAndGet() != authenticatorsCount) {
            //CONTINUE
            this.connectSimpleAuthTaskOutput = new ConnectSimpleAuthTaskOutput(pluginOutput);
            return;
        }

        //DONE
        final InternalUserProperties changedUserProperties = pluginOutput.getChangedUserProperties();
        if (changedUserProperties != null) {
            ctx.channel()
                    .attr(ChannelAttributes.AUTH_USER_PROPERTIES)
                    .set(changedUserProperties.consolidate().toMqtt5UserProperties());
        }

        switch (pluginOutput.getAuthenticationState()) {

            case SUCCESS:
                succeed(pluginOutput);
                break;
            case FAILED:
            case CONTINUE:
            case NEXT_EXTENSION_OR_DEFAULT:
                fail(pluginOutput);
                break;
            case UNDECIDED:
                //may happen if all providers return null.
                if (!connectSimpleAuthTaskOutput.isAuthenticatorPresent()) {
                    ctx.executor().execute(() -> connectHandler.connectSuccessfulUnauthenticated(ctx, connect, pluginOutput.getClientSettings()));
                }
        }

    }

    private void fail(@NotNull final ConnectSimpleAuthTaskOutput pluginOutput) {
        final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.valueOf(Objects.requireNonNull(pluginOutput.getConnackReasonCode()).name()), pluginOutput.getReasonString(), pluginOutput.getChangedUserProperties());
        mqttConnacker.connackError(ctx.channel(), "Client with ip {} could not be authenticated",
                "Failed Authentication", ReasonCodeUtil.toMqtt5(pluginOutput.getConnackReasonCode()),
                ReasonCodeUtil.toMqtt3(pluginOutput.getConnackReasonCode()), pluginOutput.getReasonString(), event);
    }

    private void succeed(@NotNull final ConnectSimpleAuthTaskOutput pluginOutput) {
        try {
            ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).set(connectSimpleAuthTaskOutput.getDefaultPermissions());
            ctx.executor().execute(() -> connectHandler.connectSuccessfulAuthenticated(ctx, connect, pluginOutput.getClientSettings()));
        } catch (final RejectedExecutionException ex) {
            if (!ctx.executor().isShutdown()) {
                log.error("Execution of successful authenticated connect was rejected for client with IP {}.",
                        ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), ex);
            }
        }
    }

    public void increment() {
        authenticationContext.getIndex().incrementAndGet();
    }

    @NotNull
    @Override
    public ConnectSimpleAuthTaskOutput get() {
        return this.connectSimpleAuthTaskOutput;
    }
}
