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
package util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Just forwards all messages which are read as a user message
 */
public class CollectUserEventsHandler<T> extends ChannelInboundHandlerAdapter {

    private final Class<T> classToCollect;

    private final Queue<T> objects = new ArrayDeque<>();

    public CollectUserEventsHandler(@NotNull final Class<T> classToCollect) {
        this.classToCollect = classToCollect;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, @NotNull final Object evt) throws Exception {
        if (classToCollect.isAssignableFrom(evt.getClass())) {
            objects.add((T) evt);
        }
        super.userEventTriggered(ctx, evt);
    }

    @NotNull
    public Queue<T> getEvents() {
        return objects;
    }

    @Nullable
    public T pollEvent() {
        return objects.poll();
    }
}