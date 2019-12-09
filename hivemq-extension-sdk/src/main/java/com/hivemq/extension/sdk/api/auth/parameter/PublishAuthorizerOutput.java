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


import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;

/**
 * This is the output parameter of any {@link PublishAuthorizer}
 * providing methods to define the outcome of the PUBLISH authorization.
 * <p>
 * It can be used to:
 * <ul>
 * <li>Authorize the PUBLISH successfully</li>
 * <li>Let the authorization of the PUBLISH fail</li>
 * <li>Disconnect the sender of the PUBLISH</li>
 * <li>Delegate the decision to the next extension</li>
 * </ul>
 * <p>
 * Only one of the methods {@link #authorizeSuccessfully()}, {@link #failAuthorization()},
 * {@link #disconnectClient()} or {@link #nextExtensionOrDefault()} may be called.
 * <p>
 * Subsequent calls will fail with an {@link UnsupportedOperationException}.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface PublishAuthorizerOutput extends AsyncOutput<PublishAuthorizerOutput> {

    /**
     * Successfully authorizes the PUBlISH.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void authorizeSuccessfully();

    /**
     * Fails the authorization of the PUBLISH.
     * The outcome depends on the MQTT version specified by the publishing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * For an MQTT 5 client the PUBLISH is completed (if qos &gt; 0) with a PUBACK/PUBREC containing the reason code
     * NOT_AUTHORIZED.
     * After that the client receives a DISCONNECT packet with reason code NOT_AUTHORIZED, then the connection is
     * closed.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void failAuthorization();

    /**
     * Fails the authorization of the PUBLISH.
     * The outcome depends on the MQTT version specified by the publishing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * For an MQTT 5 client the PUBLISH is completed (if qos &gt; 0) with a PUBACK/PUBREC containing the reason code given
     * with <code>reasonCode</code>.
     * After that the client receives a DISCONNECT packet with reason code NOT_AUTHORIZED, then the connection is
     * closed.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode Used as the reason code in the PUBACK/PUBREC.
     * @throws IllegalArgumentException      If the passed {@link AckReasonCode} is not an error code.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void failAuthorization(@NotNull AckReasonCode reasonCode);

    /**
     * Fails the authorization of the PUBLISH.
     * The outcome depends on the MQTT version specified by the publishing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * For an MQTT 5 client the PUBLISH is completed (if qos &gt; 0) with a PUBACK/PUBREC containing the reason code and
     * reason string given with <code>reasonCode</code> and <code>reasonString</code> respectively.
     * After that the client receives a DISCONNECT packet with reason code NOT_AUTHORIZED, then the connection is
     * closed.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode   Used as the reason code in the PUBACK/PUBREC.
     * @param reasonString Used as the reason string in the PUBACK/PUBREC.
     * @throws IllegalArgumentException      If the passed {@link AckReasonCode} is not an error code.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void failAuthorization(@NotNull AckReasonCode reasonCode, @NotNull String reasonString);

    /**
     * Disconnects the client that sent the PUBLISH.
     * The outcome depends on the MQTT version specified by the publishing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * An MQTT 5 client receives a DISCONNECT packet with reason code NOT_AUTHORIZED, then the connection is closed.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void disconnectClient();

    /**
     * Disconnects the client that sent the PUBLISH.
     * The outcome depends on the MQTT version specified by the publishing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * An MQTT 5 client receives a DISCONNECT packet with the reason code given with <code>reasonCode</code>, then the
     * connection is closed.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode Used as the reason code in the DISCONNECT packet.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void disconnectClient(@NotNull DisconnectReasonCode reasonCode);

    /**
     * Disconnects the client that sent the PUBLISH.
     * The outcome depends on the MQTT version specified by the publishing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * An MQTT 5 client receives a DISCONNECT with the reason code and reason string given with
     * <code>reasonCode</code> and <code>reasonString</code> respectively, then the connection is closed.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode   Used as the reason code in the DISCONNECT packet.
     * @param reasonString Used as the reason string in the DISCONNECT packet.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void disconnectClient(@NotNull DisconnectReasonCode reasonCode, @NotNull String reasonString);

    /**
     * The outcome of the authorization is determined by the next extension with a {@link PublishAuthorizer}.
     * <p>
     * If no extension with a PublishAuthorizer is left the default permissions (see {@link
     * ModifiableDefaultPermissions})are used.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void nextExtensionOrDefault();

}
