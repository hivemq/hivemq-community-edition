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
package com.hivemq.configuration.service.impl.listener;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.configuration.service.entity.TlsTcpListener;
import com.hivemq.configuration.service.entity.TlsWebsocketListener;
import com.hivemq.configuration.service.entity.WebsocketListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The default implementation of the listener configuration service.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class ListenerConfigurationServiceImpl implements ListenerConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ListenerConfigurationServiceImpl.class);

    /**
     * The actual listener. COWAL because we read a lot more than we write
     */
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public <T extends Listener> void addListener(final @NotNull T listener) {
        if (listener.getClass().equals(TcpListener.class) ||
                listener.getClass().equals(TlsTcpListener.class) ||
                listener.getClass().equals(WebsocketListener.class) ||
                listener.getClass().equals(TlsWebsocketListener.class)) {

            log.debug("Adding {} on bind address {} and port {}. Name: {}.",
                    listener.readableName(),
                    listener.getBindAddress(),
                    listener.getPort(),
                    listener.getName());

            listeners.add(listener);

            final ImmutableList<Listener> allListeners = ImmutableList.copyOf(listeners);
            log.trace("Notifying {} update listeners for changes", allListeners.size());
        } else {
            throw new IllegalArgumentException(listener.getClass().getName() + " is not a valid listener type");
        }
    }

    @Override
    public @NotNull ImmutableList<Listener> getListeners() {
        return ImmutableList.copyOf(listeners);
    }

    @Override
    public @NotNull ImmutableList<TcpListener> getTcpListeners() {
        return filterListeners(TcpListener.class);
    }

    @Override
    public @NotNull ImmutableList<TlsTcpListener> getTlsTcpListeners() {
        return filterListeners(TlsTcpListener.class);
    }

    @Override
    public @NotNull ImmutableList<WebsocketListener> getWebsocketListeners() {
        return filterListeners(WebsocketListener.class);
    }

    @Override
    public @NotNull ImmutableList<TlsWebsocketListener> getTlsWebsocketListeners() {
        return filterListeners(TlsWebsocketListener.class);
    }

    public void clear() {
        listeners.clear();
    }

    private <T extends Listener> @NotNull ImmutableList<T> filterListeners(final @NotNull Class<T> clazz) {
        final ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (final Listener listener : listeners) {
            //We're interested in the actual class, not subclasses!
            if (listener.getClass().equals(clazz)) {
                builder.add(clazz.cast(listener));
            }
        }
        return builder.build();
    }
}
