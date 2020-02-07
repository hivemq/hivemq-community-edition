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

package com.hivemq.mqtt.callback;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.ChannelInactiveHandler;
import com.hivemq.mqtt.handler.publish.PublishStatus;

/**
 * Callback which is called when the channel becomes inactive before a message has been at least stored in any
 * persistence
 */
public class PublishChannelInactiveCallback implements ChannelInactiveHandler.ChannelInactiveCallback {
    @NotNull
    private final SettableFuture<PublishStatus> publishFuture;

    public PublishChannelInactiveCallback(@NotNull final SettableFuture<PublishStatus> publishFuture) {
        this.publishFuture = publishFuture;
    }

    @Override
    public void channelInactive() {
        publishFuture.set(PublishStatus.NOT_CONNECTED);
    }
}