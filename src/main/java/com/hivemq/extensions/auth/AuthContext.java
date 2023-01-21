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
package com.hivemq.extensions.auth;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

abstract class AuthContext<T extends AuthOutput<?>> extends PluginInOutTaskContext<T> implements Supplier<T> {

    private static final Logger log = LoggerFactory.getLogger(AuthContext.class);

    final @NotNull ChannelHandlerContext ctx;
    final @NotNull MqttAuthSender authSender;
    private final int authenticatorsCount;
    private int counter;
    private @NotNull AuthenticationState state = AuthenticationState.UNDECIDED;
    @NotNull T output;

    AuthContext(
            final @NotNull String identifier,
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull MqttAuthSender authSender,
            final int authenticatorsCount,
            final @NotNull T output) {

        super(identifier);
        this.ctx = ctx;
        this.authSender = authSender;
        this.authenticatorsCount = authenticatorsCount;
        this.output = output;
    }

    @Override
    public void pluginPost(final @NotNull T output) {
        if (output.isTimedOut()) {
            switch (output.getTimeoutFallback()) {
                case FAILURE:
                    output.failByTimeout();
                    break;
                case SUCCESS:
                    output.nextByTimeout();
                    break;
            }
        } else if ((output.getAuthenticationState() == AuthenticationState.UNDECIDED) &&
                output.isAuthenticatorPresent()) {
            output.failByUndecided();
        }

        if (!state.isFinal() && (output.getAuthenticationState() != AuthenticationState.UNDECIDED)) {
            state = output.getAuthenticationState();
        }

        if (++counter < authenticatorsCount) {
            if (!state.isFinal()) {
                this.output = createNextOutput(output);
            }
        } else {
            finishExtensionFlow(output);
        }
    }

    abstract @NotNull T createNextOutput(@NotNull T prevOutput);

    @Override
    public @NotNull T get() {
        return output;
    }

    private void finishExtensionFlow(final @NotNull T output) {
        if (!ctx.channel().isActive()) {
            return;
        }

        try {
            ctx.executor().execute(() -> {
                switch (state) {
                    case CONTINUE:
                        continueAuthentication(output);
                        break;
                    case SUCCESS:
                        succeedAuthentication(output);
                        break;
                    case FAILED:
                    case NEXT_EXTENSION_OR_DEFAULT:
                        failAuthentication(output);
                        break;
                    case UNDECIDED:
                        assert !output.isAuthenticatorPresent(); // happens only if all providers return null
                        undecidedAuthentication(output);
                }
            });
        } catch (final RejectedExecutionException ex) {
            if (!ctx.executor().isShutdown()) {
                final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
                log.error("Execution of authentication was rejected for client with IP {}.",
                        clientConnection.getChannelIP().orElse("UNKNOWN"), ex);
            }
        }
    }

    private void continueAuthentication(final @NotNull T output) {
        final ChannelFuture authFuture = authSender.sendAuth(
                ctx.channel(),
                output.getAuthenticationData(),
                Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION,
                Mqtt5UserProperties.of(output.getOutboundUserProperties().asInternalList()),
                output.getReasonString());

        authFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                final ScheduledFuture<?> timeoutFuture =
                        ctx.executor().schedule(this::onTimeout, output.getTimeout(), TimeUnit.SECONDS);
                ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthFuture(timeoutFuture);
            } else if (future.channel().isActive()) {
                onSendException(future.cause());
            }
        });
    }

    void succeedAuthentication(final @NotNull T output) {
        ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(output.getDefaultPermissions());
    }

    abstract void failAuthentication(@NotNull T output);

    abstract void undecidedAuthentication(@NotNull T output);

    abstract void onTimeout();

    abstract void onSendException(@NotNull Throwable cause);
}
