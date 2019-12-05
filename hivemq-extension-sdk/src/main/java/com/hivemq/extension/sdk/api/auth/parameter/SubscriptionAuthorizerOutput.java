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
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;

/**
 * This is the output parameter of any {@link SubscriptionAuthorizer}
 * providing methods to define the outcome of the subscription authorization.
 * <p>
 * It can be used to
 * <ul>
 * <li>Authorize a subscription successfully</li>
 * <li>Let the authorization of the subscription fail</li>
 * <li>Disconnect the sender of the subscription</li>
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
public interface SubscriptionAuthorizerOutput extends AsyncOutput<SubscriptionAuthorizerOutput> {

    /**
     * Successfully authorizes the subscription.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void authorizeSuccessfully();

    /**
     * Fails the authorization of the subscription.
     * The outcome depends on the MQTT version specified by the subscribing client.
     * <ul>
     * <li>For an MQTT 3.1 client the connection is closed.</li>
     * <li>For an MQTT 3.1.1 client the return code for the subscription in the SUBACK packet is <b>'Failure'</b>.</li>
     * <li>For an MQTT 5 client the reason code for the subscription in the SUBACK packet is NOT_AUTHORIZED.</li>
     * </ul>
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void failAuthorization();

    /**
     * Fails the authorization of the subscription.
     * The outcome depends on the MQTT version specified by the subscribing client.
     * <ul>
     * <li>For an MQTT 3.1 client the connection is closed.</li>
     * <li>For an MQTT 3.1.1 client the return code for the subscription in the SUBACK packet is <b>'Failure'</b>.</li>
     * <li>For an MQTT 5 client <code>reasonCode</code> is used as the reason code for the subscription in the SUBACK
     * packet.</li>
     * </ul>
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode Used as the reason code for the subscription in the SUBACK packet.
     * @throws IllegalArgumentException      If the passed {@link SubackReasonCode} is not an error code.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void failAuthorization(@NotNull SubackReasonCode reasonCode);

    /**
     * Fails the authorization of the subscription.
     * The outcome depends on the MQTT version specified by the subscribing client.
     * <ul>
     * <li>For an MQTT 3.1 client the connection is closed.</li>
     * <li>For an MQTT 3.1.1 client the return code for the subscription in the SUBACK packet is <b>'Failure'</b>.</li>
     * <li>For an MQTT 5 client <code>reasonCode</code> is Used as the reason code for the subscription and
     * <code>reasonString</code> is Used as the reason string for the SUBACK packet. If this method is called for
     * more than one subscription, the <code>reasonString</code>'s are combined.</li>
     * </ul>
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode   Used as the reason code for the subscription in the SUBACK packet.
     * @param reasonString Used as the reason string for the SUBACK packet.
     * @throws IllegalArgumentException      If the passed {@link SubackReasonCode} is not an error code.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void failAuthorization(@NotNull SubackReasonCode reasonCode, @NotNull String reasonString);

    /**
     * Disconnects the client that sent the subscription.
     * The outcome depends on the MQTT version specified by the subscribing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * An MQTT 5 client receives a DISCONNECT packet with reason code NOT_AUTHORIZED, then the connection is closed.
     * <p>
     * All Subscriptions from the same SUBSCRIBE packet are ignored, independent of the outcome for the other
     * Subscriptions in this packet.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void disconnectClient();

    /**
     * Disconnects the client that sent the subscription.
     * The outcome depends on the MQTT version specified by the subscribing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * An MQTT 5 client receives a DISCONNECT packet with the reason code given with <code>reasonCode</code>, then the
     * connection is closed.
     * <p>
     * All Subscriptions from the same SUBSCRIBE packet are ignored, independent of the outcome for the other
     * Subscriptions in this packet.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode Used as the reason code for the DISCONNECT packet.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void disconnectClient(@NotNull DisconnectReasonCode reasonCode);

    /**
     * Disconnects the client that sent the subscription.
     * The outcome depends on the MQTT version specified by the subscribing client.
     * <p>
     * For an MQTT 3 client the connection is closed.
     * An MQTT 5 client receives a DISCONNECT packet with the reason code and reason string given with
     * <code>reasonCode</code> and <code>reasonString</code> respectively, then the connection is closed.
     * <p>
     * All Subscriptions from the same SUBSCRIBE packet are ignored, independent of the outcome for the other
     * Subscriptions in this packet.
     * <p>
     * This is a final decision, other extensions or default permissions are ignored.
     *
     * @param reasonCode   Used as the reason code for the DISCONNECT packet.
     * @param reasonString Used as the reason string for the DISCONNECT packet.
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void disconnectClient(@NotNull DisconnectReasonCode reasonCode, @NotNull String reasonString);

    /**
     * The outcome of the authorization is determined by the next extension with a {@link SubscriptionAuthorizer}.
     * <p>
     * If no extension with a SubscriptionAuthorizer is left the default permissions (see {@link
     * ModifiableDefaultPermissions})are used. If no default permissions are set, then the authorization is denied.
     *
     * @throws UnsupportedOperationException When authorizeSuccessfully, failAuthorization, disconnectClient or
     *                                       nextExtensionOrDefault has already been called.
     * @since 4.0.0
     */
    void nextExtensionOrDefault();
}
