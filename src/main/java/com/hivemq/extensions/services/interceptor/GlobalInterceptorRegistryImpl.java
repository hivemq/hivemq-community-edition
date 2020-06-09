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

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.services.interceptor.GlobalInterceptorRegistry;

import javax.inject.Inject;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public class GlobalInterceptorRegistryImpl implements GlobalInterceptorRegistry {

    @NotNull
    private final Interceptors interceptors;

    @Inject
    public GlobalInterceptorRegistryImpl(@NotNull final Interceptors interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void setConnectInboundInterceptorProvider(@NotNull final ConnectInboundInterceptorProvider connectInboundInterceptorProvider) {
        Preconditions.checkNotNull(connectInboundInterceptorProvider, "Connect interceptor provider must never be null");
        interceptors.addConnectInboundInterceptorProvider(connectInboundInterceptorProvider);
    }

    @Override
    public void setConnackOutboundInterceptorProvider(@NotNull final ConnackOutboundInterceptorProvider provider) {
        Preconditions.checkNotNull(provider, "Connack outbound interceptor provider must never be null");
        interceptors.addConnackOutboundInterceptorProvider(provider);
    }
}
