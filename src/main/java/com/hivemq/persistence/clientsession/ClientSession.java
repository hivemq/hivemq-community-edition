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
package com.hivemq.persistence.clientsession;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.Sizable;
import com.hivemq.util.ObjectMemoryEstimation;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;

/**
 * @author Lukas Brandl
 */
public class ClientSession implements Sizable {

    private boolean connected;
    private long sessionExpiryInterval;
    private int inMemorySize = SIZE_NOT_CALCULATED;
    private final @Nullable Long queueLimit;
    private @Nullable ClientSessionWill willPublish;

    public ClientSession(final boolean connected, final long sessionExpiryInterval) {
        this(connected, sessionExpiryInterval, null, null);
    }

    public ClientSession(final boolean connected,
                         final long sessionExpiryInterval,
                         final @Nullable ClientSessionWill willPublish,
                         final @Nullable Long queueLimit) {

        Preconditions.checkArgument(
                sessionExpiryInterval >= SESSION_EXPIRE_ON_DISCONNECT,
                "Session expiry interval must never be less than zero");

        this.connected = connected;
        this.sessionExpiryInterval = sessionExpiryInterval;
        this.willPublish = willPublish;
        this.queueLimit = queueLimit;
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

    @Nullable
    public ClientSessionWill getWillPublish() {
        return willPublish;
    }

    public void setWillPublish(final @Nullable ClientSessionWill willPublish) {
        this.willPublish = willPublish;
    }

    public @Nullable Long getQueueLimit() {
        return queueLimit;
    }

    public @NotNull ClientSession deepCopyWithoutPayload() {
        return new ClientSession(
                this.connected,
                this.sessionExpiryInterval,
                this.willPublish != null ? this.willPublish.deepCopyWithoutPayload() : null,
                this.queueLimit);
    }

    public @NotNull ClientSession copyWithoutWill() {
        return new ClientSession(this.connected, this.sessionExpiryInterval, null, this.queueLimit);
    }

    @Override
    public int getEstimatedSize() {

        if (inMemorySize != SIZE_NOT_CALCULATED) {
            return inMemorySize;
        }

        int size = ObjectMemoryEstimation.objectShellSize();
        size += ObjectMemoryEstimation.intSize(); // inMemorySize
        size += ObjectMemoryEstimation.booleanSize(); // connected
        size += ObjectMemoryEstimation.longSize(); // sessionExpiryInterval

        size += ObjectMemoryEstimation.objectRefSize(); // reference to will
        if (willPublish != null) {
            size += willPublish.getEstimatedSize();
        }
        if(queueLimit != null){
            size += ObjectMemoryEstimation.longSize();
        }

        inMemorySize = size;

        return inMemorySize;
    }
}
