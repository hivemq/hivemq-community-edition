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
package com.hivemq.logging;

import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.Channel;

public final class LoggingUtils {

    private LoggingUtils() {
    }

    /**
     * Append listeners readableName and port to any message
     *
     * @param channel the channel of the client connection
     * @param message the message to append listener and port to
     * @return the original message with appended listener and port.
     * @since 4.15.0
     */
    public static @NotNull String appendListenerToMessage(
            final @NotNull Channel channel, final @NotNull String message) {
        final Listener listener = ClientConnectionContext.of(channel).getConnectedListener();
        final String listenerName = listener.readableName();
        final int listenerPort = listener.getPort();
        return String.format("%s for %s on port: %d", message, listenerName, listenerPort);
    }

}
