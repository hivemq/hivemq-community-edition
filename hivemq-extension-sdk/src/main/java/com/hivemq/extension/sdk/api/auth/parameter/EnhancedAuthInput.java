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

package com.hivemq.extension.sdk.api.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.packets.auth.AuthPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * Input parameter provided to the methods of an {@link EnhancedAuthenticator}.
 * <p>
 * Provides an unmodifiable {@link AuthPacket} and {@link ClientBasedInput}.
 *
 * @author Daniel Krüger
 * @author Florian Limpöck
 */
@DoNotImplement
public interface EnhancedAuthInput extends ClientBasedInput {

    /**
     * Provides the unmodifiable AUTH packet sent by the MQTT client that has to be authenticated.
     *
     * @return The {@link AuthPacket} sent by the client.
     */
    @Immutable @NotNull AuthPacket getAuthPacket();

    /**
     * @return <code>true</code> if this is an input of a re-authentication, else <code>false</code>.
     */
    boolean isReAuthentication();
}
