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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryInput;
import com.hivemq.extension.sdk.api.services.cluster.parameter.ClusterDiscoveryOutput;

/**
 * This callback is meant to regularly discover the addresses of all available HiveMQ cluster nodes.
 * <p>
 * Note: Extension discovery is only used by HiveMQ if {@code <discovery>} is set to {@code <extension>} in the
 * {@code <cluster>} section of the HiveMQ config file.
 *
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @since 4.0.0
 */
public interface ClusterDiscoveryCallback {

    /**
     * This method is called once by HiveMQ when this callback is added via
     * {@link ClusterService#addDiscoveryCallback(ClusterDiscoveryCallback)}.
     * <p>
     * It can be used to register this HiveMQ instance with some kind of central registry or to save it to a database or
     * file server for example.
     *
     * @param clusterDiscoveryInput  The object providing input information for cluster discovery.
     * @param clusterDiscoveryOutput The object for providing output information for cluster discovery, e.g. the
     *                               addresses of all HiveMQ cluster nodes.
     * @since 4.0.0
     */
    void init(@NotNull ClusterDiscoveryInput clusterDiscoveryInput,
              @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput);

    /**
     * This method is called regularly by HiveMQ to discover all current available cluster nodes.
     * <p>
     * The interval between calls to this method is 60 seconds by default and can be overwritten by an individual {@link
     * ClusterDiscoveryCallback} via {@link ClusterDiscoveryOutput#setReloadInterval(int)}.
     *
     * @param clusterDiscoveryInput  The object providing input information for cluster discovery.
     * @param clusterDiscoveryOutput The object for providing output information for cluster discovery, e.g. the
     *                               addresses of all HiveMQ cluster nodes.
     * @since 4.0.0
     */
    void reload(@NotNull ClusterDiscoveryInput clusterDiscoveryInput,
                @NotNull ClusterDiscoveryOutput clusterDiscoveryOutput);

    /**
     * This method is called once by HiveMQ in one of the following cases:
     * <ul>
     * <li>This callback is removed via {@link ClusterService#removeDiscoveryCallback(ClusterDiscoveryCallback)}</li>
     * <li>The extension which added the callback is stopped</li>
     * <li>The HiveMQ instance is shut down</li>
     * </ul>
     * <p>
     * It can be used to unregister this HiveMQ instance from a central registry.
     *
     * @param clusterDiscoveryInput The object providing input information for cluster discovery.
     * @since 4.0.0
     */
    void destroy(@NotNull ClusterDiscoveryInput clusterDiscoveryInput);
}
