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

package com.hivemq.extensions.handler;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.services.auth.ModifiableClientSettingsImpl;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.Channel;
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
     * @param connectHandler the connect handler to proceed the CONNECT.
     * @param ctx            the context of the channel handler
     * @param connect        the original CONNECT message
     * @param clientSettings the client settings.
     */
    void authenticateConnect(@NotNull ConnectHandler connectHandler, @NotNull ChannelHandlerContext ctx, @NotNull CONNECT connect, @NotNull ModifiableClientSettingsImpl clientSettings);

    /**
     * Re-Authenticate a client.
     * <p>
     * May cause DISCONNECT or AUTH sent to the client.
     *
     * @param ctx            the context of the channel handler
     * @param auth           the original AUTH message
     */
    void authenticateReAuth(@NotNull ChannelHandlerContext ctx, @NotNull AUTH auth);

    /**
     * Authenticate a client.
     * <p>
     * May cause DISCONNECT or AUTH sent to the client.
     *
     * @param connectHandler the connect handler to proceed the CONNECT.
     * @param ctx            the context of the channel handler
     * @param auth           the original AUTH message
     * @param reAuth         is re-authentication?
     */
    void authenticateAuth(@Nullable ConnectHandler connectHandler, @NotNull ChannelHandlerContext ctx, @NotNull AUTH auth, boolean reAuth);

}
