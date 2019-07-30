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

package com.hivemq.extension.sdk.api.interceptor.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundProviderInput;

/**
 * The interceptor provider allows to implement custom logic to modify outbound CONNACK messages.
 * For each outbound CONNACK message an {@link ConnackOutboundInterceptor} can be provided that contains the
 * modification logic.
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@FunctionalInterface
public interface ConnackOutboundInterceptorProvider {

    /**
     * This method is called for each outbound CONNACK message by HiveMQ.
     * <p>
     * Either the same {@link ConnackOutboundInterceptor} (stateless or must be thread-safe)<br/>
     * or a new one (stateful, must not be thread-safe) can be supplied on each call.
     * <p>
     * <code>null</code> can be returned if a CONNACK message should not be intercepted.
     *
     * @return An implementation of the {@link ConnackOutboundInterceptor} or null if the CONNECT should not be intercepted.
     * @since 4.2.0
     */
    @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(@NotNull ConnackOutboundProviderInput connackOutboundProviderInput);
}
