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
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public class PublishOutboundOutputImpl extends AbstractAsyncOutput<PublishOutboundOutput> implements PublishOutboundOutput, PluginTaskOutput, Supplier<PublishOutboundOutputImpl> {

    private final @NotNull AtomicBoolean preventDelivery;

    private final @NotNull ModifiableOutboundPublishImpl publishPacket;

    public PublishOutboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull PUBLISH publish) {
        super(asyncer);
        this.publishPacket = new ModifiableOutboundPublishImpl(configurationService, publish);
        this.preventDelivery = new AtomicBoolean(false);
    }

    @Override
    public @NotNull ModifiableOutboundPublishImpl getPublishPacket() {
        return publishPacket;
    }


    @Override
    public void preventPublishDelivery() {
        checkPrevented();
    }

    public void forciblyPreventPublishDelivery() {
        this.preventDelivery.set(true);
    }

    @Override
    public @NotNull Async<PublishOutboundOutput> async(final @NotNull Duration duration, final @NotNull TimeoutFallback timeoutFallback) {

        Preconditions.checkNotNull(duration, "Duration must never be null");
        Preconditions.checkNotNull(timeoutFallback, "Fallback must never be null");

        return super.async(duration, timeoutFallback);
    }


    @Override
    public @NotNull PublishOutboundOutputImpl get() {
        return this;
    }

    public boolean isPreventDelivery() {
        return preventDelivery.get();
    }

    private void checkPrevented() {
        if (!preventDelivery.compareAndSet(false, true)) {
            throw new UnsupportedOperationException("preventPublishDelivery must not be called more than once");
        }
    }
}
