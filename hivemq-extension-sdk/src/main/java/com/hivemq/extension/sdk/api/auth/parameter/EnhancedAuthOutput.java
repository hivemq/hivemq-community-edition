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
 * Output object provided to the methods of a {@link EnhancedAuthenticator}.
 * <p>
 * It can be used to
 * <ul>
 *   <li>Authenticate the client successfully OR fail authentication OR delegate the decision to the next extension</li>
 *   <li>Alter the user properties sent to the client as part of the CONNACK packet</li>
 *   <li>Alter the default topic permissions which apply only if the client is authenticated successfully</li>
 * </ul>
 * <p>
 * Only one of the methods {@link #authenticateSuccessfully()}, {@link #failAuthentication()}, {@link
 * #failAuthentication(String)}, {@link #failAuthentication(DisconnectedReasonCode, String)} or {@link
 * #nextExtensionOrDefault()} may be called.
 * <p>
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 * <p>
 * In case of a failed enhanced authentication a CONNACK message with corresponding reason code is sent.
 * In case of a failed re-authentication process a DISCONNECT message with corresponding reason code is sent.
 *
 * @author Christoph Schäbel
 * @author Daniel Krüger
 * @author Florian Limpöck
*/
@DoNotImplement
public interface EnhancedAuthOutput extends AsyncOutput<EnhancedAuthOutput> {

    /**
     * A {@link ModifiableUserProperties} to add or remove user properties to or from the outgoing CONNACK / DISCONNECT
     * packet.
     *
     * @return The {@link ModifiableUserProperties} object.
    */
    @NotNull
    ModifiableUserProperties getOutboundUserProperties();

    /**
     * Provides a {@link ModifiableClientSettings} object, that is used to configure client specific parameters and restrictions.
     *
     * @return A {@link ModifiableClientSettings} object.
    */
    @NotNull ModifiableClientSettings getClientSettings();

    /**
     * Sends AUTH packet with reason code CONTINUE and no authentication data
     * <p>
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void continueAuthentication();

    /**
     * Sends AUTH packet with reason code CONTINUE and specified authentication data
     * <p>
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void continueAuthentication(@NotNull ByteBuffer authenticationData);

    /**
     * Sends AUTH packet with reason code CONTINUE and specified authentication data
     * <p>
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void continueAuthentication(@NotNull byte[] authenticationData);

    /**
     * Successfully authenticates the client by sending an AUTH with reason code SUCCESS and no authentication data.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void authenticateSuccessfully();

    /**
     * Successfully authenticates the client by sending an AUTH with reason code SUCCESS and the given authentication
     * data.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param authenticationData the authentication data that are sent to the client.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void authenticateSuccessfully(@NotNull ByteBuffer authenticationData);

    /**
     * Successfully authenticates the client by sending an AUTH with reason code SUCCESS and the given authentication
     * data.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param authenticationData the authentication data that are sent to the client.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void authenticateSuccessfully(@NotNull byte[] authenticationData);

    /**
     * Fails the authentication for the client.
     * <p>
     * A CONNACK or DISCONNECT with reason code NOT_AUTHORIZED is sent.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void failAuthentication();

    /**
     * Fails the authentication for the client.
     * <p>
     * A CONNACK or DISCONNECT with reason code NOT_AUTHORIZED is sent.
     *
     * @param reasonString Used as the reason string in the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void failAuthentication(@NotNull String reasonString);

    /**
     * Fails the authentication for the client.
     * <p>
     *
     * @param disconnectedReasonCode Used as the reason code in the CONNACK or DISCONNECT packet.
     * @param reasonString           Used as the reason string in the CONNACK or DISCONNECT packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
     * @throws IllegalArgumentException when {@link DisconnectedReasonCode} is set to CONNACK only reason code and the client is already connected.
     * @throws IllegalArgumentException when {@link DisconnectedReasonCode} is set to DISCONNECT only reason code and the client is currently connecting.
    */
    void failAuthentication(@NotNull DisconnectedReasonCode disconnectedReasonCode, @NotNull String reasonString);

    /**
     * The outcome of the authentication is determined by the next extension with an {@link Authenticator}.
     * If no extension with an Authenticator is left the default behaviour is used. <p>
     * <p>
     * The default behaviour is to sent a CONNACK or DISCONNECT with NOT_AUTHORIZED and reason string <b>'authentication
     * failed by extension'</b>.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication, continueAuthentication
     *                                       or nextExtensionOrDefault
     *                                       has already been called.
    */
    void nextExtensionOrDefault();

    /**
     * set timeout in seconds for client auth handling. This only applies if continueAuthentication is called.
     *
    */
    void setTimeout(int timeout);

    /**
     * The default permissions for the client. Default permissions are automatically applied by HiveMQ for every
     * MQTT PUBLISH and SUBSCRIBE packet sent by the client.
     *
     * @return The {@link ModifiableDefaultPermissions} object for the client.
    */
    @NotNull ModifiableDefaultPermissions getDefaultPermissions();


    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then next extension or default is called.
     *                        If the fallback is FAILURE then the client is disconnected.
     * @param reasonCode      The reason code sent in CONNACK or DISCONNECT when timeout occurs.
     * @param reasonString    The reason string sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @throws IllegalArgumentException when {@link DisconnectedReasonCode} is set to CONNACK only reason code and the client is already connected.
     * @throws IllegalArgumentException when {@link DisconnectedReasonCode} is set to DISCONNECT only reason code and the client is currently connecting.
    */
    @NotNull
    Async<EnhancedAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                    @NotNull DisconnectedReasonCode reasonCode, @NotNull String reasonString);


    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then next extension or default is called.
     *                        If the fallback is FAILURE then the client is disconnected.
     * @param reasonCode      The reason code sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @throws IllegalArgumentException when {@link DisconnectedReasonCode} is set to CONNACK only reason code and the client is already connected.
     * @throws IllegalArgumentException when {@link DisconnectedReasonCode} is set to DISCONNECT only reason code and the client is currently connecting.
    */
    @NotNull
    Async<EnhancedAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                    @NotNull DisconnectedReasonCode reasonCode);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then next extension or default is called.
     *                        If the fallback is FAILURE then the client is disconnected.
     * @param reasonString    The reason string sent in CONNACK or DISCONNECT when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
    */
    @NotNull
    Async<EnhancedAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                    @NotNull String reasonString);

}
