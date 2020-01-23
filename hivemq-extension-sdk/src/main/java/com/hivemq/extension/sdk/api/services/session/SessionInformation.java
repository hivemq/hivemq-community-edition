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
package com.hivemq.extension.sdk.api.services.session;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * Information about the session of an MQTT client.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface SessionInformation {

    /**
     * @return The clients unique identifier.
     * @since 4.0.0
     */
    @NotNull String getClientIdentifier();

    /**
     * The session expiry interval, when the session information of the client will be deleted after the client
     * disconnected.
     *
     * @return The session expiry interval in seconds.
     * @since 4.0.0
     */
    long getSessionExpiryInterval();

    /**
     * @return <b>true</b> if the client is still connected, else <b>false</b>.
     * @since 4.0.0
     */
    boolean isConnected();
}
