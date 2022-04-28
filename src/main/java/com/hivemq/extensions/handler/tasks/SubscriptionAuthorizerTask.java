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
package com.hivemq.extensions.handler.tasks;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerInputImpl;
import com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Christoph Schäbel
 */
public class SubscriptionAuthorizerTask
        implements PluginInOutTask<SubscriptionAuthorizerInputImpl, SubscriptionAuthorizerOutputImpl> {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAuthorizerTask.class);

    private final @NotNull AuthorizerProvider authorizerProvider;
    private final @NotNull AuthorizerProviderInput authorizerProviderInput;
    private final @NotNull String pluginId;
    private final @NotNull ClientAuthorizers clientAuthorizers;

    public SubscriptionAuthorizerTask(
            final @NotNull AuthorizerProvider authorizerProvider,
            final @NotNull String pluginId,
            final @NotNull AuthorizerProviderInput input,
            final @NotNull ClientAuthorizers clientAuthorizers) {
        this.authorizerProvider = authorizerProvider;
        this.pluginId = pluginId;
        this.authorizerProviderInput = input;
        this.clientAuthorizers = clientAuthorizers;
    }

    @Override
    public @NotNull SubscriptionAuthorizerOutputImpl apply(
            final @NotNull SubscriptionAuthorizerInputImpl input,
            final @NotNull SubscriptionAuthorizerOutputImpl output) {

        if (output.isCompleted()) {
            return output;
        }

        final SubscriptionAuthorizer authorizer = updateAndGetAuthorizer();
        if (authorizer == null) {
            return output;
        }

        try {
            output.authorizerPresent();
            authorizer.authorizeSubscribe(input, output);
        } catch (final Throwable e) {
            log.warn(
                    "Uncaught exception was thrown from extension with id \"{}\" at subscription authorization. Extensions are responsible on their own to handle exceptions.",
                    pluginId, e);
            Exceptions.rethrowError(e);
        }

        return output;
    }

    private @Nullable SubscriptionAuthorizer updateAndGetAuthorizer() {

        SubscriptionAuthorizer authorizer = null;
        for (final Map.Entry<String, SubscriptionAuthorizer> authorizerEntry : clientAuthorizers.getSubscriptionAuthorizersMap()
                .entrySet()) {
            final String pluginId = authorizerEntry.getKey();
            final SubscriptionAuthorizer subscriptionAuthorizer = authorizerEntry.getValue();
            if (subscriptionAuthorizer.getClass()
                    .getClassLoader()
                    .equals(authorizerProvider.getClass().getClassLoader()) && pluginId.equals(this.pluginId)) {
                authorizer = subscriptionAuthorizer;
            }
        }
        if (authorizer == null) {
            try {
                final Authorizer authorizerProvided = authorizerProvider.getAuthorizer(authorizerProviderInput);
                if (authorizerProvided instanceof SubscriptionAuthorizer) {
                    authorizer = (SubscriptionAuthorizer) authorizerProvided;
                    clientAuthorizers.put(pluginId, authorizer);
                }
            } catch (final Throwable t) {
                log.warn("Uncaught exception was thrown from extension with id \"{}\" in authorizer provider. " +
                        "Extensions are responsible on their own to handle exceptions.", pluginId, t);
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
