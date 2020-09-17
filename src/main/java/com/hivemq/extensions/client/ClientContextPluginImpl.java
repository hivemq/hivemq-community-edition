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
package com.hivemq.extensions.client;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
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
import com.hivemq.extension.sdk.api.interceptor.suback.SubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.executor.task.AbstractOutput;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientContextPluginImpl extends AbstractOutput implements ClientContext {

    private final @NotNull ClassLoader pluginClassloader;
    private final @NotNull ClientContextImpl clientContext;

    public ClientContextPluginImpl(
            final @NotNull ClassLoader pluginClassloader,
            final @NotNull ClientContextImpl clientContext) {

        this.pluginClassloader = pluginClassloader;
        this.clientContext = clientContext;
    }

    @Override
    public void addPublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        clientContext.addPublishInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        clientContext.addPublishOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubackInboundInterceptor(final @NotNull PubackInboundInterceptor interceptor) {
        clientContext.addPubackInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubackOutboundInterceptor(final @NotNull PubackOutboundInterceptor interceptor) {
        clientContext.addPubackOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrecInboundInterceptor(final @NotNull PubrecInboundInterceptor interceptor) {
        clientContext.addPubrecInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrecOutboundInterceptor(final @NotNull PubrecOutboundInterceptor interceptor) {
        clientContext.addPubrecOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrelInboundInterceptor(final @NotNull PubrelInboundInterceptor interceptor) {
        clientContext.addPubrelInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubrelOutboundInterceptor(final @NotNull PubrelOutboundInterceptor interceptor) {
        clientContext.addPubrelOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubcompInboundInterceptor(final @NotNull PubcompInboundInterceptor interceptor) {
        clientContext.addPubcompInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPubcompOutboundInterceptor(final @NotNull PubcompOutboundInterceptor interceptor) {
        clientContext.addPubcompOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        clientContext.addSubscribeInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addSubackOutboundInterceptor(final @NotNull SubackOutboundInterceptor interceptor) {
        clientContext.addSubackOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addUnsubscribeInboundInterceptor(final @NotNull UnsubscribeInboundInterceptor interceptor) {
        clientContext.addUnsubscribeInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addUnsubackOutboundInterceptor(final @NotNull UnsubackOutboundInterceptor interceptor) {
        clientContext.addUnsubackOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addDisconnectInboundInterceptor(final @NotNull DisconnectInboundInterceptor interceptor) {
        clientContext.addDisconnectInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addDisconnectOutboundInterceptor(final @NotNull DisconnectOutboundInterceptor interceptor) {
        clientContext.addDisconnectOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPingReqInboundInterceptor(final @NotNull PingReqInboundInterceptor interceptor) {
        clientContext.addPingReqInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void addPingRespOutboundInterceptor(final @NotNull PingRespOutboundInterceptor interceptor) {
        clientContext.addPingRespOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        clientContext.removePublishInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        clientContext.removePublishOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubackInboundInterceptor(final @NotNull PubackInboundInterceptor interceptor) {
        clientContext.removePubackInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubackOutboundInterceptor(final @NotNull PubackOutboundInterceptor interceptor) {
        clientContext.removePubackOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrecInboundInterceptor(final @NotNull PubrecInboundInterceptor interceptor) {
        clientContext.removePubrecInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrecOutboundInterceptor(final @NotNull PubrecOutboundInterceptor interceptor) {
        clientContext.removePubrecOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrelInboundInterceptor(final @NotNull PubrelInboundInterceptor interceptor) {
        clientContext.removePubrelInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubrelOutboundInterceptor(final @NotNull PubrelOutboundInterceptor interceptor) {
        clientContext.removePubrelOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubcompInboundInterceptor(final @NotNull PubcompInboundInterceptor interceptor) {
        clientContext.removePubcompInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePubcompOutboundInterceptor(final @NotNull PubcompOutboundInterceptor interceptor) {
        clientContext.removePubcompOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        clientContext.removeSubscribeInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeSubackOutboundInterceptor(final @NotNull SubackOutboundInterceptor interceptor) {
        clientContext.removeSubackOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeUnsubscribeInboundInterceptor(final @NotNull UnsubscribeInboundInterceptor interceptor) {
        clientContext.removeUnsubscribeInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeUnsubackOutboundInterceptor(final @NotNull UnsubackOutboundInterceptor interceptor) {
        clientContext.removeUnsubackOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeDisconnectInboundInterceptor(final @NotNull DisconnectInboundInterceptor interceptor) {
        clientContext.removeDisconnectInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removeDisconnectOutboundInterceptor(final @NotNull DisconnectOutboundInterceptor interceptor) {
        clientContext.removeDisconnectOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePingReqInboundInterceptor(final @NotNull PingReqInboundInterceptor interceptor) {
        clientContext.removePingReqInboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public void removePingRespOutboundInterceptor(final @NotNull PingRespOutboundInterceptor interceptor) {
        clientContext.removePingRespOutboundInterceptor(checkNotNull(interceptor, "The interceptor must never be null"));
    }

    @Override
    public @Immutable @NotNull List<@NotNull Interceptor> getAllInterceptors() {
        return clientContext.getAllInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PublishInboundInterceptor> getPublishInboundInterceptors() {
        return clientContext.getPublishInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PublishOutboundInterceptor> getPublishOutboundInterceptors() {
        return clientContext.getPublishOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubackInboundInterceptor> getPubackInboundInterceptors() {
        return clientContext.getPubackInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubackOutboundInterceptor> getPubackOutboundInterceptors() {
        return clientContext.getPubackOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubrecInboundInterceptor> getPubrecInboundInterceptors() {
        return clientContext.getPubrecInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubrecOutboundInterceptor> getPubrecOutboundInterceptors() {
        return clientContext.getPubrecOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubrelInboundInterceptor> getPubrelInboundInterceptors() {
        return clientContext.getPubrelInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubrelOutboundInterceptor> getPubrelOutboundInterceptors() {
        return clientContext.getPubrelOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubcompInboundInterceptor> getPubcompInboundInterceptors() {
        return clientContext.getPubcompInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PubcompOutboundInterceptor> getPubcompOutboundInterceptors() {
        return clientContext.getPubcompOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull SubscribeInboundInterceptor> getSubscribeInboundInterceptors() {
        return clientContext.getSubscribeInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull SubackOutboundInterceptor> getSubackOutboundInterceptors() {
        return clientContext.getSubackOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull UnsubscribeInboundInterceptor> getUnsubscribeInboundInterceptors() {
        return clientContext.getUnsubscribeInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull UnsubackOutboundInterceptor> getUnsubackOutboundInterceptors() {
        return clientContext.getUnsubackOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull DisconnectInboundInterceptor> getDisconnectInboundInterceptors() {
        return clientContext.getDisconnectInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull DisconnectOutboundInterceptor> getDisconnectOutboundInterceptors() {
        return clientContext.getDisconnectOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PingReqInboundInterceptor> getPingReqInboundInterceptors() {
        return clientContext.getPingReqInboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @Immutable @NotNull List<@NotNull PingRespOutboundInterceptor> getPingRespOutboundInterceptors() {
        return clientContext.getPingRespOutboundInterceptorsOfExtension(pluginClassloader);
    }

    @Override
    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return clientContext.getDefaultPermissions();
    }
}
