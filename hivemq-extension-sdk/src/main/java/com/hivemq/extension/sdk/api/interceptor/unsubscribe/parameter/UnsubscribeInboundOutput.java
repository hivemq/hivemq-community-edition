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

package com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link UnsubscribeInboundInterceptor}.
 * <p>
 * It can be used to modify an inbound UNSUBSCRIBE packet.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface UnsubscribeInboundOutput extends AsyncOutput<UnsubscribeInboundOutput> {

    /**
     * Use this Object to make any changes to the inbound UNSUBSCRIBE.
     *
     * @return A modifiable UNSUBSCRIBE packet.
     */
    @NotNull ModifiableUnsubscribePacket getUnsubscribePacket();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is handled as failed. This
     * means that the outcome results in an Unsuback with {@link UnsubackReasonCode#UNSPECIFIED_ERROR} and the
     * unsubscribe will be prevented.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout Timeout that HiveMQ waits for the result of the async operation.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @Override
    @NotNull Async<UnsubscribeInboundOutput> async(@NotNull Duration timeout);
}
