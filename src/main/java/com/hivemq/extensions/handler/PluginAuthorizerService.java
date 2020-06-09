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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public interface PluginAuthorizerService {

    /**
     * authorize a publish message when authorizers are available, otherwise delegate to incoming publish service
     *
     * @param ctx the context of the channel handler
     * @param msg the publish message to authorize
     */
    void authorizePublish(@NotNull ChannelHandlerContext ctx, @NotNull PUBLISH msg);

    /**
     * authorize a will publish message when authorizers are available
     *
     * @param ctx     the context of the channel handler
     * @param connect the connect message to authorize the will for.
     */
    void authorizeWillPublish(@NotNull ChannelHandlerContext ctx, @NotNull CONNECT connect);

    /**
     * authorize a subscribe message when authorizers are available, otherwise delegate to subscribe handler
     *
     * @param ctx the context of the channel handler
     * @param msg the subscribe message to authorize
     */
    void authorizeSubscriptions(@NotNull ChannelHandlerContext ctx, @NotNull SUBSCRIBE msg);
}
