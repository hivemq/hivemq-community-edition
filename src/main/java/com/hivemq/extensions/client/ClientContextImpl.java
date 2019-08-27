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
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
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

    @NotNull
    private final List<Interceptor> interceptorList;

    @NotNull
    private final ModifiableDefaultPermissions defaultPermissions;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    public ClientContextImpl(
            @NotNull final HiveMQExtensions hiveMQExtensions,
            @NotNull final ModifiableDefaultPermissions defaultPermissions) {
        this.interceptorList = new CopyOnWriteArrayList<>();
        this.hiveMQExtensions = hiveMQExtensions;
        this.defaultPermissions = defaultPermissions;
    }

    public void addInterceptor(@NotNull final Interceptor interceptor) {
        if (!interceptorList.contains(interceptor)) {
            interceptorList.add(interceptor);
        }
    }

    public void addPublishInboundInterceptor(@NotNull final PublishInboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void addSubscribeInboundInterceptor(@NotNull final SubscribeInboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void addUnsubscribeInboundInterceptor(@NotNull final UnsubscribeInboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void removePublishInboundInterceptor(@NotNull final PublishInboundInterceptor interceptor) {
        removeInterceptor(interceptor);
    }

    public void addPublishOutboundInterceptor(@NotNull final PublishOutboundInterceptor interceptor) {
        addInterceptor(interceptor);
    }

    public void removePublishOutboundInterceptor(@NotNull final PublishOutboundInterceptor interceptor) {
        removeInterceptor(interceptor);
    }

    public void removeSubscribeInboundInterceptor(@NotNull final SubscribeInboundInterceptor interceptor) {
        removeInterceptor(interceptor);
    }

    public void removeUnsubscribeInboundInterceptor(@NotNull final UnsubscribeInboundInterceptor interceptor) {
        removeInterceptor(interceptor);
    }

    public void removeInterceptor(@NotNull final Interceptor interceptor) {
        interceptorList.remove(interceptor);
    }

    @NotNull
    @Immutable
    public List<Interceptor> getAllInterceptorsForPlugin(@NotNull final IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<Interceptor> getAllInterceptors() {
        return interceptorList.stream()
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<PublishInboundInterceptor> getPublishInboundInterceptorsForPlugin(@NotNull final IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PublishInboundInterceptor)
                .map(interceptor -> (PublishInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<SubscribeInboundInterceptor> getSubscribeInboundInterceptorsForPlugin(@NotNull final IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof SubscribeInboundInterceptor)
                .map(interceptor -> (SubscribeInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<PublishInboundInterceptor> getPublishInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PublishInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PublishInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<PublishOutboundInterceptor> getPublishOutboundInterceptorsForPlugin(@NotNull final IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof PublishOutboundInterceptor)
                .map(interceptor -> (PublishOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<PublishOutboundInterceptor> getPublishOutboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PublishOutboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PublishOutboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<SubscribeInboundInterceptor> getSubscribeInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof SubscribeInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (SubscribeInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<UnsubscribeInboundInterceptor> getUnsubscribeInboundInterceptorsForPlugin(
            @NotNull final IsolatedPluginClassloader pluginClassloader) {
        return interceptorList.stream()
                .filter(interceptor -> interceptor.getClass().getClassLoader().equals(pluginClassloader))
                .filter(interceptor -> interceptor instanceof UnsubscribeInboundInterceptor)
                .map(interceptor -> (UnsubscribeInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Immutable
    public List<UnsubscribeInboundInterceptor> getUnsubscribeInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof UnsubscribeInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (UnsubscribeInboundInterceptor) interceptor)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    public ModifiableDefaultPermissions getDefaultPermissions() {
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
