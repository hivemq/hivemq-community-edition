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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.channel.ChannelFuture;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dominik Obermaier
 */
@Immutable
public class BindInformation {

    private final Listener listener;

    private final ChannelFuture bindFuture;


    public BindInformation(@NotNull final Listener listener, @NotNull final ChannelFuture bindFuture) {
        checkNotNull(listener, "Listener must not be null");
        checkNotNull(bindFuture, "Future must not be null");
        this.listener = listener;
        this.bindFuture = bindFuture;
    }

    public Listener getListener() {
        return listener;
    }

    public ChannelFuture getBindFuture() {
        return bindFuture;
    }
}
