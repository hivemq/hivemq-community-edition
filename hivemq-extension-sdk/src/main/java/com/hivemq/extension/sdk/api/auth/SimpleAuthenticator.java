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

package com.hivemq.extension.sdk.api.auth;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;

/**
 * Interface for the simple authentication of MQTT clients.
 * <p>
 * Simple authentication only uses CONNECT and CONNACK but no AUTH packets. This means:
 * <ul>
 *   <li>it does not support challenge/response style authentication</li>
 *   <li>it does not support re-authentication</li>
 * </ul>
 * <p>
 * An SimpleAuthenticator can be provided by an {@link com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider AuthenticatorProvider}.
 * <p>
 * If an implementation stores state, an object of the implementation can not be shared by different clients.<br/>
 * If no state is stored, the implementation has to be thread safe if it is shared by different clients.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@FunctionalInterface
public interface SimpleAuthenticator extends Authenticator {

    /**
     * This method is called when the MQTT client (that has to be authenticated) sent the CONNECT packet.
     *
     * @param simpleAuthInput  The {@link SimpleAuthInput}.
     * @param simpleAuthOutput The {@link SimpleAuthOutput}.
     * @since 4.0.0
     */
    void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput);
}
