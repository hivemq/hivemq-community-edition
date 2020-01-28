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
import com.hivemq.extension.sdk.api.auth.parameter.ConnectEnhancedAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthOutput;

/**
 * Interface for the enhanced authentication with AUTH packets introduced in MQTT 5.
 * <p>
 * Enhanced authentication has two life cycles:
 * <ul>
 *   <li>When a client connects
 *     <ol>
 *       <li>{@link #onConnect(ConnectEnhancedAuthInput, EnhancedAuthOutput)}</li>
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
 * If an implementation stores state, an object of the implementation can not be shared by different clients.
 * If no state is stored, it has to be thread safe that it can be shared.
 *
 * @author Christoph Schäbel
 * @author Daniel Krüger
 * @author Florian Limpöck
 */
public interface EnhancedAuthenticator {

    /**
     * This method is called when the client sent the CONNECT packet, that the {@link EnhancedAuthenticator} is
     * delegated to authenticate.
     *
     * @param connectEnhancedAuthInput The {@link ConnectEnhancedAuthInput}.
     * @param enhancedAuthOutput       The {@link EnhancedAuthOutput}.
     * @since 4.3.0
     */
    void onConnect(
            @NotNull ConnectEnhancedAuthInput connectEnhancedAuthInput,
            @NotNull EnhancedAuthOutput enhancedAuthOutput);

    /**
     * This method is called when the client sent an AUTH packet for re-authentication, that the {@link
     * EnhancedAuthenticator} is delegated to authenticate.
     *
     * @param enhancedAuthInput  The {@link EnhancedAuthInput}.
     * @param enhancedAuthOutput The {@link EnhancedAuthOutput}.
     * @since 4.3.0
     */
    default void onReAuth(
            final @NotNull EnhancedAuthInput enhancedAuthInput,
            final @NotNull EnhancedAuthOutput enhancedAuthOutput) {

        onAuth(enhancedAuthInput, enhancedAuthOutput);
    }

    /**
     * This method is called when the client sent an AUTH packet, that the {@link EnhancedAuthenticator} is delegated to
     * authenticate.
     *
     * @param enhancedAuthInput  The {@link EnhancedAuthInput}.
     * @param enhancedAuthOutput The {@link EnhancedAuthOutput}.
     * @since 4.3.0
     */
    void onAuth(@NotNull EnhancedAuthInput enhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput);
}
