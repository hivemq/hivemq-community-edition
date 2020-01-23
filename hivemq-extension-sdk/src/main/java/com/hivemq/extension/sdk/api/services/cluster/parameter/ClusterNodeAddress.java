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
package com.hivemq.extension.sdk.api.services.cluster.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a cluster node's address.
 *
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @since 4.0.0
 */
public class ClusterNodeAddress {

    private final @NotNull String host;
    private final int port;

    /**
     * Create the nodes address for the cluster.
     *
     * @param host The hostname or IP address of the cluster node.
     * @param port The cluster bind-port of the cluster node.
     * @throws NullPointerException     If host is null.
     * @throws IllegalArgumentException If port is not a valid port number (0-65535).
     * @since 4.0.0
     */
    public ClusterNodeAddress(final @NotNull String host, final int port) {
        Objects.requireNonNull(host, "host must not be null");
        if ((port < 0) || (port > 65535)) {
            throw new IllegalArgumentException("port must be within range [0, 65535]");
        }
        this.host = host;
        this.port = port;
    }

    /**
     * @return The host name.
     * @since 4.0.0
     */
    public @NotNull String getHost() {
        return host;
    }

    /**
     * @return The port number.
     * @since 4.0.0
     */
    public int getPort() {
        return port;
    }

    /**
     * Compare the {@link ClusterNodeAddress} against another object.
     *
     * @param o The other object.
     * @return <b>true</b> if the object is also a {@link ClusterNodeAddress} with the same host and port value.
     * @since 4.0.0
     */
    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClusterNodeAddress)) {
            return false;
        }
        final ClusterNodeAddress that = (ClusterNodeAddress) o;
        return (port == that.port) && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public @NotNull String toString() {
        return host + ":" + port;
    }
}