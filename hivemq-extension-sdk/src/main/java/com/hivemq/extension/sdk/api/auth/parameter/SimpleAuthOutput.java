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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.time.Duration;

/**
 * Output parameter provided to {@link SimpleAuthenticator#onConnect(SimpleAuthInput, SimpleAuthOutput)}.
 * <p>
 * It can be used to
 * <ul>
 *   <li>Authenticate the client successfully OR</li>
 *   <li>Fail authentication OR</li>
 *   <li>Delegate the decision to the next extension</li>
 * </ul>
 * <p>
 * Exactly one of the decisive methods must be called:
 * <ul>
 *   <li>{@link #authenticateSuccessfully()}</li>
 *   <li>{@link #failAuthentication()}</li>
 *   <li>{@link #failAuthentication(ConnackReasonCode)}</li>
 *   <li>{@link #failAuthentication(String)}</li>
 *   <li>{@link #failAuthentication(ConnackReasonCode, String)}</li>
 *   <li>{@link #nextExtensionOrDefault()}</li>
 * </ul>
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 * <p>
 * The default topic permissions only apply if the client is authenticated successfully.
 * <p>
 * In case of a failed authentication a CONNACK packet with the appropriate reason code is sent to the client.
 *
 * @author Christoph Schäbel
 * @author Silvio Giebl
 * @since 4.0.0
 */
@DoNotImplement
public interface SimpleAuthOutput extends AsyncOutput<SimpleAuthOutput> {

    /**
     * Successfully authenticates the client.
     * <p>
     * A CONNACK packet with reason code {@link ConnackReasonCode#SUCCESS SUCCESS} is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void authenticateSuccessfully();

    /**
     * Fails the authentication of the client.
     * <p>
     * A CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and reason string <code>Authentication failed</code> is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void failAuthentication();

    /**
     * Fails the authentication of the client.
     * <p>
     * A CONNACK packet with the specified reason code and reason string <code>Authentication failed</code> is sent to
     * the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param reasonCode The reason code of the CONNACK packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     */
    void failAuthentication(@NotNull ConnackReasonCode reasonCode);

    /**
     * Fails the authentication of the client.
     * <p>
     * A CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and the specified reason string is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param reasonString Used as the reason string in the CONNACK packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void failAuthentication(@Nullable String reasonString);

    /**
     * Fails the authentication of the client.
     * <p>
     * A CONNACK packet with the specified reason code and reason string is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param reasonCode   Used as the reason code in the CONNACK packet.
     * @param reasonString Used as the reason string in the CONNACK packet.
     * @throws IllegalArgumentException      If the reasonCode is SUCCESS.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void failAuthentication(@NotNull ConnackReasonCode reasonCode, @Nullable String reasonString);

    /**
     * The outcome of the authentication is determined by an authenticator of the next extension (with lower priority).
     * <p>
     * If no extension with an authenticator is left the default behaviour is used.
     * The default behaviour is the same as {@link #failAuthentication()}.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void nextExtensionOrDefault();

    /**
     * Provides {@link ModifiableUserProperties} to add or remove user properties to or from the outgoing CONNACK
     * packet.
     *
     * @return The {@link ModifiableUserProperties} of the CONNACK packet.
     * @since 4.0.0
     */
    @NotNull ModifiableUserProperties getOutboundUserProperties();

    /**
     * Provides {@link ModifiableDefaultPermissions} to configure client specific default permissions.
     * <p>
     * Default permissions are automatically applied by HiveMQ for every PUBLISH and SUBSCRIBE packet sent by the
     * client.
     *
     * @return The {@link ModifiableDefaultPermissions} for the client.
     * @since 4.0.0
     */
    @NotNull ModifiableDefaultPermissions getDefaultPermissions();

    /**
     * Provides {@link ModifiableClientSettings} to configure client specific parameters and restrictions.
     *
     * @return The {@link ModifiableClientSettings} for the client.
     * @since 4.2.0
     */
    @NotNull ModifiableClientSettings getClientSettings();

    /**
     * {@inheritDoc}
     *
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(ConnackReasonCode, String)} with
     *                        reason code {@link ConnackReasonCode#NOT_AUTHORIZED NOT_AUTHORIZED} and reason string
     *                        <code>Authentication failed, authenticator timed out</code>.
     * @since 4.0.0
     */
    @Override
    @NotNull Async<SimpleAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(ConnackReasonCode, String)} with
     *                        the specified reason code and reason string <code>Authentication failed, authenticator
     *                        timed out</code>.
     * @param reasonCode      The reason code sent in CONNACK when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<SimpleAuthOutput> async(
            @NotNull Duration timeout,
            @NotNull TimeoutFallback timeoutFallback,
            @NotNull ConnackReasonCode reasonCode);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(ConnackReasonCode, String)}
     *                        with reason code {@link ConnackReasonCode#NOT_AUTHORIZED NOT_AUTHORIZED} and the specified
     *                        reason string.
     * @param reasonString    The reason string sent in CONNACK when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<SimpleAuthOutput> async(
            @NotNull Duration timeout,
            @NotNull TimeoutFallback timeoutFallback,
            @Nullable String reasonString);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(ConnackReasonCode, String)}
     *                        with the specified reason code and reason string.
     * @param reasonCode      The reason code sent in CONNACK when timeout occurs.
     * @param reasonString    The reason string sent in CONNACK when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<SimpleAuthOutput> async(
            @NotNull Duration timeout,
            @NotNull TimeoutFallback timeoutFallback,
            @NotNull ConnackReasonCode reasonCode,
            @Nullable String reasonString);
}
