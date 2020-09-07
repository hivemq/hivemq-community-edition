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
package com.hivemq.mqtt.handler.connect;

import com.hivemq.configuration.HivemqId;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeepAliveIdleHandlerTest {


    @Test
    public void test_disconnect_when_idle() throws Exception {
        final KeepAliveIdleHandler keepAliveIdleHandler = new KeepAliveIdleHandler(new MqttServerDisconnectorImpl(new EventLog(), new HivemqId()));
        final EmbeddedChannel channel = new EmbeddedChannel(keepAliveIdleHandler, new AnotherIdleHandler(false));

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);

        //We got disconnected
        assertEquals(false, channel.isActive());

        channel.checkException();
    }

    @Test
    public void test_dont_disconnect_on_wrong_idles() throws Exception {
        final KeepAliveIdleHandler keepAliveIdleHandler = new KeepAliveIdleHandler(new MqttServerDisconnectorImpl(new EventLog(), new HivemqId()));
        final EmbeddedChannel channel = new EmbeddedChannel(keepAliveIdleHandler, new AnotherIdleHandler(true));
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT);
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT);

        assertEquals(true, channel.isActive());
        channel.checkException();
    }

    private static class AnotherIdleHandler extends ChannelInboundHandlerAdapter {

        private final boolean shouldGetTriggered;

        public AnotherIdleHandler(final boolean shouldGetTriggered) {
            this.shouldGetTriggered = shouldGetTriggered;
        }

        @Override
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
            assertEquals(shouldGetTriggered, true);
        }
    }
}