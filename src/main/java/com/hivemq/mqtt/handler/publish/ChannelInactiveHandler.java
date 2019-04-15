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

package com.hivemq.mqtt.handler.publish;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler which is used to prevent messages from getting lost if
 * a user event is fired from this channel but never handled because the channel is being closed
 *
 * @author Christoph Schäbel
 */
public class ChannelInactiveHandler extends ChannelDuplexHandler {

    private final Map<String, ChannelInactiveCallback> callbacks = new ConcurrentHashMap<>();

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        for (final ChannelInactiveCallback callback : callbacks.values()) {
            callback.channelInactive();
        }

        super.channelInactive(ctx);
    }

    public void addCallback(final String id, final ChannelInactiveCallback callback) {
        callbacks.put(id, callback);
    }

    public void removeCallback(final String id) {
        callbacks.remove(id);
    }

    public interface ChannelInactiveCallback {
        void channelInactive();
    }
}
