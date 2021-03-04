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
 * A listener which allows to listen to MQTT traffic via TLS
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @since 3.0
 */
@Immutable
public class TlsTcpListener extends TcpListener implements TlsListener {

    private final @NotNull Tls tls;

    /**
     * Creates a new TLS Listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     * @param tls         the TLS configuration
     */
    public TlsTcpListener(
            final int port,
            final @NotNull String bindAddress,
            final @NotNull Tls tls) {
        this(port, bindAddress, tls, "tls-tcp-listener-" + port);
    }

    /**
     * Creates a new TLS Listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     * @param tls         the TLS configuration
     * @param name        the name of the listener
     */
    public TlsTcpListener(
            final int port,
            final @NotNull String bindAddress,
            final @NotNull Tls tls,
            final @NotNull String name) {
        super(port, bindAddress, name);
        checkNotNull(tls);
        this.tls = tls;
    }

    /**
     * @return the TLS configuration
     */
    @NotNull
    public Tls getTls() {
        return tls;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String readableName() {
        return "TCP Listener with TLS";
    }

}
