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
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.cluster.ClusterDiscoveryCallback;

/**
 * Input for the methods of a {@link ClusterDiscoveryCallback
 * ClusterDiscoveryCallback}.
 *
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Immutable
@DoNotImplement
public interface ClusterDiscoveryInput {

    /**
     * Provides the address of the HiveMQ instance the extension is executed on.
     *
     * @return The address of the HiveMQ instance.
     * @since 4.0.0
     */
    @NotNull ClusterNodeAddress getOwnAddress();

    /**
     * Provides the cluster id of the HiveMQ instance the extension is executed on. This id is unique for all HiveMQ
     * instances in the same cluster.
     *
     * @return The cluster id of the HiveMQ instance.
     * @since 4.0.0
     */
    @NotNull String getOwnClusterId();

    /**
     * Provides the current reload interval for updating the addresses of all HiveMQ cluster nodes.
     *
     * @return The current reload interval in seconds.
     * @since 4.0.0
     */
    int getReloadInterval();

}
