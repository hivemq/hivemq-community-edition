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
 * Interface for the enhanced authentication with AUTH packets introduced in MQTT 5. CONNECT, CONNACK and AUTH packets
 * are used here.
 * <p>
 * If an implementation stores state, an object of the implementation can not be shared by different clients.<br/>
 * If no state is stored, it has to be thread safe that it can be shared.
 * <p>
 *
 * @author Christoph Schäbel
 * @author Daniel Krüger
 * @author Florian Limpöck
*/
public interface EnhancedAuthenticator {

    /**
     * This method is called for CONNECT packet, that the {@link EnhancedAuthenticator} is delegated to authenticate.
     *
     * @param connectEnhancedAuthInput The {@link ConnectEnhancedAuthInput}.
     * @param enhancedAuthOutput       The {@link EnhancedAuthOutput}.
    */
    void onConnect(@NotNull ConnectEnhancedAuthInput connectEnhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput);

    /**
     * This method is called for AUTH packet, that the {@link EnhancedAuthenticator} is delegated to authenticate.
     *
     * @param enhancedAuthInput  The {@link EnhancedAuthInput}.
     * @param enhancedAuthOutput The {@link EnhancedAuthOutput}.
    */
    void onAuth(@NotNull EnhancedAuthInput enhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput);

    /**
     * This method is called for AUTH packet, that the {@link EnhancedAuthenticator} is delegated to authenticate in
     * case of a re-authentication.
     *
     * @param enhancedAuthInput  The {@link EnhancedAuthInput}.
     * @param enhancedAuthOutput The {@link EnhancedAuthOutput}.
    */
    default void onReAuth(final @NotNull EnhancedAuthInput enhancedAuthInput, final @NotNull EnhancedAuthOutput enhancedAuthOutput) {
        onAuth(enhancedAuthInput, enhancedAuthOutput);
    }

}
