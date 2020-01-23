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

package com.hivemq.extension.sdk.api.interceptor.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;

/**
 * Interface for the outbound DISCONNECT interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * This interceptor is only called for MQTT 5 clients. With MQTT 3 it is not possible to sent a DISCONNECT from server
 * to client.
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface DisconnectOutboundInterceptor extends Interceptor {

    /**
     * When a {@link DisconnectOutboundInterceptor} is set through any extension, this method gets called for every
     * outbound DISCONNECT packet from any MQTT 5 client.
     *
     * @param disconnectOutboundInput  The {@link DisconnectOutboundInput} parameter.
     * @param disconnectOutboundOutput The {@link DisconnectOutboundOutput} parameter.
     */
    void onOutboundDisconnect(
            @NotNull DisconnectOutboundInput disconnectOutboundInput,
            @NotNull DisconnectOutboundOutput disconnectOutboundOutput);
}
