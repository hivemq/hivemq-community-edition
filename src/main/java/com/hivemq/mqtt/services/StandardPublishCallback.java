/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.mqtt.services;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Exceptions;

/**
 * @author Christoph Schäbel
 */
public class StandardPublishCallback implements FutureCallback<PublishStatus> {

    private final @NotNull String subscriber;
    private final @NotNull PUBLISH msg;
    private final @NotNull SettableFuture<Void> publishFinishedFuture;

    StandardPublishCallback(final @NotNull String subscriber, final @NotNull PUBLISH msg, final @NotNull SettableFuture<Void> publishFinishedFuture) {
        this.subscriber = subscriber;
        this.msg = msg;
        this.publishFinishedFuture = publishFinishedFuture;
    }

    @Override
    public void onSuccess(@Nullable final PublishStatus response) {
        publishFinishedFuture.set(null);
    }

    @Override
    public void onFailure(@NotNull final Throwable throwable) {

        publishFinishedFuture.set(null);

        Exceptions.rethrowError("Unable to send message with id " + msg.getUniqueId() + " on topic " + msg.getTopic() + " to client " + subscriber + "", throwable);
    }
}
