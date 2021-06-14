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
import com.hivemq.extension.sdk.api.annotations.Nullable;

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

    private int port;

    private final @NotNull String name;
    private final @NotNull String bindAddress;

    /**
     * Creates a new TCP listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     */
    @Deprecated
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
    public TcpListener(final int port, final @NotNull String bindAddress, final @NotNull String name) {

        checkNotNull(bindAddress, "bindAddress must not be null");
        checkNotNull(name, "name must not be null");

        this.port = port;
        this.bindAddress = bindAddress;
        this.name = name;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(final int port) {
        this.port = port;
    }

    @Override
    public @NotNull String getBindAddress() {
        return bindAddress;
    }

    @Override
    public @NotNull String readableName() {
        return "TCP Listener";
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    public static class Builder {

        private @Nullable String name;
        private @Nullable Integer port;
        private @Nullable String bindAddress;

        public Builder() {
        }

        public Builder(final @NotNull TcpListener tcpListener) {
            port = tcpListener.getPort();
            bindAddress = tcpListener.getBindAddress();
            name = tcpListener.getName();
        }

        public @NotNull Builder port(final int port) {
            this.port = port;
            return this;
        }

        public @NotNull Builder bindAddress(final @NotNull String bindAddress) {
            checkNotNull(bindAddress);
            this.bindAddress = bindAddress;
            return this;
        }

        public @NotNull Builder name(final @NotNull String name) {
            checkNotNull(name);
            this.name = name;
            return this;
        }

        public @NotNull TcpListener build() throws IllegalStateException {
            if (port == null) {
                throw new IllegalStateException("The port for a TCP listener was not set.");
            }
            if (bindAddress == null) {
                throw new IllegalStateException("The bind address for a TCP listener was not set.");
            }
            if (name == null) {
                name = "tcp-listener-" + port;
            }
            return new TcpListener(port, bindAddress, name);
        }
    }
}
