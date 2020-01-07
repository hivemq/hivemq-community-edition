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
package com.hivemq.extension.sdk.api.interceptor.pingresp;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.parameter.PingRespOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresp.parameter.PingRespOutboundOutput;

/**
 * Interface for the ping response interception.
 * <p>
 * Interceptors are always called by the same thread for all messages for the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called by different threads and therefore must be
 * thread-safe.
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface PingRespOutboundInterceptor extends Interceptor {

    /**
     * When a {@link PingRespOutboundInterceptor} is set through any extension, this method gets called for every
     * outbound PINGRESP packet from any MQTT client.
     * <p/>
     * The execution of interceptor delays the handling of the PINGRESP.
     * Therefore the client may disconnect if the execution of the interceptor takes to long.
     *
     * @param pingRespOutboundInput  The {@link PingRespOutboundInput} parameter.
     * @param pingRespOutboundOutput The {@link PingRespOutboundOutput} parameter.
     */
    void onOutboundPingResp(
            @NotNull PingRespOutboundInput pingRespOutboundInput,
            @NotNull PingRespOutboundOutput pingRespOutboundOutput);
}
