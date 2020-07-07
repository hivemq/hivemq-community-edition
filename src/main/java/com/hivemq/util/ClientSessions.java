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

package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.clientsession.ClientSession;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Lukas Brandl
 */
public class ClientSessions {

    /**
     * Check if the time to live for a client session is expired
     *
     * @param clientSession       to check
     * @param timeSinceDisconnect The time that passed since the client disconnected in milliseconds
     * @return true if the client sessions ttl is expired
     */
    public static boolean isExpired(final @NotNull ClientSession clientSession, final long timeSinceDisconnect) {
        checkNotNull(clientSession);

        if (clientSession.isConnected()) {
            return false;
        }

        final long timeSinceDisconnectSeconds = timeSinceDisconnect / 1000;

        return timeSinceDisconnectSeconds >= clientSession.getSessionExpiryInterval();
    }
}
