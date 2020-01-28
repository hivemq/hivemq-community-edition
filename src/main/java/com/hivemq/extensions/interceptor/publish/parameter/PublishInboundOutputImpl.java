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

package com.hivemq.extensions.interceptor.publish.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PublishInboundOutputImpl extends AbstractAsyncOutput<PublishInboundOutput>
        implements PublishInboundOutput, PluginTaskOutput, Supplier<PublishInboundOutputImpl> {

    private final @NotNull AtomicBoolean preventDelivery;
    private @NotNull AckReasonCode reasonCode = AckReasonCode.SUCCESS;
    private @Nullable String reasonString;

    private final @NotNull ModifiablePublishPacketImpl publishPacket;

    public PublishInboundOutputImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PUBLISH publish) {

        super(asyncer);
        this.publishPacket = new ModifiablePublishPacketImpl(configurationService, publish);
        this.preventDelivery = new AtomicBoolean(false);
    }

    @Override
    public @NotNull ModifiablePublishPacketImpl getPublishPacket() {
        return publishPacket;
    }

    @Override
    public void preventPublishDelivery() {
        preventPublishDelivery(AckReasonCode.SUCCESS, null);
    }

    @Override
    public void preventPublishDelivery(final @NotNull AckReasonCode reasonCode) {
        preventPublishDelivery(reasonCode, null);
    }

    @Override
    public void preventPublishDelivery(final @NotNull AckReasonCode reasonCode, final @Nullable String reasonString) {
        Preconditions.checkNotNull(reasonCode, "reason code must never be null");
        if (reasonCode == AckReasonCode.SUCCESS) {
            Preconditions.checkArgument(reasonString == null, "reason string must not be set when ack reason code is success");
        }
        checkPrevented();
        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
    }

    public void forciblyPreventPublishDelivery(final @NotNull AckReasonCode reasonCode, final @Nullable String reasonString) {
        Preconditions.checkNotNull(reasonCode, "reason code must never be null");
        this.preventDelivery.set(true);
        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
    }

    @Override
    public @NotNull Async<PublishInboundOutput> async(
            final @NotNull Duration duration,
            final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull AckReasonCode ackReasonCode) {

        return async(duration, timeoutFallback, ackReasonCode, null);
    }

    @Override
    public @NotNull Async<PublishInboundOutput> async(
            final @NotNull Duration duration,
            final @NotNull TimeoutFallback timeoutFallback,
            final @NotNull AckReasonCode ackReasonCode,
            final @Nullable String reasonString) {

        Preconditions.checkNotNull(duration, "Duration must never be null");
        Preconditions.checkNotNull(timeoutFallback, "Fallback must never be null");
        if (timeoutFallback == TimeoutFallback.SUCCESS) {
            Preconditions.checkArgument(ackReasonCode == AckReasonCode.SUCCESS, "reason code must be success when fallback success");
        }
        if (ackReasonCode == AckReasonCode.SUCCESS) {
            Preconditions.checkArgument(reasonString == null, "reason string must not be set when ack reason code is success");
        }
        Preconditions.checkNotNull(ackReasonCode, "Reason code must never be null");

        final Async<PublishInboundOutput> async = super.async(duration, timeoutFallback);
        this.reasonCode = ackReasonCode;
        this.reasonString = reasonString;
        return async;
    }

    @Override
    public @NotNull PublishInboundOutputImpl get() {
        return this;
    }

    public boolean isPreventDelivery() {
        return preventDelivery.get();
    }

    public @NotNull AckReasonCode getReasonCode() {
        return reasonCode;
    }

    public @Nullable String getReasonString() {
        return reasonString;
    }

    private void checkPrevented() {
        if (!preventDelivery.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("preventPublishDelivery must not be called more than once");
        }
    }
}
