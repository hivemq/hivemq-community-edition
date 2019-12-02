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

package com.hivemq.extension.sdk.api.interceptor.disconnect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link DisconnectInboundInterceptor} providing methods to define the outcome of a
 * DISCONNECT interception. It can be used to modify an inbound DISCONNECT packet.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface DisconnectInboundOutput extends SimpleAsyncOutput<DisconnectInboundOutput> {

    /**
     * Use this object to make any changes to the inbound DISCONNECT.
     *
     * @return A {@link ModifiableInboundDisconnectPacket}.
     */
    @NotNull ModifiableInboundDisconnectPacket getDisconnectPacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed. In that
     * case an unmodified DISCONNECT is forwarded to the next interceptor or server, all changes made by this
     * interceptor are not passed on.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @NotNull Async<DisconnectInboundOutput> async(@NotNull Duration timeout);
}
