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
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
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
    public void addPubrelOutboundInterceptor(final @NotNull PubrelOutboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrelInboundInterceptor(final @NotNull PubrelInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubcompOutboundInterceptor(final @NotNull PubcompOutboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubcompInboundInterceptor(final @NotNull PubcompInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addDisconnectInboundInterceptor(
            final @NotNull DisconnectInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addDisconnectOutboundInterceptor(
            final @NotNull DisconnectOutboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubackOutboundInterceptor(final @NotNull PubackOutboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubackInboundInterceptor(final @NotNull PubackInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrecInboundInterceptor(final @NotNull PubrecInboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrecOutboundInterceptor(final @NotNull PubrecOutboundInterceptor interceptor) {
        clientContext.addInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrelOutboundInterceptor(final @NotNull PubrelOutboundInterceptor pubackOutboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubackOutboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrelInboundInterceptor(final @NotNull PubrelInboundInterceptor pubrelInboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubrelInboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubcompOutboundInterceptor(final @NotNull PubcompOutboundInterceptor pubcompOutboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubcompOutboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubcompInboundInterceptor(final @NotNull PubcompInboundInterceptor pubcompInboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubcompInboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeDisconnectInboundInterceptor(
            final @NotNull DisconnectInboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeDisconnectOutboundInterceptor(
            final @NotNull DisconnectOutboundInterceptor interceptor) {
        clientContext.removeInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubackOutboundInterceptor(final @NotNull PubackOutboundInterceptor pubackOutboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubackOutboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubackInboundInterceptor(final @NotNull PubackInboundInterceptor pubackInboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubackInboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrecOutboundInterceptor(final @NotNull PubrecOutboundInterceptor pubrecOutboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubrecOutboundInterceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrecInboundInterceptor(final @NotNull PubrecInboundInterceptor pubrecInboundInterceptor) {
        clientContext.removeInterceptor(checkNotNull(pubrecInboundInterceptor, "The interceptor must never be null"));
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
    public List<@NotNull PubrelOutboundInterceptor> getPubrelOutboundInterceptors() {
        return clientContext.getPubrelOutboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<@NotNull PubrelInboundInterceptor> getPubrelInboundInterceptors() {
        return clientContext.getPubrelInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<SubscribeInboundInterceptor> getSubscribeInboundInterceptors() {
        return clientContext.getSubscribeInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<DisconnectOutboundInterceptor> getDisconnectOutboundInterceptors() {
        return clientContext.getDisconnectOutboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<DisconnectInboundInterceptor> getDisconnectInboundInterceptors() {
        return clientContext.getDisconnectInboundInterceptorsForPlugin(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubackOutboundInterceptor> getPubackOutboundInterceptors() {
        return clientContext.getPubackOutboundInterceptorsForPlugin(pluginClassloader);
    }

    @Override
    public @Immutable
    @NotNull List<@NotNull PubackInboundInterceptor> getPubackInboundInterceptors() {
        return clientContext.getPubackInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<PubrecOutboundInterceptor> getPubrecOutboundInterceptors() {
        return clientContext.getPubrecOutboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<PubrecInboundInterceptor> getPubrecInboundInterceptors() {
        return clientContext.getPubrecInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<@NotNull PubcompOutboundInterceptor> getPubcompOutboundInterceptors() {
        return clientContext.getPubcompOutboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    @Immutable
    public List<@NotNull PubcompInboundInterceptor> getPubcompInboundInterceptors() {
        return clientContext.getPubcompInboundInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    public ModifiableDefaultPermissions getDefaultPermissions() {
        return clientContext.getDefaultPermissions();
    }
}
