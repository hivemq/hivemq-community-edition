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
package com.hivemq.extension.sdk.api.interceptor.pingreq;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundOutput;

/**
 * Interface for the ping request inbound interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 *
 * @author Robin Atherton
 */
@FunctionalInterface
public interface PingReqInboundInterceptor extends Interceptor {

    /**
     * When a {@link PingReqInboundInterceptor} is set through any extension, this method gets called for every inbound
     * PINGREQ packet from any MQTT client.
     * <p/>
     * The execution of interceptor delays the handling of the PINGREQ.
     * Therefore the client may get disconnected if the execution of the interceptor takes to long.
     *
     * @param pingReqInboundInput  The {@link PingReqInboundInput} parameter.
     * @param pingReqInboundOutput The {@link PingReqInboundOutput} parameter.
     */
    void onInboundPingReq(
            @NotNull PingReqInboundInput pingReqInboundInput,
            @NotNull PingReqInboundOutput pingReqInboundOutput);
}
