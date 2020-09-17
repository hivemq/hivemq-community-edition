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

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ListenerType;
import com.hivemq.extensions.ExtensionInformationUtil;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ListenerImpl implements Listener {

    private final int port;
    private final @NotNull String bindAddress;
    private final @NotNull ListenerType listenerType;
    private final @NotNull String name;

    public ListenerImpl(final @NotNull com.hivemq.configuration.service.entity.Listener hiveMQListener) {
        Preconditions.checkNotNull(hiveMQListener, "listener must never be null");
        this.port = hiveMQListener.getPort();
        this.bindAddress = hiveMQListener.getBindAddress();
        this.listenerType = ExtensionInformationUtil.listenerTypeFromInstance(hiveMQListener);
        this.name = hiveMQListener.getName();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public @NotNull String getBindAddress() {
        return bindAddress;
    }

    @Override
    public @NotNull ListenerType getListenerType() {
        return listenerType;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }
}
