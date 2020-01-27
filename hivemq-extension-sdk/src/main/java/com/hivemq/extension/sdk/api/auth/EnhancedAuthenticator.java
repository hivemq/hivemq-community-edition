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
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthConnectInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthOutput;

/**
 * Interface for the enhanced authentication of MQTT clients.
 * <p>
 * Enhanced authentication can use AUTH packets (introduced in MQTT 5) to implement:
 * <ul>
 *   <li>challenge/response style authentication and</li>
 *   <li>re-authentication</li>
 * </ul>
 * <p>
 * Enhanced authentication has two life cycles:
 * <ul>
 *   <li>When a client connects
 *     <ol>
 *       <li>{@link #onConnect(EnhancedAuthConnectInput, EnhancedAuthOutput)}</li>
 *       <li>({@link #onAuth(EnhancedAuthInput, EnhancedAuthOutput)})*</li>
 *     </ol>
 *   </li>
 *   <li>When a client triggers re-authentication
 *     <ol>
 *       <li>{@link #onReAuth(EnhancedAuthInput, EnhancedAuthOutput)}</li>
 *       <li>({@link #onAuth(EnhancedAuthInput, EnhancedAuthOutput)})*</li>
 *     </ol>
 *   </li>
 * </ul>
 * <p>
 * An EnhancedAuthenticator can be provided by an {@link com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider EnhancedAuthenticatorProvider}.
 * The provider is only called once per client connection, enabling the EnhancedAuthenticator to store state between the
 * initial authentication and later re-authentication(s).
 * <p>
 * If an implementation stores state, an object of the implementation can not be shared by different clients.<br/>
 * If no state is stored, the implementation has to be thread safe if it is shared by different clients.
 *
 * @author Christoph Schäbel
 * @author Daniel Krüger
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public interface EnhancedAuthenticator {

    /**
     * This method is called when the MQTT client (that has to be authenticated) sent the CONNECT packet.
     *
     * @param enhancedAuthConnectInput The {@link EnhancedAuthConnectInput}.
     * @param enhancedAuthOutput       The {@link EnhancedAuthOutput}.
     */
    void onConnect(
            @NotNull EnhancedAuthConnectInput enhancedAuthConnectInput,
            @NotNull EnhancedAuthOutput enhancedAuthOutput);

    /**
     * This method is called when the MQTT client (that has to be authenticated) sent an AUTH packet with reason code
     * {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#REAUTHENTICATE REAUTHENTICATE}.
     *
     * @param enhancedAuthInput  The {@link EnhancedAuthInput}.
     * @param enhancedAuthOutput The {@link EnhancedAuthOutput}.
     */
    default void onReAuth(
            final @NotNull EnhancedAuthInput enhancedAuthInput,
            final @NotNull EnhancedAuthOutput enhancedAuthOutput) {

        onAuth(enhancedAuthInput, enhancedAuthOutput);
    }

    /**
     * This method is called when the MQTT client (that has to be authenticated) sent an AUTH packet.
     *
     * @param enhancedAuthInput  The {@link EnhancedAuthInput}.
     * @param enhancedAuthOutput The {@link EnhancedAuthOutput}.
     */
    void onAuth(@NotNull EnhancedAuthInput enhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput);
}
