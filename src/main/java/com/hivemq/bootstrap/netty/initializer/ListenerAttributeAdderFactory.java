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

package com.hivemq.bootstrap.netty.initializer;

import com.google.common.collect.ImmutableList;
import com.hivemq.annotations.Immutable;
import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.impl.listener.InternalListenerConfigurationService;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ListenerAttributeAdderFactory {

    /**
     * We cache the listeners Attribute adders to avoid garbage pressure
     */
    private final ConcurrentHashMap<Listener, ListenerAttributeAdder> listeners = new ConcurrentHashMap<>();


    @Inject
    ListenerAttributeAdderFactory(final InternalListenerConfigurationService listenerConfigurationService) {


        listenerConfigurationService.addUpdateListener(new InternalListenerConfigurationService.UpdateListener() {
            @Override
            public void onRegister(@NotNull final ImmutableList<Listener> allListeners) {

                addListeners(allListeners);
            }


            @Override
            public void update(@NotNull final Listener newListener, @NotNull final ImmutableList<Listener> allListeners) {
                addListeners(allListeners);
            }

            private void addListeners(@NotNull final ImmutableList<Listener> allListeners) {
                for (final Listener currentListener : allListeners) {
                    listeners.putIfAbsent(currentListener, new ListenerAttributeAdder(currentListener));
                }
            }
        });
    }

    public ListenerAttributeAdder get(final Listener listener) {
        final ListenerAttributeAdder attributeAdder = listeners.get(listener);

        //This case should never happen but better be safe than sorry
        if (attributeAdder == null) {
            return new ListenerAttributeAdder(listener);
        }
        return attributeAdder;
    }


    @ChannelHandler.Sharable
    @Immutable
    public static class ListenerAttributeAdder extends ChannelInboundHandlerAdapter {

        private final Listener listener;

        private ListenerAttributeAdder(final Listener listener) {

            this.listener = listener;
        }

        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            ctx.channel().attr(ChannelAttributes.LISTENER).set(listener);
            //we're removing ourselves as soon as we added the listener
            ctx.pipeline().remove(this);

            super.channelActive(ctx);
        }
    }
}