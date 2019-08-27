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

package com.hivemq.extensions.client;

import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.task.AbstractOutput;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientContextPluginImpl extends AbstractOutput implements ClientContext {

    @NotNull
    private final IsolatedPluginClassloader pluginClassloader;

    @NotNull
    private final ClientContextImpl clientContext;

    public ClientContextPluginImpl(
            final @NotNull IsolatedPluginClassloader pluginClassloader,
            final @NotNull ClientContextImpl clientContext) {
        this.pluginClassloader = pluginClassloader;
        this.clientContext = clientContext;
    }

    @Override
    public void addPublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        clientContext.addInterceptor(interceptor);
    }

    @Override
    public void addSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addUnsubscribeInboundInterceptor(
            final @NotNull UnsubscribeInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        clientContext.removeInterceptor(interceptor);
    }

    @Override
    public void removeSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeUnsubscribeInboundInterceptor(
            final @NotNull UnsubscribeInboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @NotNull
    @Override
    @Immutable
    public List<Interceptor> getAllInterceptors() {
        return clientContext.getAllInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<PublishInboundInterceptor> getPublishInboundInterceptors() {
        return clientContext.getPublishInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<PublishOutboundInterceptor> getPublishOutboundInterceptors() {
        return clientContext.getPublishOutboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<SubscribeInboundInterceptor> getSubscribeInboundInterceptors() {
        return clientContext.getSubscribeInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    public ModifiableDefaultPermissions getDefaultPermissions() {
        return clientContext.getDefaultPermissions();
    }
}
