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
package com.hivemq.extension.sdk.api.events.client;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.client.parameters.*;

/**
 * A {@link ClientLifecycleEventListener} contains methods that allow custom logic to be executed when:
 *
 * <ul>
 * <li>A client starts an MQTT connection</li>
 * <li>The authentication for a client is successful</li>
 * <li>A client is disconnected</li>
 * </ul>
 * <p>
 * The methods here are for informational purpose only and do not allow modification of the MQTT packets exchanged
 * between the client and the server. Interceptors and Packet Modifiers can be used to modify MQTT packets.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface ClientLifecycleEventListener {

    /**
     * This method is called by HiveMQ when an MQTT connection is started by the client. The MQTT connection starts when
     * the client sends an MQTT CONNECT packet to the server.
     * <p>
     * This method is executed after the ConnectInboundPacketModifier and the information represented in the
     * input can already modified by an extension.
     *
     * @param connectionStartInput The {@link ConnectionStartInput} containing information about the MQTT CONNECT
     *                             packet, the client and the connection.
     * @since 4.0.0
     */
    void onMqttConnectionStart(@NotNull ConnectionStartInput connectionStartInput);

    /**
     * This method is called by HiveMQ after an MQTT connection is successfully authenticated.
     *
     * @param authenticationSuccessfulInput The {@link AuthenticationSuccessfulInput} containing information about the
     *                                      client and the connection.
     * @since 4.0.0
     */
    void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput authenticationSuccessfulInput);

    /**
     * This method is the default method called by HiveMQ for all cases where a client disconnects.
     * <p>
     * To implement custom logic for specific disconnect cases the following methods can be overridden:
     * <ul>
     * <li>{@link ClientLifecycleEventListener#onAuthenticationFailedDisconnect(AuthenticationFailedInput)}</li>
     * <li>{@link ClientLifecycleEventListener#onClientInitiatedDisconnect(ClientInitiatedDisconnectInput)}</li>
     * <li>{@link ClientLifecycleEventListener#onConnectionLost(ConnectionLostInput)}</li>
     * <li>{@link ClientLifecycleEventListener#onServerInitiatedDisconnect(ServerInitiatedDisconnectInput)}</li>
     * </ul>
     * <p>
     * If all the above methods are implemented, onDisconnect will never be called.
     *
     * @param disconnectEventInput The {@link DisconnectEventInput} containing information about the exact disconnect
     *                             reason, the client and the connection.
     * @since 4.0.0
     */
    void onDisconnect(@NotNull DisconnectEventInput disconnectEventInput);

    /**
     * This method is called when a client is disconnected by the server when the authentication for this MQTT
     * connection failed.
     * <p>
     * By default this calls {@link ClientLifecycleEventListener#onDisconnect}. This method can be overridden to execute
     * custom logic for the specific disconnect when the authentication fails.
     *
     * @param authenticationFailedInput The {@link AuthenticationFailedInput} containing information about the exact
     *                                  disconnect reason, the client and the connection.
     * @since 4.0.0
     */
    default void onAuthenticationFailedDisconnect(@NotNull final AuthenticationFailedInput authenticationFailedInput) {
        onDisconnect(authenticationFailedInput);
    }

    /**
     * This method is called when a client is disconnected because the connection is lost.
     * <p>
     * By default this calls {@link ClientLifecycleEventListener#onDisconnect}. This method can be overridden to execute
     * custom logic for the specific disconnect when the connection is lost.
     * <p>
     * In this case a reason code, reason string and user properties are not present in the input.
     *
     * @param connectionLostInput The {@link ConnectionLostInput} containing information about the client and the
     *                            connection.
     * @since 4.0.0
     */
    default void onConnectionLost(@NotNull final ConnectionLostInput connectionLostInput) {
        onDisconnect(connectionLostInput);
    }

    /**
     * This method is called when a client disconnects by sending a MQTT DISCONNECT packet to the server.
     * <p>
     * By default this calls {@link ClientLifecycleEventListener#onDisconnect}. This method can be overridden to execute
     * a custom logic when the client initiates the disconnect.
     *
     * @param clientInitiatedDisconnectInput The {@link ClientInitiatedDisconnectInput} containing information about the
     *                                       exact disconnect reason, the client and the connection.
     * @since 4.0.0
     */
    default void onClientInitiatedDisconnect(@NotNull final ClientInitiatedDisconnectInput clientInitiatedDisconnectInput) {
        onDisconnect(clientInitiatedDisconnectInput);
    }

    /**
     * This method is called when a client is disconnected by the server with an MQTT DISCONNECT or CONNACK packet for
     * any other reason than a failed authentication.
     * <p>
     * By default this calls {@link ClientLifecycleEventListener#onDisconnect}. This method can be overridden to execute
     * custom logic when a client is disconnected by the server.
     *
     * @param serverInitiatedDisconnectInput The {@link ServerInitiatedDisconnectInput} containing information about the
     *                                       exact disconnect reason, the client and the connection.
     * @since 4.0.0
     */
    default void onServerInitiatedDisconnect(@NotNull final ServerInitiatedDisconnectInput serverInitiatedDisconnectInput) {
        onDisconnect(serverInitiatedDisconnectInput);
    }

}
