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

package com.hivemq.extensions.handler.tasks;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.auth.parameter.PublishAuthorizerInputImpl;
import com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Christoph Schäbel
 */
public class PublishAuthorizerTask implements PluginInOutTask<PublishAuthorizerInputImpl, PublishAuthorizerOutputImpl> {

    private static final Logger log = LoggerFactory.getLogger(PublishAuthorizerTask.class);

    private final @NotNull AuthorizerProvider authorizerProvider;
    private final @NotNull AuthorizerProviderInput authorizerProviderInput;
    private final @NotNull String pluginId;
    private final @NotNull ClientAuthorizers clientAuthorizers;
    private final @NotNull ChannelHandlerContext channelHandlerContext;

    public PublishAuthorizerTask(final @NotNull AuthorizerProvider authorizerProvider,
                                 final @NotNull String pluginId,
                                 final @NotNull AuthorizerProviderInput input,
                                 final @NotNull ClientAuthorizers clientAuthorizers,
                                 final @NotNull ChannelHandlerContext channelHandlerContext) {
        this.authorizerProvider = authorizerProvider;
        this.pluginId = pluginId;
        this.authorizerProviderInput = input;
        this.clientAuthorizers = clientAuthorizers;
        this.channelHandlerContext = channelHandlerContext;
    }

    @Override
    public @NotNull PublishAuthorizerOutputImpl apply(final @NotNull PublishAuthorizerInputImpl input, final @NotNull PublishAuthorizerOutputImpl output) {

        if (output.isCompleted()) {
            return output;
        }

        final PublishAuthorizer authorizer = updateAndGetAuthorizer();
        if (authorizer == null) {
            return output;
        }

        output.authorizerPresent();
        if (channelHandlerContext.channel().attr(ChannelAttributes.INCOMING_PUBLISHES_SKIP_REST).get() != null) {
            //client already disconnected by authorizer, no more processing of any messages allowed.
            output.forceFailedAuthorization();
        } else {
            try {
                authorizer.authorizePublish(input, output);
            } catch (final Throwable e) {
                log.warn("Uncaught exception was thrown from extension with id \"{}\" at subscription authorization. Extensions are responsible on their own to handle exceptions.",
                        pluginId);
                log.debug("Original exception:", e);
                Exceptions.rethrowError(e);
            }
        }

        return output;
    }

    private @Nullable PublishAuthorizer updateAndGetAuthorizer() {

        PublishAuthorizer authorizer = null;
        for (final Map.Entry<String, PublishAuthorizer> authorizerEntry : clientAuthorizers.getPublishAuthorizersMap().entrySet()) {
            final String pluginId = authorizerEntry.getKey();
            final PublishAuthorizer publishAuthorizer = authorizerEntry.getValue();
            if (publishAuthorizer.getClass().getClassLoader().equals(authorizerProvider.getClass().getClassLoader()) && pluginId.equals(this.pluginId)) {
                authorizer = publishAuthorizer;
            }
        }
        if (authorizer == null) {
            try {
                final Authorizer authorizerProvided = authorizerProvider.getAuthorizer(authorizerProviderInput);
                if (authorizerProvided instanceof PublishAuthorizer) {
                    authorizer = (PublishAuthorizer) authorizerProvided;
                    clientAuthorizers.put(pluginId, authorizer);
                }
            } catch (final Throwable t) {
                log.warn("Uncaught exception was thrown from extension with id \"{}\" in authorizer provider. " +
                        "Extensions are responsible on their own to handle exceptions.", pluginId);
                log.debug("Original exception:", t);
                Exceptions.rethrowError(t);
            }
        }
        return authorizer;
    }


    @Override
    public @NotNull ClassLoader getPluginClassLoader() {
        return authorizerProvider.getClass().getClassLoader();
    }
}
