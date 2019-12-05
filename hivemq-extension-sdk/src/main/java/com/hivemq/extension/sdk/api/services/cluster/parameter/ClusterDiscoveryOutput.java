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

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.cluster.ClusterDiscoveryCallback;

import java.util.List;

/**
 * Output for the methods of a {@link ClusterDiscoveryCallback
 * ClusterDiscoveryCallback} that provide the current addresses of all HiveMQ cluster nodes.
 *
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @since 4.0.0
 */
@DoNotImplement
public interface ClusterDiscoveryOutput {

    /**
     * Provides the current addresses of all HiveMQ cluster nodes.
     *
     * @param nodeAddresses The current addresses of all HiveMQ cluster nodes.
     * @throws NullPointerException If node addresses is null or one of the elements is null.
     * @since 4.0.0
     */
    void provideCurrentNodes(@NotNull List<@NotNull ClusterNodeAddress> nodeAddresses);

    /**
     * Overwrites the current reload interval for updating the addresses of all HiveMQ cluster nodes.
     * <p>
     * The reload interval is in seconds and must be at least 1.
     *
     * @param reloadInterval The reload interval in seconds.
     * @throws IllegalArgumentException If the reload interval is less than one.
     * @since 4.0.0
     */
    void setReloadInterval(int reloadInterval);
}
