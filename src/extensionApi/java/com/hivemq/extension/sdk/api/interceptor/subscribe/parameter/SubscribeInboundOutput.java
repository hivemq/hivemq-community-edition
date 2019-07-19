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

package com.hivemq.extension.sdk.api.interceptor.subscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;

import java.time.Duration;

/**
 * This is the output parameter of any {@link SubscribeInboundInterceptor}
 * providing methods to define the outcome of SUBSCRIBE interception.
 * <p>
 * It can be used to modify an inbound SUBSCRIBE packet.
 * <p>
 *
 * @author Florian Limpöck
 * @since 4.2.0
 */
@DoNotImplement
public interface SubscribeInboundOutput extends AsyncOutput<SubscribeInboundOutput> {

    /**
     * Use this object to make any changes to the inbound SUBSCRIBE.
     *
     * @return A modifiable subscribe packet.
     * @since 4.2.0
     */
    @NotNull
    ModifiableSubscribePacket getSubscribePacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     * <p>
     * {@link TimeoutFallback#FAILURE} results in a Suback with {@link SubackReasonCode#UNSPECIFIED_ERROR} and the
     * subscription will be prevented.
     * <p>
     * {@link TimeoutFallback#SUCCESS} will proceed the subscription.
     *
     * @param timeout  Timeout that HiveMQ waits for the result of the async operation.
     * @param fallback Fallback behaviour if a timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.2.0
     */
    @Override
    @NotNull Async<SubscribeInboundOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback fallback);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed.
     * This means that the outcome results in a Suback with {@link SubackReasonCode#UNSPECIFIED_ERROR} and the
     * subscription will be prevented.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.2.0
     */
    @Override
    @NotNull Async<SubscribeInboundOutput> async(@NotNull Duration timeout);

}
