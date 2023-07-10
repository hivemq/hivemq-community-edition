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
package com.hivemq.mqtt.message.publish;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishStatus;

public class PublishWithFuture extends PUBLISH {

    private final @NotNull SettableFuture<PublishStatus> future;
    private final boolean shared;

    public PublishWithFuture(
            final @NotNull PUBLISH publish, final @NotNull SettableFuture<PublishStatus> future, final boolean shared) {
        super(publish);
        this.future = future;
        this.shared = shared;
    }

    public @NotNull SettableFuture<PublishStatus> getFuture() {
        return future;
    }

    public boolean isShared() {
        return shared;
    }
}
