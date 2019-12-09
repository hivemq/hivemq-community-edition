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
package com.hivemq.extension.sdk.api.services.cluster;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * Service for cluster related configuration.
 * <p>
 * It has the following functions:
 * <ul>
 * <li>Adding and removing callbacks for discovery of HiveMQ cluster nodes</li>
 * </ul>
 *
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @since 4.0.0
 */
@DoNotImplement
public interface ClusterService {

    /**
     * Adds a {@link ClusterDiscoveryCallback} that will be used by HiveMQ to discover cluster nodes.
     * <p>
     * Note: Extension discovery is only used by HiveMQ if {@code <discovery>} is set to {@code <extension>} in the
     * {@code <cluster>} section of the HiveMQ config file.
     * <p>
     * If the given callback is already added, the callback is not added once more.
     *
     * @param clusterDiscoveryCallback The callback to add to the cluster discovery callbacks.
     * @since 4.0.0
     */
    void addDiscoveryCallback(@NotNull ClusterDiscoveryCallback clusterDiscoveryCallback);

    /**
     * Removes a {@link ClusterDiscoveryCallback} from the callbacks added by {@link
     * #addDiscoveryCallback(ClusterDiscoveryCallback)}.
     * <p>
     * The removed callback will not be used anymore by HiveMQ to discover cluster nodes.
     * <p>
     * If the given callback is not added or removed already, this method does not change anything.
     *
     * @param clusterDiscoveryCallback The callback to remove from the cluster discovery callbacks.
     * @since 4.0.0
     */
    void removeDiscoveryCallback(@NotNull ClusterDiscoveryCallback clusterDiscoveryCallback);
}
