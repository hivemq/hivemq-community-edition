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
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.nio.ByteBuffer;
import java.time.Duration;

/**
 * Output object provided to the methods of an {@link EnhancedAuthenticator}.
 * <p>
 * It can be used to
 * <ul>
 *   <li>Authenticate a client successfully OR</li>
 *   <li>Fail the authentication OR</li>
 *   <li>Continue the authentication OR</li>
 *   <li>Delegate the decision to the next extension</li>
 * </ul>
 * <p>
 * Only one of the methods {@link #authenticateSuccessfully()}, {@link #authenticateSuccessfully(byte[])},
 * {@link #authenticateSuccessfully(ByteBuffer)}, {@link #failAuthentication()}, {@link #failAuthentication(String)},
 * {@link #failAuthentication(DisconnectedReasonCode, String)}, {@link #continueAuthentication()},
 * {@link #continueAuthentication(byte[])}, {@link #continueAuthentication(ByteBuffer)} or
 * {@link #nextExtensionOrDefault()} must be called.
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 * <p>
 * The default topic permissions only apply if the client is authenticated successfully.
 * <p>
 * In case of a failed authentication a CONNACK message with corresponding reason code is sent.
 * In case of a failed re-authentication a DISCONNECT message with corresponding reason code is sent.
 *
 * @author Christoph Schäbel
 * @author Daniel Krüger
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
@DoNotImplement
public interface EnhancedAuthOutput extends AsyncOutput<EnhancedAuthOutput> {

    /**
     * Sends an AUTH packet with reason code CONTINUE and no authentication data.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void continueAuthentication();

    /**
     * Sends an AUTH packet with reason code CONTINUE and the specified authentication data.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param authenticationData The authentication data of the AUTH packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void continueAuthentication(@NotNull ByteBuffer authenticationData);

    /**
     * Sends AUTH packet with reason code CONTINUE and specified authentication data.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param authenticationData The authentication data of the AUTH packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void continueAuthentication(@NotNull byte[] authenticationData);

    /**
     * Successfully authenticates the client.
     * <p>
     * During authentication a CONNACK packet with reason code SUCCESS and no authentication data is sent to the
     * client.
     * During re-authentication an AUTH packet with reason code SUCCESS and no authentication data is sent to the
     * client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void authenticateSuccessfully();

    /**
     * Successfully authenticates the client.
     * <p>
     * During authentication a CONNACK packet with the specified reason code and no authentication data is sent to the
     * client.
     * During re-authentication an AUTH packet with the specified reason code and no authentication data is sent to the
     * client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param authenticationData The authentication data of the CONNACK or AUTH packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void authenticateSuccessfully(@NotNull ByteBuffer authenticationData);

    /**
     * Successfully authenticates the client.
     * <p>
     * During authentication a CONNACK packet with the specified reason code and no authentication data is sent to the
     * client.
     * During re-authentication an AUTH packet with the specified reason code and no authentication data is sent to the
     * client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param authenticationData The authentication data that of the CONNACK or AUTH packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void authenticateSuccessfully(@NotNull byte[] authenticationData);

    /**
     * Fails the authentication for the client.
     * <p>
     * During authentication a CONNACK packet with reason code NOT_AUTHORIZED and reason string <code>Authentication
     * failed by extension</code> is sent to the client.
     * During re-authentication a DISCONNECT packet with reason code NOT_AUTHORIZED and reason string
     * <code>Authentication failed by extension</code> is sent to the client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void failAuthentication();

    /**
     * Fails the authentication for the client.
     * <p>
     * During authentication a CONNACK packet with the specified reason code and reason string <code>Authentication
     * failed by extension</code> is sent to the client.
     * During re-authentication a DISCONNECT packet with the specified reason code and reason string
     * <code>Authentication failed by extension</code> is sent to the client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param reasonCode The reason code of the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       and the client is already connected.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       and the client is currently connecting.
     */
    void failAuthentication(@NotNull DisconnectedReasonCode reasonCode);

    /**
     * Fails the authentication for the client.
     * <p>
     * During authentication a CONNACK packet with reason code NOT_AUTHORIZED and the specified reason string is sent to
     * the client.
     * During re-authentication a DISCONNECT packet with reason code NOT_AUTHORIZED and the specified reason string is
     * sent to the client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param reasonString The reason string of the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void failAuthentication(@Nullable String reasonString);

    /**
     * Fails the authentication for the client.
     * <p>
     * During authentication a CONNACK packet with the specified reason code and reason string is sent to the
     * client.
     * During re-authentication a DISCONNECT packet with the specified reason code and reason string is sent to the
     * client.
     * <p>
     * This is a final decision, the next extensions are ignored.
     *
     * @param reasonCode   The reason code of the CONNACK or DISCONNECT packet.
     * @param reasonString The reason string of the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       and the client is already connected.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       and the client is currently connecting.
     */
    void failAuthentication(@NotNull DisconnectedReasonCode reasonCode, @Nullable String reasonString);

    /**
     * The outcome of the authentication is determined by the next extension with an {@link Authenticator}.
     * <p>
     * If no extension with an Authenticator is left the default behaviour is used.
     * The default behaviour is the same as {@link #failAuthentication()}.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void nextExtensionOrDefault();

    /**
     * Sets the timeout in seconds for client auth handling. This only applies if continueAuthentication is called.
     *
     * @param timeout The timeout in seconds.
     */
    void setTimeout(int timeout);

    /**
     * Provides {@link ModifiableUserProperties} to add or remove user properties to or from the outgoing CONNACK, AUTH
     * or DISCONNECT packet.
     *
     * @return The {@link ModifiableUserProperties} of the CONNACK, AUTH or DISCONNECT packet.
     */
    @NotNull ModifiableUserProperties getOutboundUserProperties();

    /**
     * Provides {@link ModifiableDefaultPermissions} to configure client specific default permissions.
     * <p>
     * Default permissions are automatically applied by HiveMQ for every PUBLISH and SUBSCRIBE packet sent by the
     * client.
     *
     * @return The {@link ModifiableDefaultPermissions} for the client.
     */
    @NotNull ModifiableDefaultPermissions getDefaultPermissions();

    /**
     * Provides {@link ModifiableClientSettings} to configure client specific parameters and restrictions.
     *
     * @return The {@link ModifiableClientSettings} for the client.
     */
    @NotNull ModifiableClientSettings getClientSettings();

    /**
     * {@inheritDoc}
     *
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(DisconnectedReasonCode, String)}
     *                        with reason code NOT_AUTHORIZED and reason string <code>Authentication failed by
     *                        timeout</code>.
     */
    @Override
    @NotNull Async<EnhancedAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(DisconnectedReasonCode, String)}
     *                        with the specified reason code and reason string <code>Authentication failed by
     *                        timeout</code>.
     * @param reasonCode      The reason code sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       and the client is already connected.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       and the client is currently connecting.
     */
    @NotNull Async<EnhancedAuthOutput> async(
            @NotNull Duration timeout,
            @NotNull TimeoutFallback timeoutFallback,
            @NotNull DisconnectedReasonCode reasonCode);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        SUCCESS has the same effect as {@link #nextExtensionOrDefault()}.
     *                        FAILURE has the same effect as {@link #failAuthentication(DisconnectedReasonCode, String)}
     *                        with reason code NOT_AUTHORIZED and the specified reason string.
     * @param reasonString    The reason string sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     */
    @NotNull Async<EnhancedAuthOutput> async(
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
     *                        FAILURE has the same effect as {@link #failAuthentication(DisconnectedReasonCode, String)}
     *                        with the specified reason code and reason string.
     * @param reasonCode      The reason code sent in CONNACK or DISCONNECT when timeout occurs.
     * @param reasonString    The reason string sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       and the client is already connected.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       and the client is currently connecting.
     */
    @NotNull Async<EnhancedAuthOutput> async(
            @NotNull Duration timeout,
            @NotNull TimeoutFallback timeoutFallback,
            @NotNull DisconnectedReasonCode reasonCode,
            @Nullable String reasonString);
}
