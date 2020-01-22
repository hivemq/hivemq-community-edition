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

import com.google.common.util.concurrent.FutureCallback;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.ChannelInactiveHandler;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.publish.PUBLISH;

/**
 * Callback which is called when at least a persistence task (outgoing message flow or queued messages) is created for a
 * publish
 */
public class PublishStoredInPersistenceCallback implements FutureCallback<PublishStatus> {
    private final ChannelInactiveHandler channelInactiveHandler;
    private final PUBLISH publish;

    public PublishStoredInPersistenceCallback(final ChannelInactiveHandler channelInactiveHandler,
                                              final PUBLISH publish) {
        this.channelInactiveHandler = channelInactiveHandler;
        this.publish = publish;
    }

    @Override
    public void onSuccess(@Nullable final PublishStatus result) {
        channelInactiveHandler.removeCallback(publish.getUniqueId());
    }

    @Override
    public void onFailure(final Throwable t) {
        // Exception is logged in a different callback
        channelInactiveHandler.removeCallback(publish.getUniqueId());
    }
}