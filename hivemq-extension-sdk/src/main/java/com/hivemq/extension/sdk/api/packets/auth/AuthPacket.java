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

package com.hivemq.extension.sdk.api.packets.auth;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Florian Limpöck
*/
@Immutable
@DoNotImplement
public interface AuthPacket {

    /**
     * @return the authentication method from the AUTH packet.
    */
    @NotNull
    String getAuthenticationMethod();

    /**
     * The authentication data from the AUTH packet.
     * <p>
     *
     * @return An {@link Optional} containing the authentication data if present.
    */
    @NotNull
    Optional<ByteBuffer> getAuthenticationData();

    /**
     * The authentication data from the AUTH packet.
     * <p>
     *
     * @return An {@link Optional} containing the authentication data if present.
    */
    @NotNull
    Optional<byte[]> getAuthenticationDataAsBytes();

    /**
     * The reason code from the AUTH packet.
     * <p>
     *
     * @return The AUTH reason code.
    */
    @NotNull
    AuthReasonCode getReasonCode();

    /**
     * The reason string of the AUTH packet.
     * <p>
     *
     * @return An {@link Optional} containing the AUTH reason string if present.
    */
    @NotNull
    Optional<String> getReasonString();

    /**
     * The user properties from the AUTH packet.
     * <p>
     *
     * @return The user properties.
    */
    @NotNull
    UserProperties getUserProperties();

}
