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

package com.hivemq.extension.sdk.api.services.interceptor;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;

/**
 * @author Lukas Brandl
 * @author Florian Limpöck
 * @since 4.2.0
 */
public interface GlobalInterceptorRegistry {

    /**
     * The provider is called once for each incoming CONNECT message.
     * <p>
     * The {@link ConnectInboundInterceptorProvider} must be implemented by the extension developer.
     * It will return an {@link ConnectInboundInterceptor} that can be used to modify incoming CONNECT messages.
     * If there is already a provider present, it will be overwritten.
     *
     * @param connectInboundInterceptorProvider The provider to be registered.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.2.0
     */
    void setConnectInboundInterceptorProvider(@NotNull ConnectInboundInterceptorProvider connectInboundInterceptorProvider);

    /**
     * The provider is called once for each outbound CONNACK message.
     * <p>
     * The {@link ConnackOutboundInterceptorProvider} must be implemented by the extension developer.
     * It will return an {@link ConnackOutboundInterceptor} that can be used to modify outbound CONNACK messages.
     * If there is already a provider present, it will be overwritten.
     *
     * @param connackOutboundInterceptorProvider The provider to be registered.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.2.0
     */
    void setConnackOutboundInterceptorProvider(@NotNull ConnackOutboundInterceptorProvider connackOutboundInterceptorProvider);
}
