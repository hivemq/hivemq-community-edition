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
import com.hivemq.configuration.service.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation of the listener configuration service.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class ListenerConfigurationServiceImpl implements InternalListenerConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ListenerConfigurationServiceImpl.class);

    /**
     * The actual listener. COWAL because we read a lot more than we write
     */
    final List<Listener> listeners = new CopyOnWriteArrayList<>();

    /**
     * The update listeners that are called as soon as the listener config changes
     */
    final List<InternalListenerConfigurationService.UpdateListener> updateListeners = new CopyOnWriteArrayList<>();

    @Override
    public <T extends Listener> void addListener(final T listener) {
        if (listener.getClass().equals(TcpListener.class) ||
                listener.getClass().equals(TlsTcpListener.class) ||
                listener.getClass().equals(WebsocketListener.class) ||
                listener.getClass().equals(TlsWebsocketListener.class)) {

            log.debug("Adding {} on bind address {} and port {}. Name: {}.",
                    listener.readableName(), listener.getBindAddress(),
                    listener.getPort(), listener.getName());
            listeners.add(listener);

            final ImmutableList<Listener> allListeners = ImmutableList.copyOf(listeners);
            log.trace("Notifying {} update listeners for changes", allListeners.size());

            //We're calling the Update Listeners in the same thread
            for (final UpdateListener updateListener : updateListeners) {

                log.trace("Notifying update listener {}", allListeners.getClass());
                updateListener.update(listener, allListeners);
            }

        } else {
            throw new IllegalArgumentException(listener.getClass().getName() + " is not a valid listener type");
        }

    }

    @Override
    public ImmutableList<Listener> getListeners() {
        return ImmutableList.copyOf(listeners);
    }

    @Override
    public ImmutableList<TcpListener> getTcpListeners() {
        return filterListeners(TcpListener.class);
    }

    @Override
    public ImmutableList<TlsTcpListener> getTlsTcpListeners() {
        return filterListeners(TlsTcpListener.class);
    }

    @Override
    public ImmutableList<WebsocketListener> getWebsocketListeners() {
        return filterListeners(WebsocketListener.class);
    }

    @Override
    public ImmutableList<TlsWebsocketListener> getTlsWebsocketListeners() {
        return filterListeners(TlsWebsocketListener.class);
    }

    public void clear() {
        listeners.clear();
    }

    <T extends Listener> ImmutableList<T> filterListeners(final Class<T> clazz) {
        final ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (final Listener listener : listeners) {
            //We're interested in the actual class, not subclasses!
            if (listener.getClass().equals(clazz)) {
                builder.add((T) listener);
            }
        }
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUpdateListener(final UpdateListener listener) {
        checkNotNull(listener, "Update Listener must not be null");

        log.trace("Adding update listener {}", listener.getClass());
        updateListeners.add(listener);

        log.trace("Notifying update listener {}", listener.getClass());
        listener.onRegister(ImmutableList.copyOf(listeners));
    }
}
