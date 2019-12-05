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
package com.hivemq.extension.sdk.api.services.auth.provider;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;

/**
 * The authenticator provider allows to implement custom logic for the authentication of connecting MQTT clients.
 * For each client an {@link Authenticator} can be provided that contains the authentication logic.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@FunctionalInterface
public interface AuthenticatorProvider {

    /**
     * This method is called for each client by HiveMQ.
     * <p>
     * Either the same {@link Authenticator} (stateless or must be thread-safe)<br/>
     * or a new one (stateful, must not be thread-safe) can be supplied on each call.
     * <p>
     * <code>null</code> can be returned if no authentication for the client is necessary.
     *
     * @return An implementation of {@link SimpleAuthenticator}.
     * {@code null} is ignored and has the same effect as if this provider would had not been set
     * for the connecting client. Returning any other implementation of the
     * {@link Authenticator} interface is an error.
     * @since 4.0.0
     */
    @Nullable Authenticator getAuthenticator(@NotNull AuthenticatorProviderInput authenticatorProviderInput);
}
