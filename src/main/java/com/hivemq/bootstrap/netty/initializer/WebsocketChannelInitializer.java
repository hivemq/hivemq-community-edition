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
package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.configuration.service.entity.WebsocketListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.ssl.NonSslHandler;
import com.hivemq.websocket.WebSocketInitializer;
import io.netty.channel.Channel;

import javax.inject.Provider;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NON_SSL_HANDLER;

/**
 * @author Christoph Schäbel
 */
public class WebsocketChannelInitializer extends AbstractChannelInitializer {

    @NotNull
    private final WebsocketListener websocketListener;
    @NotNull
    private final Provider<NonSslHandler> nonSslHandlerProvider;

    public WebsocketChannelInitializer(@NotNull final ChannelDependencies channelDependencies,
                                       @NotNull final WebsocketListener websocketListener,
                                       @NotNull final Provider<NonSslHandler> nonSslHandlerProvider) {
        super(channelDependencies, websocketListener);
        this.websocketListener = websocketListener;
        this.nonSslHandlerProvider = nonSslHandlerProvider;
    }

    @Override
    protected void addSpecialHandlers(@NotNull final Channel ch) {
        ch.pipeline().addFirst(NON_SSL_HANDLER, nonSslHandlerProvider.get());
        new WebSocketInitializer(websocketListener).addHandlers(ch, NON_SSL_HANDLER);
    }
}
