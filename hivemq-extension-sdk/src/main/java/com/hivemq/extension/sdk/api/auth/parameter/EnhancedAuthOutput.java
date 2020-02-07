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
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.nio.ByteBuffer;
import java.time.Duration;

/**
 * Output parameter provided to the methods of an {@link EnhancedAuthenticator}.
 * <p>
 * It can be used to
 * <ul>
 *   <li>Authenticate the client successfully OR</li>
 *   <li>Fail the authentication OR</li>
 *   <li>Continue the authentication OR</li>
 *   <li>Delegate the decision to the next extension</li>
 * </ul>
 * <p>
 * Exactly one of the decisive methods must be called:
 * <ul>
 *   <li>{@link #authenticateSuccessfully()}</li>
 *   <li>{@link #authenticateSuccessfully(byte[])}</li>
 *   <li>{@link #authenticateSuccessfully(ByteBuffer)}</li>
 *   <li>{@link #failAuthentication()}</li>
 *   <li>{@link #failAuthentication(DisconnectedReasonCode)}</li>
 *   <li>{@link #failAuthentication(String)}</li>
 *   <li>{@link #failAuthentication(DisconnectedReasonCode, String)}</li>
 *   <li>{@link #continueAuthentication()}</li>
 *   <li>{@link #continueAuthentication(byte[])}</li>
 *   <li>{@link #continueAuthentication(ByteBuffer)}</li>
 *   <li>{@link #nextExtensionOrDefault()}</li>
 * </ul>
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 * <p>
 * The default topic permissions only apply if the client is authenticated successfully.
 * <p>
 * In case of a failed authentication a CONNACK packet with the appropriate reason code is sent to the client.
 * In case of a failed re-authentication a DISCONNECT packet with the appropriate reason code is sent to the client.
 *
 * @author Christoph Schäbel
 * @author Daniel Krüger
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
@DoNotImplement
public interface EnhancedAuthOutput extends AsyncOutput<EnhancedAuthOutput> {

    /**
     * Continues the authentication of the client by sending an AUTH packet to the client and expecting another AUTH
     * packet in response from the client.
     * <p>
     * Sends an AUTH packet with reason code {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#CONTINUE_AUTHENTICATION
     * CONTINUE_AUTHENTICATION} and no authentication data to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @throws UnsupportedOperationException If the client does not support enhanced authentication (it did not specify
     *                                       a authentication method in the CONNECT packet).
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void continueAuthentication();

    /**
     * Continues the authentication of the client by sending an AUTH packet to the client and expecting another AUTH
     * packet in response from the client.
     * <p>
     * Sends an AUTH packet with reason code {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#CONTINUE_AUTHENTICATION
     * CONTINUE_AUTHENTICATION} and the specified authentication data to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param authenticationData The authentication data of the AUTH packet.
     * @throws UnsupportedOperationException If the client does not support enhanced authentication (it did not specify
     *                                       a authentication method in the CONNECT packet).
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void continueAuthentication(@NotNull ByteBuffer authenticationData);

    /**
     * Continues the authentication of the client by sending an AUTH packet to the client and expecting another AUTH
     * packet in response from the client.
     * <p>
     * Sends AUTH packet with reason code {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#CONTINUE_AUTHENTICATION
     * CONTINUE_AUTHENTICATION} and the specified authentication data to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param authenticationData The authentication data of the AUTH packet.
     * @throws UnsupportedOperationException If the client does not support enhanced authentication (it did not specify
     *                                       a authentication method in the CONNECT packet).
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void continueAuthentication(@NotNull byte[] authenticationData);

    /**
     * Successfully authenticates the client.
     * <p>
     * During authentication a CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#SUCCESS
     * SUCCESS} and no authentication data is sent to the client.
     * <p>
     * During re-authentication an AUTH packet with reason code {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#SUCCESS
     * SUCCESS} and no authentication data is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void authenticateSuccessfully();

    /**
     * Successfully authenticates the client.
     * <p>
     * During authentication a CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#SUCCESS
     * SUCCESS} and the specified authentication data is sent to the client.
     * <p>
     * During re-authentication an AUTH packet with reason code {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#SUCCESS
     * SUCCESS} and the specified authentication data is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param authenticationData The authentication data of the CONNACK or AUTH packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void authenticateSuccessfully(@NotNull ByteBuffer authenticationData);

    /**
     * Successfully authenticates the client.
     * <p>
     * During authentication a CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#SUCCESS
     * SUCCESS} and the specified authentication data is sent to the client.
     * <p>
     * During re-authentication an AUTH packet with reason code {@link com.hivemq.extension.sdk.api.packets.auth.AuthReasonCode#SUCCESS
     * SUCCESS} and the specified authentication data is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param authenticationData The authentication data of the CONNACK or AUTH packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void authenticateSuccessfully(@NotNull byte[] authenticationData);

    /**
     * Fails the authentication of the client.
     * <p>
     * During authentication a CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and reason string <code>Authentication failed</code> is sent to the client.
     * <p>
     * During re-authentication a DISCONNECT packet with reason code {@link com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and reason string <code>Re-authentication failed</code> is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void failAuthentication();

    /**
     * Fails the authentication of the client.
     * <p>
     * During authentication a CONNACK packet with the specified reason code and reason string <code>Authentication
     * failed</code> is sent to the client.
     * <p>
     * During re-authentication a DISCONNECT packet with the specified reason code and reason string
     * <code>Re-authentication failed</code> is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param reasonCode The reason code of the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       during authentication.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       during re-authentication.
     */
    void failAuthentication(@NotNull DisconnectedReasonCode reasonCode);

    /**
     * Fails the authentication of the client.
     * <p>
     * During authentication a CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and the specified reason string is sent to the client.
     * <p>
     * During re-authentication a DISCONNECT packet with reason code {@link com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and the specified reason string is sent to the client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param reasonString The reason string of the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void failAuthentication(@Nullable String reasonString);

    /**
     * Fails the authentication of the client.
     * <p>
     * During authentication a CONNACK packet with the specified reason code and reason string is sent to the client.
     * <p>
     * During re-authentication a DISCONNECT packet with the specified reason code and reason string is sent to the
     * client.
     * <p>
     * This is a final decision, authenticators of the next extensions (with lower priority) are not called.
     *
     * @param reasonCode   The reason code of the CONNACK or DISCONNECT packet.
     * @param reasonString The reason string of the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       during authentication.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       during re-authentication.
     */
    void failAuthentication(@NotNull DisconnectedReasonCode reasonCode, @Nullable String reasonString);

    /**
     * The outcome of the authentication is determined by an authenticator of the next extension (with lower priority).
     * <p>
     * If no extension with an authenticator is left the default behaviour is used.
     * The default behaviour is the same as {@link #failAuthentication()}.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault has already been called.
     */
    void nextExtensionOrDefault();

    /**
     * Sets the time interval (in seconds) in which a response from the client is expected before the authentication
     * times out.
     * <p>
     * This only applies if continueAuthentication is called.
     * <p>
     * If the authentication times out a CONNACK packet with reason code {@link com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#NOT_AUTHORIZED
     * NOT_AUTHORIZED} and reason string <code>Authentication failed, timeout before the client provided required
     * authentication data</code> is sent to the client.
     * <p>
     * If the re-authentication times out a DISCONNECT packet with reason code {@link
     * com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode#NOT_AUTHORIZED NOT_AUTHORIZED} and reason string
     * <code>Re-authentication failed, timeout before the client provided required authentication data</code> is sent
     * to the client.
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
     *                        with reason code {@link DisconnectedReasonCode#NOT_AUTHORIZED NOT_AUTHORIZED} and reason
     *                        string <code>Authentication failed, authenticator timed out</code> (or
     *                        <code>Re-authentication failed, authenticator timed out</code> during re-authentication).
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
     *                        with the specified reason code and reason string <code>Authentication failed,
     *                        authenticator timed out</code> (or <code>Re-authentication failed, authenticator timed
     *                        out</code> during re-authentication).
     * @param reasonCode      The reason code sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       during authentication.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       during re-authentication.
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
     *                        with reason code {@link DisconnectedReasonCode#NOT_AUTHORIZED NOT_AUTHORIZED} and the
     *                        specified reason string.
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
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a DISCONNECT only reason code
     *                                       during authentication.
     * @throws IllegalArgumentException      when {@link DisconnectedReasonCode} is set to a CONNACK only reason code
     *                                       during re-authentication.
     */
    @NotNull Async<EnhancedAuthOutput> async(
            @NotNull Duration timeout,
            @NotNull TimeoutFallback timeoutFallback,
            @NotNull DisconnectedReasonCode reasonCode,
            @Nullable String reasonString);
}
