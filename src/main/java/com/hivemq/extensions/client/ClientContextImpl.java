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
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQPlugins;
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
    private final HiveMQPlugins hiveMQPlugins;

    public ClientContextImpl(@NotNull final HiveMQPlugins hiveMQPlugins, @NotNull final ModifiableDefaultPermissions defaultPermissions) {
        this.interceptorList = new CopyOnWriteArrayList<>();
        this.hiveMQPlugins = hiveMQPlugins;
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

    public void removePublishInboundInterceptor(@NotNull final PublishInboundInterceptor interceptor) {
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
    public List<PublishInboundInterceptor> getPublishInboundInterceptors() {
        return interceptorList.stream()
                .filter(interceptor -> interceptor instanceof PublishInboundInterceptor)
                .filter(this::hasPluginForClassloader)
                .sorted(Comparator.comparingInt(this::comparePluginPriority).reversed())
                .map(interceptor -> (PublishInboundInterceptor) interceptor)
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
        final HiveMQExtension plugin = hiveMQPlugins.getPluginForClassloader((IsolatedPluginClassloader) object.getClass().getClassLoader());
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
        return hiveMQPlugins.getPluginForClassloader((IsolatedPluginClassloader) object.getClass().getClassLoader()) != null;
    }
}
