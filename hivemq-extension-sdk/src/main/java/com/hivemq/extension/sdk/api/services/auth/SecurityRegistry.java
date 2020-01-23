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
package com.hivemq.extension.sdk.api.services.auth;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;

/**
 * The SecurityRegistry allows extensions to define the authentication and authorization of MQTT clients.
 * <p>
 * It can be accessed by {@link Services#securityRegistry()}.
 * <p>
 * An extension can only set one {@link AuthenticatorProvider} and one {@link AuthorizerProvider}.
 * <p>
 * That means for authorizing PUBLISH and SUBSCRIBE packets the {@link AuthorizerProvider} must implement {@link
 * PublishAuthorizer} and {@link SubscriptionAuthorizer}.
 * <p>
 * The providers are removed at extension stop automatically.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface SecurityRegistry {

    /**
     * @param authenticatorProvider The {@link AuthenticatorProvider} to set.
     * @since 4.0.0
     */
    void setAuthenticatorProvider(@NotNull AuthenticatorProvider authenticatorProvider);

    /**
     * @param authorizerProvider The {@link AuthorizerProvider} to set.
     * @since 4.0.0
     */
    void setAuthorizerProvider(@NotNull AuthorizerProvider authorizerProvider);
}
