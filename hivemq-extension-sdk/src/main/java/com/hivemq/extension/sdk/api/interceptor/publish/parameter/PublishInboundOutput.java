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
package com.hivemq.extension.sdk.api.interceptor.publish.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;

import java.time.Duration;

/**
 * This is the output parameter of any {@link PublishInboundInterceptor}
 * providing methods to define the outcome of PUBLISH interception.
 * <p>
 * It can be used to
 * <ul>
 * <li>Modify an inbound PUBLISH packet</li>
 * <li>Prevent delivery of an inbound PUBLISH packet</li>
 * </ul>
 * <p>
 * Only one of the methods {@link #preventPublishDelivery()} may be called.
 * <p>
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface PublishInboundOutput extends AsyncOutput<PublishInboundOutput> {

    /**
     * Use this object to make any changes to the inbound PUBLISH.
     *
     * @return A modifiable publish packet.
     * @since 4.0.0
     */
    @NotNull ModifiablePublishPacket getPublishPacket();

    /**
     * Prevent the onward delivery of the PUBLISH packet with reason code
     * {@link AckReasonCode#SUCCESS} for the PUBACK/PUBREC.
     *
     * @throws UnsupportedOperationException When preventPublishDelivery is called more than once.
     * @since 4.0.0
     */
    void preventPublishDelivery();

    /**
     * Prevent the onward delivery of the PUBLISH packet with <code>reasonCode</code> set as reason code in the
     * PUBACK/PUBREC.
     *
     * @param reasonCode The reason code to sent in PUBACK/PUBREC.
     * @throws UnsupportedOperationException When preventPublishDelivery is called more than once.
     * @since 4.0.0
     */
    void preventPublishDelivery(final @NotNull AckReasonCode reasonCode);

    /**
     * Prevent the onward delivery of the PUBLISH packet with <code>reasonCode</code> and <code>reasonString</code>
     * set as reason code and reason string in the PUBACK/PUBREC respectively.
     *
     * @param reasonCode   The reason code to sent in PUBACK/PUBREC.
     * @param reasonString The reason string to sent in PUBACK/PUBREC.
     * @throws UnsupportedOperationException When preventPublishDelivery is called more than once.
     * @since 4.0.0
     */
    void preventPublishDelivery(final @NotNull AckReasonCode reasonCode, final @Nullable String reasonString);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then the publish will be delivered.
     *                        If the fallback is FAILURE then the publish will be dropped.
     * @param reasonCode      The reason code sent in PUBACK/PUBREC when timeout occurs.
     * @param reasonString    The reason string sent in PUBACK/PUBREC when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<PublishInboundOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                               @NotNull AckReasonCode reasonCode, @Nullable String reasonString);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then the publish will be delivered.
     *                        If the fallback is FAILURE then the publish will be dropped.
     * @param reasonCode      The reason code sent in PUBACK/PUBREC when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<PublishInboundOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                               @NotNull AckReasonCode reasonCode);
}
