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
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;

/**
 * The Security Registry allows extensions to define the authentication and authorization of MQTT clients.
 * <p>
 * It can be accessed by {@link Services#securityRegistry()}.
 * <p>
 * An extension can only set at most one {@link AuthenticatorProvider} OR one {@link EnhancedAuthenticatorProvider}.
 * <p>
 * An extension can also only set at most one {@link AuthorizerProvider}.
 * That means for authorizing PUBLISH and SUBSCRIBE packets the {@link AuthorizerProvider} must implement {@link
 * PublishAuthorizer} and {@link SubscriptionAuthorizer}.
 * <p>
 * The providers are automatically removed at extension stop.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface SecurityRegistry {

    /**
     * Sets the {@link AuthenticatorProvider} of this extension.
     * <p>
     * At most one {@link AuthenticatorProvider} OR one {@link EnhancedAuthenticatorProvider} can be set.
     * <p>
     * A previously set {@link AuthenticatorProvider} or {@link EnhancedAuthenticatorProvider} is replaced.
     *
     * @param authenticatorProvider The {@link AuthenticatorProvider} to set.
     * @since 4.0.0
     */
    void setAuthenticatorProvider(@NotNull AuthenticatorProvider authenticatorProvider);

    /**
     * Sets the {@link EnhancedAuthenticatorProvider} of this extension.
     * <p>
     * At most one {@link AuthenticatorProvider} OR one {@link EnhancedAuthenticatorProvider} can be set.
     * <p>
     * A previously set {@link AuthenticatorProvider} or {@link EnhancedAuthenticatorProvider} is replaced.
     *
     * @param authenticatorProvider The {@link EnhancedAuthenticatorProvider} to set.
     */
    void setEnhancedAuthenticatorProvider(@NotNull EnhancedAuthenticatorProvider authenticatorProvider);

    /**
     * Sets the {@link AuthorizerProvider} of this extension.
     * <p>
     * At most one {@link AuthorizerProvider} can be set.
     * <p>
     * A previously set {@link AuthorizerProvider} is replaced.
     *
     * @param authorizerProvider The {@link AuthorizerProvider} to set.
     * @since 4.0.0
     */
    void setAuthorizerProvider(@NotNull AuthorizerProvider authorizerProvider);
}
