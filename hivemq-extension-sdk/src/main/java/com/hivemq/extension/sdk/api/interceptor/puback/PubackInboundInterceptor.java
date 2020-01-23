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
package com.hivemq.extension.sdk.api.interceptor.puback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import com.hivemq.extension.sdk.api.packets.puback.ModifiablePubackPacket;

import java.time.Duration;

/**
 * Interface for the inbound PUBACK interception.
 * <p>
 * Interceptors are always called by the same Thread for all messages from the same client.
 * <p>
 * If the same instance is shared between multiple clients it can be called in different Threads and must therefore be
 * thread-safe.
 * <p>
 * When the method {@link #onInboundPuback(PubackInboundInput, PubackInboundOutput)} throws an exception or a call to
 * {@link PubackInboundOutput#async(Duration)} times out with {@link TimeoutFallback#FAILURE}, HiveMQ will ignore this
 * interceptor and will:
 * <ol>
 *    <li>Log the exception</li>
 *    <li>Revert the changes to the {@link ModifiablePubackPacket} made by the interceptor</li>
 *    <li>Call the next {@link PubackInboundInterceptor} or send the PUBACK to the server if no interceptor is left</li>
 * </ol>
 *
 * @author Yannick Weber
 */
@FunctionalInterface
public interface PubackInboundInterceptor extends Interceptor {

    /**
     * When a {@link PubackInboundInterceptor} is set through any extension, this method gets called for every inbound
     * PUBACK packet from any MQTT client.
     *
     * @param pubackInboundInput  The {@link PubackInboundInput} parameter.
     * @param pubackInboundOutput The {@link PubackInboundOutput} parameter.
     */
    void onInboundPuback(
            @NotNull PubackInboundInput pubackInboundInput,
            @NotNull PubackInboundOutput pubackInboundOutput);
}
