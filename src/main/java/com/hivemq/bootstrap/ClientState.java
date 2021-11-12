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
package com.hivemq.bootstrap;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.EnumSet;

/**
 * @author Abdullah Imal
 */
public enum ClientState {

    CONNECTING,

    AUTHENTICATING,
    RE_AUTHENTICATING,
    AUTHENTICATED,

    DISCONNECTING,
    CONNECT_FAILED,
    DISCONNECTED_UNSPECIFIED,
    DISCONNECTED_BY_CLIENT,
    DISCONNECTED_BY_SERVER,
    DISCONNECTED_TAKEN_OVER,
    DISCONNECTED_TAKE_OVER_FAILED;

    private static final @NotNull EnumSet<ClientState> DISCONNECTED = EnumSet.of(CONNECT_FAILED,
            DISCONNECTED_UNSPECIFIED, DISCONNECTED_BY_CLIENT, DISCONNECTED_BY_SERVER, DISCONNECTED_TAKEN_OVER,
            DISCONNECTED_TAKE_OVER_FAILED);

    public boolean disconnected() {
        return DISCONNECTED.contains(this);
    }

    public boolean disconnectingOrDisconnected() {
        return this == DISCONNECTING || disconnected();
    }
}
