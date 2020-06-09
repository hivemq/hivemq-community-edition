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
package com.hivemq.extensions.services.session;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class SessionInformationImpl implements SessionInformation {

    @NotNull
    private final String clientIdentifier;
    private final long sessionExpiryInterval;
    private final boolean connected;

    public SessionInformationImpl(@NotNull final String clientIdentifier, final long sessionExpiryInterval, final boolean connected) {
        this.clientIdentifier = clientIdentifier;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.connected = connected;
    }

    @NotNull
    @Override
    public String getClientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
