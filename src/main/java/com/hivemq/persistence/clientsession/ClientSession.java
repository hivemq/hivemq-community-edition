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

package com.hivemq.persistence.clientsession;

import com.google.common.base.Preconditions;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;

/**
 * @author Lukas Brandl
 */
public class ClientSession {

    private boolean connected;

    private long sessionExpiryInterval;

    private ClientSessionWill willPublish;

    public ClientSession(final boolean connected,
                         final long sessionExpiryInterval) {

        Preconditions.checkArgument(sessionExpiryInterval >= SESSION_EXPIRE_ON_DISCONNECT, "Session expiry interval must never be less than zero");

        this.connected = connected;
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public ClientSession(final boolean connected, final long sessionExpiryInterval, final ClientSessionWill willPublish) {
        this.connected = connected;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.willPublish = willPublish;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(final boolean connected) {
        this.connected = connected;
    }

    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(final long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public ClientSessionWill getWillPublish() {
        return willPublish;
    }

    public void setWillPublish(final ClientSessionWill willPublish) {
        this.willPublish = willPublish;
    }
}
