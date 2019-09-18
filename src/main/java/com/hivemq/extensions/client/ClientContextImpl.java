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

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
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
import com.hivemq.extension.sdk.api.interceptor.suback.SubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientContextImpl {

    private final @NotNull List<Interceptor> interceptorList;
    private final @NotNull ModifiableDefaultPermissions defaultPermissions;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    public ClientContextImpl(
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull ModifiableDefaultPermissions defaultPermissions) {

        this.interceptorList = new CopyOnWriteArrayList<>();
        this.hiveMQExtensions = hiveMQExtensions;
        this.defaultPermissions = defaultPermissions;
    }

    public void addInterceptor(final @NotNull Interceptor interceptor) {
        if (!interceptorList.contains(interceptor)) {
            interceptorList.add(interceptor);
        }
    }

    public void update(final @NotNull ClientContextPluginImpl clientContextPlugin) {
        interceptorList.addAll(clientContextPlugin.getAllInterceptors());
    }

    public void addPublishInboundInterceptor(final @NotNull PublishInboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void addPublishOutboundInterceptor(final @NotNull PublishOutboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void addSubscribeInboundInterceptor(final @NotNull SubscribeInboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void removeInterceptor(final @NotNull Interceptor interceptor) {
        interceptorList.remove(interceptor);
    }

    public @Immutable @NotNull List<@NotNull Interceptor> getAllInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull Interceptor> getAllInterceptors() {
        return interceptorList.stream()
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PublishInboundInterceptor> getPublishInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PublishInboundInterceptor)
                .map(interceptor -> (PublishInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PublishInboundInterceptor> getPublishInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PublishInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PublishInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PublishOutboundInterceptor> getPublishOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PublishOutboundInterceptor)
                .map(interceptor -> (PublishOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PublishOutboundInterceptor> getPublishOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PublishOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PublishOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubackInboundInterceptor> getPubackInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubackInboundInterceptor)
                .map(interceptor -> (PubackInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubackInboundInterceptor> getPubackInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubackInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubackInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubackOutboundInterceptor> getPubackOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubackOutboundInterceptor)
                .map(interceptor -> (PubackOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubackOutboundInterceptor> getPubackOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubackOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubackOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrecInboundInterceptor> getPubrecInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubrecInboundInterceptor)
                .map(interceptor -> (PubrecInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrecInboundInterceptor> getPubrecInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubrecInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubrecInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrecOutboundInterceptor> getPubrecOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubrecOutboundInterceptor)
                .map(interceptor -> (PubrecOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrecOutboundInterceptor> getPubrecOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubrecOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubrecOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrelInboundInterceptor> getPubrelInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubrelInboundInterceptor)
                .map(interceptor -> (PubrelInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrelInboundInterceptor> getPubrelInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubrelInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubrelInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrelOutboundInterceptor> getPubrelOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubrelOutboundInterceptor)
                .map(interceptor -> (PubrelOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubrelOutboundInterceptor> getPubrelOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubrelOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubrelOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubcompInboundInterceptor> getPubcompInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubcompInboundInterceptor)
                .map(interceptor -> (PubcompInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubcompInboundInterceptor> getPubcompInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubcompInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubcompInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubcompOutboundInterceptor> getPubcompOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PubcompOutboundInterceptor)
                .map(interceptor -> (PubcompOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull PubcompOutboundInterceptor> getPubcompOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PubcompOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PubcompOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull SubscribeInboundInterceptor> getSubscribeInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof SubscribeInboundInterceptor)
                .map(interceptor -> (SubscribeInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull SubscribeInboundInterceptor> getSubscribeInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof SubscribeInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (SubscribeInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull SubackOutboundInterceptor> getSubackOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof SubackOutboundInterceptor)
                .map(interceptor -> (SubackOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull SubackOutboundInterceptor> getSubackOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof SubackOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (SubackOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull UnsubackOutboundInterceptor> getUnsubackOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof UnsubackOutboundInterceptor)
                .map(interceptor -> (UnsubackOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull UnsubackOutboundInterceptor> getUnsubackOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof UnsubackOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (UnsubackOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull DisconnectInboundInterceptor> getDisconnectInboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof DisconnectInboundInterceptor)
                .map(interceptor -> (DisconnectInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull DisconnectInboundInterceptor> getDisconnectInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof DisconnectInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (DisconnectInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull DisconnectOutboundInterceptor> getDisconnectOutboundInterceptorsForPlugin(
            final @NotNull IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof DisconnectOutboundInterceptor)
                .map(interceptor -> (DisconnectOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @Immutable @NotNull List<@NotNull DisconnectOutboundInterceptor> getDisconnectOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof DisconnectOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (DisconnectOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    public @NotNull ModifiableDefaultPermissions getDefaultPermissions() {
        return defaultPermissions;
    }

    private int comparePluginPriority(final Object object) {
        if (!(object.getClass().getClassLoader() instanceof IsolatedPluginClassloader)) {
            return -1;
        }
        final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(
                (IsolatedPluginClassloader) object.getClass().getClassLoader());
        if (plugin != null) {
            return plugin.getPriority();
        } else {
            return -1;
        }
    }

    private boolean hasPluginForClassloader(final Object object) {
        if (!(object.getClass().getClassLoader() instanceof IsolatedPluginClassloader)) {
            return true;
        }
        return hiveMQExtensions.getExtensionForClassloader(
                (IsolatedPluginClassloader) object.getClass().getClassLoader()) != null;
    }
}
