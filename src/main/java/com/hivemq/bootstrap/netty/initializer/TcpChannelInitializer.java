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
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.ssl.NonSslHandler;
import io.netty.channel.Channel;

import javax.inject.Provider;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NON_SSL_HANDLER;


/**
 * @author Christoph Schäbel
 */
public class TcpChannelInitializer extends AbstractChannelInitializer {

    @NotNull
    private final Provider<NonSslHandler> nonSslHandlerProvider;

    public TcpChannelInitializer(@NotNull final ChannelDependencies channelDependencies,
                                 @NotNull final TcpListener tcpListener,
                                 @NotNull final Provider<NonSslHandler> nonSslHandlerProvider) {
        super(channelDependencies, tcpListener);
        this.nonSslHandlerProvider = nonSslHandlerProvider;
    }

    @Override
    protected void addSpecialHandlers(@NotNull final Channel ch) {
        ch.pipeline().addFirst(NON_SSL_HANDLER, nonSslHandlerProvider.get());
    }

}
