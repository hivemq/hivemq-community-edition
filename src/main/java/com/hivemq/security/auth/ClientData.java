/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.security.auth;

import com.google.common.base.Optional;
import com.hivemq.configuration.service.entity.Listener;

import java.net.InetAddress;

/**
 * Represents information available of a connected client.
 *
 * @author Christian Goetz
 * @author Dominik Obermaier
 * @since 1.4
 */
public interface ClientData {

    /**
     * @return the client identifier which was provided in the MQTT CONNECT message
     */
    String getClientId();

    /**
     * @return an {@link Optional} with the username which was provided in the MQTT CONNECT message.
     */
    Optional<String> getUsername();

    /**
     * @return <code>true</code> if the client is authenticated, <code>false</code> otherwise
     */
    boolean isAuthenticated();

    /**
     * @return an {@link Optional} of a {@link SslClientCertificate} which was provided at transport level client
     * certificate authentication.
     * @since 3.0
     */
    Optional<SslClientCertificate> getCertificate();

    /**
     * @return <code>true</code> if the client is anonymous. That means the client is <b>not</b> authenticated
     */
    boolean isAnonymous();

    /**
     * @return <code>true</code> if this client is a bridge.
     * @since 2.0
     */
    boolean isBridge();


    /**
     * @return an {@link Optional} of the clients IP address as {@link InetAddress}
     * @since 3.0
     */
    Optional<InetAddress> getInetAddress();


    /**
     * Returns the listener the client connected to.
     * <p>
     * If the client is connected, the listener will always be available.
     *
     * @return an {@link Optional} of the {@link Listener} the client connected to.
     * @see com.hivemq.configuration.service.entity.TcpListener
     * @see com.hivemq.configuration.service.entity.TlsTcpListener
     * @see com.hivemq.configuration.service.entity.WebsocketListener
     * @see com.hivemq.configuration.service.entity.TlsWebsocketListener
     * @since 3.2
     */
    Optional<Listener> getListener();

    /**
     * Returns optional {@link Long} that contains the disconnect timestamp.
     *
     * @return an {@link Optional} of the {@link Long}.
     * @since 3.4
     */
    Optional<Long> getDisconnectTimestamp();

}
