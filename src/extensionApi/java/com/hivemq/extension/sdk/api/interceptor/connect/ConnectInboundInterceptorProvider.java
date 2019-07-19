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

package com.hivemq.extension.sdk.api.interceptor.connect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;

/**
 * The interceptor provider allows to implement custom logic to modify incoming CONNECT messages.
 * For each incoming CONNECT message a {@link ConnectInboundInterceptor} can be provided that contains the modification logic.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface ConnectInboundInterceptorProvider {

    /**
     * This method is called for each incoming CONNECT message by HiveMQ.
     * <p>
     * Either the same {@link ConnectInboundInterceptor} (stateless or must be thread-safe)<br/>
     * or a new one (stateful, must not be thread-safe) can be supplied on each call.
     * <p>
     * <code>null</code> can be returned if a CONNECT message should not be intercepted.
     *
     * @return An implementation of the {@link ConnectInboundInterceptor} or null if the CONNECT should not be intercepted.
     * @since 4.2.0
     */
    @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull ConnectInboundProviderInput input);
}
