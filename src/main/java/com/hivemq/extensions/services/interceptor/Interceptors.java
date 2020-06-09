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
package com.hivemq.extensions.services.interceptor;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface Interceptors {

    /**
     * Add an connect interceptor provider to the connect interceptor provider map
     * If there is already a provider present, it will be overwritten
     *
     * @param provider to be added
     */
    void addConnectInboundInterceptorProvider(@NotNull ConnectInboundInterceptorProvider provider);

    /**
     * Get a map of connect interceptor providers (value) mapped by the id of the plugin which added the interceptor
     * provider (key)
     *
     * @return An immutable copy of the connect interceptor providers
     */
    @NotNull
    ImmutableMap<String, ConnectInboundInterceptorProvider> connectInboundInterceptorProviders();

    /**
     * Add an connack outbound interceptor provider to the connack outbound interceptor provider map
     *
     * @param provider to be added
     */
    void addConnackOutboundInterceptorProvider(@NotNull ConnackOutboundInterceptorProvider provider);

    /**
     * Get a map of connack outbound interceptor providers (value) mapped by the id of the plugin which added the
     * interceptor provider (key)
     *
     * @return An immutable copy of the connack outbound interceptor providers
     */
    @NotNull
    ImmutableMap<String, ConnackOutboundInterceptorProvider> connackOutboundInterceptorProviders();
}
