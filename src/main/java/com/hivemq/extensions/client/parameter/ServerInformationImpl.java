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
package com.hivemq.extensions.client.parameter;

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
public class ServerInformationImpl implements ServerInformation {

    @NotNull
    private final SystemInformation systemInformation;

    @NotNull
    private final ListenerConfigurationService listenerConfigurationService;

    @Inject
    public ServerInformationImpl(@NotNull final SystemInformation systemInformation, @NotNull final ListenerConfigurationService listenerConfigurationService) {
        this.systemInformation = systemInformation;
        this.listenerConfigurationService = listenerConfigurationService;
    }

    @NotNull
    @Override
    public String getVersion() {
        return systemInformation.getHiveMQVersion();
    }

    @NotNull
    @Override
    public File getHomeFolder() {
        return systemInformation.getHiveMQHomeFolder();
    }

    @NotNull
    @Override
    public File getDataFolder() {
        return systemInformation.getDataFolder();
    }

    @NotNull
    @Override
    public File getLogFolder() {
        return systemInformation.getLogFolder();
    }

    @NotNull
    @Override
    public File getExtensionsFolder() {
        return systemInformation.getExtensionsFolder();
    }

    @NotNull
    @Override
    public Set<Listener> getListener() {
        final List<com.hivemq.configuration.service.entity.Listener> listeners = listenerConfigurationService.getListeners();
        final ImmutableSet.Builder<Listener> builder = ImmutableSet.builder();
        for (final com.hivemq.configuration.service.entity.Listener listener : listeners) {
            builder.add(new ListenerImpl(listener));
        }
        return builder.build();
    }

}
