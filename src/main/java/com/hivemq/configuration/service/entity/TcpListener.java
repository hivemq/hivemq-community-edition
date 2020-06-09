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
package com.hivemq.configuration.service.entity;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A listener which allows to listen to MQTT traffic via TCP
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @author Georg Held
 * @since 3.0
 */
@Immutable
public class TcpListener implements Listener {

    private final int port;
    private final String name;

    private final @NotNull String bindAddress;

    /**
     * Creates a new TCP listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     */
    public TcpListener(final int port, @NotNull final String bindAddress) {
        this(port, bindAddress, "tcp-listener-" + port);
    }

    /**
     * Creates a new TCP listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     * @param name        the name of the listener
     */
    public TcpListener(final int port, @NotNull final String bindAddress, final @NotNull String name) {

        checkNotNull(bindAddress, "bindAddress must not be null");

        this.port = port;
        this.bindAddress = bindAddress;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String getBindAddress() {
        return bindAddress;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String readableName() {
        return "TCP Listener";
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

}
