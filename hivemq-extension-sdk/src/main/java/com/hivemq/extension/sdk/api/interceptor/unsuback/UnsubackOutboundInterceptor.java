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

package com.hivemq.extension.sdk.api.interceptor.unsuback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundOutput;

/**
 * Interface for the UNSUBACK outbound interception.
 * <p>
 * Interceptors are always called by the same Thread for all UNSUBACK messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface UnsubackOutboundInterceptor extends Interceptor {

    /**
     * When an {@link UnsubackOutboundInterceptor} is set through any extension, this method gets called for every
     * outbound UNSUBACK packet from any MQTT client.
     *
     * @param unsubackOutboundInput  The {@link UnsubackOutboundInput} parameter.
     * @param unsubackOutboundOutput The {@link UnsubackOutboundOutput} parameter.
     */
    void onOutboundUnsuback(
            @NotNull UnsubackOutboundInput unsubackOutboundInput,
            @NotNull UnsubackOutboundOutput unsubackOutboundOutput);
}
