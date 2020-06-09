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
package com.hivemq.bootstrap.netty;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.netty.initializer.*;
import com.hivemq.configuration.service.entity.Listener;

/**
 * Interface for {@link ChannelInitializerFactoryImpl}
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface ChannelInitializerFactory {

    /**
     * The returned channel initializer ca be:
     * <ul>
     * <li>{@link TcpChannelInitializer}</li>
     * <li>{@link TlsTcpChannelInitializer}</li>
     * <li>{@link WebsocketChannelInitializer}</li>
     * <li>{@link TlsWebsocketChannelInitializer}</li>
     * </ul>
     *
     * @param listener the listener to create a channel initializer for
     * @return a channel initializer for a specific listener.
     * @throws NullPointerException     If listener is null.
     * @throws IllegalArgumentException If listener type is unknown.
     */
    @NotNull
    AbstractChannelInitializer getChannelInitializer(final @NotNull Listener listener);

}
