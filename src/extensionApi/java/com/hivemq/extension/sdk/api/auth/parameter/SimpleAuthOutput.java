/*
 * Copyright 2018 dc-square GmbH
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
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.time.Duration;

/**
 * Output object provided to the methods of a {@link SimpleAuthenticator}.
 * <p>
 * It can be used to
 * <ul>
 * <li>Authenticate the client successfully OR fail authentication OR delegate the decision to the next extension</li>
 * <li>Alter the user properties sent to the client as part of the CONNACK packet</li>
 * <li>Alter the default topic permissions which apply only if the client is authenticated successfully</li>
 * <li>Configure client specific parameters and restrictions</li>
 * </ul>
 * <p>
 * Only one of the methods {@link #authenticateSuccessfully()}, {@link #failAuthentication()}, {@link
 * #failAuthentication(String)}, {@link #failAuthentication(ConnackReasonCode, String)} or {@link
 * #nextExtensionOrDefault()} may be called.
 * <p>
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface SimpleAuthOutput extends AsyncOutput<SimpleAuthOutput> {

    /**
     * Successfully authenticates the client.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void authenticateSuccessfully();

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then the client is successfully authenticated.
     *                        If the fallback is FAILURE then the client is disconnected.
     * @param reasonCode      The reason code sent in CONNACK when timeout occurs.
     * @param reasonString    The reason string sent in CONNACK when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<SimpleAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                           @NotNull ConnackReasonCode reasonCode, @NotNull String reasonString);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then the client is successfully authenticated.
     *                        If the fallback is FAILURE then the client is disconnected.
     * @param reasonCode      The reason code sent in CONNACK when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<SimpleAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                           @NotNull ConnackReasonCode reasonCode);

    /**
     * If the timeout is expired before {@link Async#resume()} is called then the outcome is
     * handled either as failed or successful, depending on the specified fallback.
     * <p>
     * Do not call this method more than once. If an async method is called multiple times an exception is thrown.
     *
     * @param timeout         Timeout that HiveMQ waits for the result of the async operation.
     * @param timeoutFallback Fallback behaviour if a timeout occurs.
     *                        If the fallback is SUCCESS then the client is successfully authenticated.
     *                        If the fallback is FAILURE then the client is disconnected.
     * @param reasonString    The reason string sent in CONNACK when timeout occurs.
     * @throws UnsupportedOperationException If async is called more than once.
     * @since 4.0.0
     */
    @NotNull Async<SimpleAuthOutput> async(@NotNull Duration timeout, @NotNull TimeoutFallback timeoutFallback,
                                           @NotNull String reasonString);

    /**
     * Fails the authentication for the client.
     * <p>
     * For an MQTT 3 client a CONNACK with connect return code <b>'Connection Refused, not authorized'</b> is sent.
     * For an MQTT 5 client a CONNACK with reason code NOT_AUTHORIZED is sent.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void failAuthentication();

    /**
     * Fails the authentication for the client.
     * <p>
     * For an MQTT 3 client a CONNACK with connect return code <b>'Connection Refused, not authorized'</b> is sent.
     * For an MQTT 5 client a CONNACK with reason code NOT_AUTHORIZED and reason string given with
     * <code>reasonString</code> is sent.
     *
     * @param reasonString Used as the reason string in the CONNACK packet.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void failAuthentication(@NotNull String reasonString);

    /**
     * Fails the authentication for the client.
     * <p>
     * For an MQTT 3 client a CONNACK with connect return code <b>'Connection Refused, not authorized'</b> is sent.
     * For an MQTT 5 client a CONNACK containing the reason code and reason string given with <code>reasonCode</code>
     * and <code>reasonString</code> respectively.
     *
     * @param reasonCode   Used as the reason code in the CONNACK packet.
     * @param reasonString Used as the reason string in the CONNACK packet.
     * @throws IllegalArgumentException      If the reasonCode is SUCCESS.
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void failAuthentication(@NotNull ConnackReasonCode reasonCode, @NotNull String reasonString);

    /**
     * The outcome of the authentication is determined by the next extension with an {@link
     * Authenticator}.
     * If no extension with an Authenticator is left the default behaviour is used. <p>
     * <p>
     * The default behaviour for an MQTT 3 client is to sent a CONNACK with connect return code <b>'Connection Refused,
     * not authorized'</b>. For an MQTT 5 client a CONNACK with reason code NOT_AUTHORIZED and reason string
     * <b>'authentication failed by extension'</b> is sent.
     *
     * @throws UnsupportedOperationException When authenticateSuccessfully, failAuthentication or nextExtensionOrDefault
     *                                       has already been called.
     * @since 4.0.0
     */
    void nextExtensionOrDefault();

    /**
     * A {@link ModifiableUserProperties} to add or remove user properties to or from the outgoing CONNACK packet.
     *
     * @return The {@link ModifiableUserProperties} object.
     * @since 4.0.0
     */
    @NotNull ModifiableUserProperties getOutboundUserProperties();

    /**
     * The default permissions for the client. Default permissions are automatically applied by HiveMQ for every
     * MQTT PUBLISH and SUBSCRIBE packet sent by the client.
     *
     * @return The {@link ModifiableDefaultPermissions} object for the client.
     * @since 4.0.0
     */
    @NotNull ModifiableDefaultPermissions getDefaultPermissions();

    /**
     * Provides a {@link ModifiableClientSettings} object, that is used to configure client specific parameters and restrictions.
     *
     * @return A {@link ModifiableClientSettings} object.
     * @since 4.2.0
     */
    @NotNull ModifiableClientSettings getClientSettings();
}
