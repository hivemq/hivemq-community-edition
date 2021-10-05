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
package com.hivemq.extensions.handler;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Florian Limpöck
 */
public interface PluginAuthenticatorService {

    /**
     * Authenticate a client at the connect process.
     * <p>
     * May cause CONNACK or AUTH sent to the client.
     *
     * @param ctx              the context of the channel handler
     * @param clientConnection the connection of the client.
     * @param connect          the original CONNECT message.
     * @param clientSettings   the client settings.
     */
    void authenticateConnect(
            @NotNull ChannelHandlerContext ctx,
            @NotNull ClientConnection clientConnection,
            @NotNull CONNECT connect,
            @NotNull ModifiableClientSettingsImpl clientSettings);

    /**
     * Authenticate a client.
     * <p>
     * May cause DISCONNECT or AUTH sent to the client.
     *
     * @param ctx              the context of the channel handler
     * @param clientConnection the connection of the client.
     * @param auth             the original AUTH message.
     */
    void authenticateAuth(
            @NotNull ChannelHandlerContext ctx,
            @NotNull ClientConnection clientConnection,
            @NotNull AUTH auth);
}
